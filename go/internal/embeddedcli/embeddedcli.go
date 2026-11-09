package embeddedcli

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"crypto/sha256"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"

	"github.com/github/copilot-sdk/go/internal/flock"
)

// Config defines the inputs used to install and locate the embedded Copilot CLI.
//
// Cli and CliHash are required. If Dir is empty, the CLI is installed into the
// per-user cache directory or a protected fallback cache.
// When Version is set, the CLI is installed into a
// version-specific child directory so multiple versions can coexist. License,
// when provided, is written next to the installed binary.
//
// RuntimeExecutable and RuntimeNode form the adjacent out-of-process runtime
// pair. RuntimeAssets is a filtered release package archive containing auxiliary
// binaries and resources. RuntimeLib is the same cdylib bytes installed under
// the natural platform name for the optional in-process transport.
type Config struct {
	Cli     io.Reader
	CliHash []byte

	License []byte

	RuntimeLib     io.Reader
	RuntimeLibHash []byte

	RuntimeExecutable     io.Reader
	RuntimeExecutableHash []byte
	RuntimeNode           io.Reader
	RuntimeNodeHash       []byte
	RuntimeAssets         io.Reader
	RuntimeAssetsHash     []byte

	// LinuxMuslCli and LinuxMuslRuntimeLib are optional alternatives selected
	// automatically when the application runs on a musl-based Linux system.
	LinuxMuslCli                   io.Reader
	LinuxMuslCliHash               []byte
	LinuxMuslRuntimeLib            io.Reader
	LinuxMuslRuntimeLibHash        []byte
	LinuxMuslRuntimeExecutable     io.Reader
	LinuxMuslRuntimeExecutableHash []byte
	LinuxMuslRuntimeNode           io.Reader
	LinuxMuslRuntimeNodeHash       []byte
	LinuxMuslRuntimeAssets         io.Reader
	LinuxMuslRuntimeAssetsHash     []byte

	Dir     string
	Version string
}

func Setup(cfg Config) {
	if cfg.Cli == nil {
		panic("Cli reader is required")
	}
	if len(cfg.CliHash) != sha256.Size {
		panic(fmt.Sprintf("CliHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(cfg.CliHash)))
	}
	if cfg.LinuxMuslCli != nil && len(cfg.LinuxMuslCliHash) != sha256.Size {
		panic(fmt.Sprintf("LinuxMuslCliHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(cfg.LinuxMuslCliHash)))
	}
	if cfg.LinuxMuslRuntimeLib != nil && len(cfg.LinuxMuslRuntimeLibHash) != sha256.Size {
		panic(fmt.Sprintf("LinuxMuslRuntimeLibHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(cfg.LinuxMuslRuntimeLibHash)))
	}
	validateRuntimePairConfig(cfg.RuntimeExecutable, cfg.RuntimeExecutableHash, cfg.RuntimeNode, cfg.RuntimeNodeHash, "")
	validateRuntimePairConfig(cfg.LinuxMuslRuntimeExecutable, cfg.LinuxMuslRuntimeExecutableHash, cfg.LinuxMuslRuntimeNode, cfg.LinuxMuslRuntimeNodeHash, "LinuxMusl")
	validateOptionalHash(cfg.RuntimeAssets, cfg.RuntimeAssetsHash, "RuntimeAssetsHash")
	validateOptionalHash(cfg.LinuxMuslRuntimeAssets, cfg.LinuxMuslRuntimeAssetsHash, "LinuxMuslRuntimeAssetsHash")
	cfg.Cli = newRetryableReader(cfg.Cli)
	cfg.RuntimeLib = newRetryableReader(cfg.RuntimeLib)
	cfg.RuntimeExecutable = newRetryableReader(cfg.RuntimeExecutable)
	cfg.RuntimeNode = newRetryableReader(cfg.RuntimeNode)
	cfg.LinuxMuslCli = newRetryableReader(cfg.LinuxMuslCli)
	cfg.LinuxMuslRuntimeLib = newRetryableReader(cfg.LinuxMuslRuntimeLib)
	cfg.LinuxMuslRuntimeExecutable = newRetryableReader(cfg.LinuxMuslRuntimeExecutable)
	cfg.LinuxMuslRuntimeNode = newRetryableReader(cfg.LinuxMuslRuntimeNode)
	setupMu.Lock()
	defer setupMu.Unlock()
	if setupDone {
		panic("Setup must only be called once")
	}
	if pathInitialized {
		panic("Setup must be called before Path is accessed")
	}
	config = cfg
	setupDone = true
}

// retryableReader lets a failed installation attempt be retried. A reader that
// can seek streams again from the offset it had at Setup, so a large artifact
// is never held in memory. Any other reader is buffered when first read, which
// is the only way to replay it. Bundles generated before the bundler emitted
// seekable readers take that path.
type retryableReader struct {
	source io.Reader
	start  int64
	buffer *bytes.Reader
	// used reports whether any install attempt has read from the source. An
	// unused reader is never released, so an artifact that a warm cache made
	// unnecessary can still be replayed into a different cache later.
	used bool
}

func newRetryableReader(r io.Reader) io.Reader {
	if r == nil {
		return nil
	}
	if seeker, ok := r.(io.Seeker); ok {
		if start, err := seeker.Seek(0, io.SeekCurrent); err == nil {
			return &retryableReader{source: r, start: start}
		}
		// A reader that cannot report its position, like the bundler's zstd
		// reader, can still rewind to its beginning.
		if _, err := seeker.Seek(0, io.SeekStart); err == nil {
			return &retryableReader{source: r, start: 0}
		}
	}
	return &retryableReader{source: r, start: -1}
}

func (r *retryableReader) Read(p []byte) (int, error) {
	r.used = true
	if r.start >= 0 {
		return r.source.Read(p)
	}
	if r.buffer == nil {
		content, err := io.ReadAll(r.source)
		if closer, ok := r.source.(io.Closer); ok {
			closer.Close()
		}
		if err != nil {
			return 0, err
		}
		r.buffer = bytes.NewReader(content)
	}
	return r.buffer.Read(p)
}

func (r *retryableReader) Seek(offset int64, whence int) (int64, error) {
	if offset != 0 || whence != io.SeekStart {
		return 0, errors.New("only Seek(0, io.SeekStart) is supported")
	}
	if r.start >= 0 {
		return r.source.(io.Seeker).Seek(r.start, io.SeekStart)
	}
	if r.buffer == nil {
		return 0, nil
	}
	return r.buffer.Seek(0, io.SeekStart)
}

// release drops the replay buffer and closes the source of a reader that an
// installation consumed, once that installation has committed, so neither stays
// reachable for the life of the process.
func (r *retryableReader) release() {
	if !r.used {
		return
	}
	r.buffer = nil
	r.Close()
}

// releaseReaders releases every configured artifact reader after a complete
// installation. Failed attempts keep them so a retry can replay the artifact.
func releaseReaders() {
	releaseRuntimeReaders()
	for _, reader := range []io.Reader{
		config.Cli, config.RuntimeLib, config.LinuxMuslCli, config.LinuxMuslRuntimeLib,
	} {
		if r, ok := reader.(*retryableReader); ok {
			r.release()
		}
	}
}

func releaseRuntimeReaders() {
	for _, reader := range []io.Reader{
		config.RuntimeExecutable, config.RuntimeNode,
		config.LinuxMuslRuntimeExecutable, config.LinuxMuslRuntimeNode,
	} {
		if r, ok := reader.(*retryableReader); ok {
			r.release()
		}
	}
}

func (r *retryableReader) Close() error {
	if closer, ok := r.source.(io.Closer); ok {
		return closer.Close()
	}
	return nil
}

func Path() string {
	setupMu.Lock()
	defer setupMu.Unlock()
	if cliPathCached {
		return cliPath
	}
	if !setupDone {
		cliPathCached = true
		return ""
	}
	pathInitialized = true
	installDir, err := configuredInstallDir()
	if err != nil {
		if os.Getenv("COPILOT_CLI_INSTALL_VERBOSE") == "1" {
			fmt.Printf("embedded CLI installation error: selecting install directory: %v\n", err)
		}
		return ""
	}
	// Cache only a successful installation so a later call can retry after the
	// environment is corrected.
	if path := install(installDir); path != "" {
		cliPathCached = true
		cliPath = path
	}
	return cliPath
}

// RuntimeLibPath returns the on-disk path to the installed native in-process
// runtime library (cdylib), or "" when no runtime library was bundled or the
// CLI could not be installed. It ensures the embedded CLI is installed first.
func RuntimeLibPath() string {
	Path()
	setupMu.Lock()
	defer setupMu.Unlock()
	return runtimeLibPath
}

// RuntimePath returns the installed copilot-runtime executable, or "" when the
// application bundle predates the out-of-process runtime pair. Installation
// failures are returned to the caller.
func RuntimePath() (string, error) {
	setupMu.Lock()
	defer setupMu.Unlock()
	if !setupDone {
		return "", nil
	}
	pathInitialized = true
	selectLinuxMuslBundle()
	if config.RuntimeExecutable == nil {
		return "", nil
	}
	if runtimePath == "" {
		path, err := installRuntime()
		if err != nil {
			return "", err
		}
		runtimePath = path
	}
	return runtimePath, nil
}

var (
	config                 Config
	setupMu                sync.Mutex
	setupDone              bool
	pathInitialized        bool
	cliPath                string
	cliPathCached          bool
	runtimeLibPath         string
	runtimePath            string
	runtimeAssetsInstalled bool
	runtimeAssetsBytes     []byte
	linuxMuslBundle        bool
)

func install(installDir string) (path string) {
	selectLinuxMuslBundle()

	verbose := os.Getenv("COPILOT_CLI_INSTALL_VERBOSE") == "1"
	logError := func(msg string, err error) {
		if verbose {
			fmt.Printf("embedded CLI installation error: %s: %v\n", msg, err)
		}
	}
	if verbose {
		start := time.Now()
		defer func() {
			duration := time.Since(start)
			fmt.Printf("installing embedded CLI at %s installation took %s\n", path, duration)
		}()
	}
	path, err := installAt(installDir)
	if err != nil {
		logError("installing in configured directory", err)
		return ""
	}
	return path
}

func installRuntime() (path string, err error) {
	verbose := os.Getenv("COPILOT_CLI_INSTALL_VERBOSE") == "1"
	logError := func(msg string, err error) {
		if verbose {
			fmt.Printf("embedded runtime installation error: %s: %v\n", msg, err)
		}
	}
	if verbose {
		start := time.Now()
		defer func() {
			fmt.Printf("installing embedded runtime at %s took %s\n", path, time.Since(start))
		}()
	}

	installDir, err := configuredInstallDir()
	if err != nil {
		logError("selecting install directory", err)
		return "", err
	}
	path, err = installRuntimeAt(installDir)
	if err != nil {
		logError("installing in configured directory", err)
		return "", err
	}
	return path, nil
}

func configuredInstallDir() (string, error) {
	installDir := config.Dir
	if installDir == "" {
		if copilotHome := os.Getenv("COPILOT_HOME"); copilotHome != "" {
			installDir = filepath.Join(copilotHome, "cache", "copilot-sdk")
		} else {
			var cacheErr error
			if installDir, cacheErr = os.UserCacheDir(); cacheErr != nil {
				var err error
				if installDir, err = fallbackCacheDir(); err != nil {
					return "", fmt.Errorf("user cache unavailable (%v); configure the user cache or set COPILOT_HOME or embeddedcli.Config.Dir: %w", cacheErr, err)
				}
			}
			installDir = filepath.Join(installDir, "copilot-sdk")
		}
	}
	return installDir, nil
}

func selectLinuxMuslBundle() {
	if linuxMuslBundle || runtime.GOOS != "linux" || config.LinuxMuslCli == nil || !isMusl() {
		return
	}
	config = linuxMuslConfig(config)
	linuxMuslBundle = true
}

func linuxMuslConfig(cfg Config) Config {
	cfg.Cli = cfg.LinuxMuslCli
	cfg.CliHash = cfg.LinuxMuslCliHash
	cfg.RuntimeLib = cfg.LinuxMuslRuntimeLib
	cfg.RuntimeLibHash = cfg.LinuxMuslRuntimeLibHash
	cfg.RuntimeExecutable = cfg.LinuxMuslRuntimeExecutable
	cfg.RuntimeExecutableHash = cfg.LinuxMuslRuntimeExecutableHash
	cfg.RuntimeNode = cfg.LinuxMuslRuntimeNode
	cfg.RuntimeNodeHash = cfg.LinuxMuslRuntimeNodeHash
	cfg.RuntimeAssets = cfg.LinuxMuslRuntimeAssets
	cfg.RuntimeAssetsHash = cfg.LinuxMuslRuntimeAssetsHash
	return cfg
}

func isMusl() bool {
	out, _ := exec.Command("ldd", "--version").CombinedOutput()
	return strings.Contains(strings.ToLower(string(out)), "musl")
}

func installAt(installDir string) (string, error) {
	version := sanitizeVersion(config.Version)
	if version != "" {
		installDir = filepath.Join(installDir, version)
	}
	if linuxMuslBundle {
		installDir = filepath.Join(installDir, "linuxmusl")
	}
	if err := os.MkdirAll(installDir, 0755); err != nil {
		return "", fmt.Errorf("creating install directory: %w", err)
	}

	// Best effort to prevent concurrent installs.
	if release, _ := flock.Acquire(filepath.Join(installDir, ".copilot-cli.lock")); release != nil {
		defer release()
	}

	binaryName := "copilot"
	if runtime.GOOS == "windows" {
		binaryName += ".exe"
	}
	finalPath := filepath.Join(installDir, binaryName)

	if _, err := os.Stat(finalPath); err == nil {
		existingHash, err := hashFile(finalPath)
		if err != nil {
			return "", fmt.Errorf("hashing existing binary: %w", err)
		}
		if !bytes.Equal(existingHash, config.CliHash) {
			return "", fmt.Errorf("existing binary hash mismatch")
		}
		if err := installLicense(finalPath); err != nil {
			return "", err
		}
		var installedRuntimePath string
		if config.RuntimeExecutable != nil {
			path, err := installRuntimePair(installDir)
			if err != nil {
				return "", err
			}
			installedRuntimePath = path
		}
		if config.RuntimeLib != nil {
			libPath, err := installRuntimeLib(installDir)
			if err != nil {
				return "", err
			}
			runtimeLibPath = libPath
		}
		if err := installRuntimeAssets(installDir); err != nil {
			return "", err
		}
		runtimePath = installedRuntimePath
		releaseReaders()
		return finalPath, nil
	}

	if err := installVerifiedFile(finalPath, config.Cli, config.CliHash, 0755, "CLI binary"); err != nil {
		return "", err
	}
	if err := installLicense(finalPath); err != nil {
		return "", err
	}

	var installedRuntimePath string
	if config.RuntimeExecutable != nil {
		path, err := installRuntimePair(installDir)
		if err != nil {
			return "", err
		}
		installedRuntimePath = path
	}

	// Install the native in-process runtime library (if bundled) next to the CLI.
	// Fail closed on any hash mismatch; never place unverified native code.
	if config.RuntimeLib != nil {
		libPath, err := installRuntimeLib(installDir)
		if err != nil {
			return "", err
		}
		runtimeLibPath = libPath
	}
	if err := installRuntimeAssets(installDir); err != nil {
		return "", err
	}
	runtimePath = installedRuntimePath
	releaseReaders()

	return finalPath, nil
}

// installLicense ensures the license sits next to the CLI. It runs on every
// install attempt so a retry after a failed write does not report success
// without it.
func installLicense(finalPath string) error {
	if len(config.License) == 0 {
		return nil
	}
	licensePath := finalPath + ".license"
	// Accept a matching existing license, which may be read-only or owned by
	// another user, and write it only when missing or different.
	if existing, err := os.ReadFile(licensePath); err == nil && bytes.Equal(existing, config.License) {
		return nil
	}
	if err := os.WriteFile(licensePath, config.License, 0644); err != nil {
		return fmt.Errorf("writing license file: %w", err)
	}
	return nil
}

func installRuntimeAt(installDir string) (string, error) {
	version := sanitizeVersion(config.Version)
	if version != "" {
		installDir = filepath.Join(installDir, version)
	}
	if linuxMuslBundle {
		installDir = filepath.Join(installDir, "linuxmusl")
	}
	if err := os.MkdirAll(installDir, 0755); err != nil {
		return "", fmt.Errorf("creating install directory: %w", err)
	}

	if release, _ := flock.Acquire(filepath.Join(installDir, ".copilot-cli.lock")); release != nil {
		defer release()
	}
	path, err := installRuntimePair(installDir)
	if err != nil {
		return "", err
	}
	if err := installRuntimeAssets(installDir); err != nil {
		return "", err
	}
	releaseRuntimeReaders()
	return path, nil
}

func validateOptionalHash(reader io.Reader, hash []byte, name string) {
	if reader != nil && len(hash) != sha256.Size {
		panic(fmt.Sprintf("%s must be a SHA-256 hash (%d bytes), got %d bytes", name, sha256.Size, len(hash)))
	}
}

func installRuntimeAssets(installDir string) error {
	if config.RuntimeAssets == nil || runtimeAssetsInstalled {
		return nil
	}
	// The reader is single-use, so keep its bytes until the assets are installed.
	// A failed write can then be retried without re-reading an exhausted reader.
	archiveBytes := runtimeAssetsBytes
	if archiveBytes == nil {
		var err error
		archiveBytes, err = io.ReadAll(config.RuntimeAssets)
		if closer, ok := config.RuntimeAssets.(io.Closer); ok {
			closer.Close()
		}
		if err != nil {
			return fmt.Errorf("reading runtime assets: %w", err)
		}
		runtimeAssetsBytes = archiveBytes
	}
	actual := sha256.Sum256(archiveBytes)
	if !bytes.Equal(actual[:], config.RuntimeAssetsHash) {
		return fmt.Errorf("runtime assets hash mismatch")
	}
	gzipReader, err := gzip.NewReader(bytes.NewReader(archiveBytes))
	if err != nil {
		return fmt.Errorf("opening runtime assets: %w", err)
	}
	defer gzipReader.Close()
	tarReader := tar.NewReader(gzipReader)
	for {
		header, err := tarReader.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return fmt.Errorf("reading runtime assets: %w", err)
		}
		if header.Typeflag != tar.TypeReg {
			continue
		}
		clean := filepath.Clean(filepath.FromSlash(header.Name))
		if !filepath.IsLocal(clean) {
			return fmt.Errorf("unsafe runtime asset path %q", header.Name)
		}
		content, err := io.ReadAll(tarReader)
		if err != nil {
			return fmt.Errorf("reading runtime asset %q: %w", header.Name, err)
		}
		path := filepath.Join(installDir, clean)
		if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
			return fmt.Errorf("creating runtime asset directory: %w", err)
		}
		hash := sha256.Sum256(content)
		mode := os.FileMode(header.Mode & 0777)
		if err := installVerifiedFile(path, bytes.NewReader(content), hash[:], mode, "runtime asset"); err != nil {
			return err
		}
	}
	runtimeAssetsInstalled = true
	runtimeAssetsBytes = nil
	return nil
}

func validateRuntimePairConfig(wrapper io.Reader, wrapperHash []byte, node io.Reader, nodeHash []byte, prefix string) {
	if (wrapper == nil) != (node == nil) {
		panic(prefix + "RuntimeExecutable and " + prefix + "RuntimeNode must be provided together")
	}
	if wrapper == nil {
		return
	}
	if len(wrapperHash) != sha256.Size {
		panic(fmt.Sprintf("%sRuntimeExecutableHash must be a SHA-256 hash (%d bytes), got %d bytes", prefix, sha256.Size, len(wrapperHash)))
	}
	if len(nodeHash) != sha256.Size {
		panic(fmt.Sprintf("%sRuntimeNodeHash must be a SHA-256 hash (%d bytes), got %d bytes", prefix, sha256.Size, len(nodeHash)))
	}
}

func installRuntimePair(installDir string) (string, error) {
	nodePath := filepath.Join(installDir, "runtime.node")
	if err := installVerifiedFile(nodePath, config.RuntimeNode, config.RuntimeNodeHash, 0644, "runtime.node"); err != nil {
		return "", err
	}
	wrapperPath := filepath.Join(installDir, runtimeExecutableName())
	if err := installVerifiedFile(wrapperPath, config.RuntimeExecutable, config.RuntimeExecutableHash, 0755, "runtime wrapper"); err != nil {
		return "", err
	}
	return wrapperPath, nil
}

func installVerifiedFile(path string, reader io.Reader, expectedHash []byte, mode os.FileMode, label string) error {
	if _, err := os.Stat(path); err == nil {
		existingHash, err := hashFile(path)
		if err != nil {
			return fmt.Errorf("hashing existing %s: %w", label, err)
		}
		if !bytes.Equal(existingHash, expectedHash) {
			return fmt.Errorf("existing %s hash mismatch", label)
		}
		if runtime.GOOS != "windows" && mode.Perm()&0111 != 0 {
			info, err := os.Stat(path)
			if err != nil {
				return fmt.Errorf("checking existing %s permissions: %w", label, err)
			}
			if info.Mode().Perm()&0111 == 0 {
				if err := os.Chmod(path, info.Mode().Perm()|mode.Perm()&0111); err != nil {
					return fmt.Errorf("restoring existing %s permissions: %w", label, err)
				}
			}
		}
		return nil
	}

	// Setup wraps every artifact reader so it can rewind, which lets a failed
	// attempt be retried without buffering a seekable artifact in memory.
	if seeker, ok := reader.(io.Seeker); ok {
		if _, err := seeker.Seek(0, io.SeekStart); err != nil {
			return fmt.Errorf("rewinding %s: %w", label, err)
		}
	}

	tmp, err := os.CreateTemp(filepath.Dir(path), ".copilot-runtime-pair-*.tmp")
	if err != nil {
		return fmt.Errorf("creating temporary %s: %w", label, err)
	}
	tmpPath := tmp.Name()
	h := sha256.New()
	_, err = io.Copy(io.MultiWriter(tmp, h), reader)
	if err1 := tmp.Chmod(mode); err1 != nil && err == nil {
		err = err1
	}
	if err1 := tmp.Close(); err1 != nil && err == nil {
		err = err1
	}
	// A rewindable reader stays open: it is released once the whole
	// installation has succeeded (releaseReaders), so a failure anywhere in the
	// attempt, even in a different install directory, can still be retried.
	// Other readers cannot be replayed and are released now.
	if closer, ok := reader.(io.Closer); ok {
		if _, rewindable := reader.(io.Seeker); !rewindable {
			closer.Close()
		}
	}
	if err != nil {
		os.Remove(tmpPath)
		return fmt.Errorf("writing %s: %w", label, err)
	}
	if !bytes.Equal(h.Sum(nil), expectedHash) {
		os.Remove(tmpPath)
		return fmt.Errorf("%s hash mismatch", label)
	}
	if err := publishStaged(tmpPath, path, expectedHash, mode); err != nil {
		os.Remove(tmpPath)
		return fmt.Errorf("installing %s: %w", label, err)
	}
	return nil
}

// publishStaged moves the verified staged file at tmpPath to path, keeping a
// file already there when it holds the expected bytes and permission bits.
//
// Installers that start on a cold cache at the same time all miss the fast
// path and stage their own copy. The install lock normally serializes them,
// but it is best effort: when it cannot be taken (for example because another
// user created the lock file), renaming each copy into place replaces the file
// an earlier installer already returned, and a process started from it keeps
// running an unlinked executable. Outside Windows the staged file is therefore
// hard-linked into place, which never replaces an existing file; an identical
// file found there is kept and the staged copy discarded. A different file is
// replaced by the rename as before, as is any target when the filesystem
// cannot hard-link. Windows keeps the rename, because a running executable
// cannot be replaced there.
func publishStaged(tmpPath, path string, expectedHash []byte, mode os.FileMode) error {
	if runtime.GOOS != "windows" {
		err := os.Link(tmpPath, path)
		if err == nil || (errors.Is(err, os.ErrExist) && isVerifiedFile(path, expectedHash, mode)) {
			os.Remove(tmpPath)
			return nil
		}
	}
	return os.Rename(tmpPath, path)
}

// isVerifiedFile reports whether path is a regular file with the expected
// SHA-256 hash and permission bits.
func isVerifiedFile(path string, expectedHash []byte, mode os.FileMode) bool {
	info, err := os.Lstat(path)
	if err != nil || !info.Mode().IsRegular() || info.Mode().Perm() != mode.Perm() {
		return false
	}
	hash, err := hashFile(path)
	return err == nil && bytes.Equal(hash, expectedHash)
}

func runtimeExecutableName() string {
	if runtime.GOOS == "windows" {
		return "copilot-runtime.exe"
	}
	return "copilot-runtime"
}

// installRuntimeLib writes the embedded runtime cdylib into installDir under its
// natural platform file name, verifying its SHA-256. It is idempotent: an
// existing file with a matching hash is reused; a mismatch is a hard error.
func installRuntimeLib(installDir string) (string, error) {
	if len(config.RuntimeLibHash) != sha256.Size {
		return "", fmt.Errorf("RuntimeLibHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(config.RuntimeLibHash))
	}
	libPath := filepath.Join(installDir, naturalRuntimeLibName())

	if _, err := os.Stat(libPath); err == nil {
		existingHash, err := hashFile(libPath)
		if err != nil {
			return "", fmt.Errorf("hashing existing runtime library: %w", err)
		}
		if !bytes.Equal(existingHash, config.RuntimeLibHash) {
			return "", fmt.Errorf("existing runtime library hash mismatch")
		}
		return libPath, nil
	}

	if err := installVerifiedFile(libPath, config.RuntimeLib, config.RuntimeLibHash, 0600, "runtime library"); err != nil {
		return "", err
	}
	return libPath, nil
}

// naturalRuntimeLibName is the flat platform file name for the runtime cdylib,
// matching ffihost.NaturalLibraryName (kept in sync; embeddedcli stays
// dependency-free for use by generated embed files).
func naturalRuntimeLibName() string {
	switch runtime.GOOS {
	case "windows":
		return "copilot_runtime.dll"
	case "darwin":
		return "libcopilot_runtime.dylib"
	default:
		return "libcopilot_runtime.so"
	}
}

// sanitizeVersion makes a version string safe for filenames.
func sanitizeVersion(version string) string {
	if version == "" {
		return ""
	}
	var b strings.Builder
	for _, r := range version {
		switch {
		case r >= 'a' && r <= 'z':
			b.WriteRune(r)
		case r >= 'A' && r <= 'Z':
			b.WriteRune(r)
		case r >= '0' && r <= '9':
			b.WriteRune(r)
		case r == '.' || r == '-' || r == '_':
			b.WriteRune(r)
		default:
			b.WriteRune('_')
		}
	}
	sanitized := b.String()
	if sanitized == "." || sanitized == ".." {
		return strings.Repeat("_", len(sanitized))
	}
	return sanitized
}

// hashFile returns the SHA-256 hash of a file on disk.
func hashFile(path string) ([]byte, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()
	h := sha256.New()
	if _, err := io.Copy(h, file); err != nil {
		return nil, err
	}
	return h.Sum(nil), nil
}
