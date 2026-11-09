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
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"testing"
	"testing/iotest"

	"github.com/klauspost/compress/zstd"
)

func resetGlobals() {
	setupMu.Lock()
	defer setupMu.Unlock()
	config = Config{}
	setupDone = false
	pathInitialized = false
	cliPath = ""
	cliPathCached = false
	runtimeLibPath = ""
	runtimePath = ""
	runtimeAssetsInstalled = false
	runtimeAssetsBytes = nil
	linuxMuslBundle = false
}

func TestInstallFallback(t *testing.T) {
	for name, installBundle := range map[string]func() (string, error){
		"cli":     func() (string, error) { return Path(), nil },
		"runtime": RuntimePath,
	} {
		t.Run(name, func(t *testing.T) {
			resetGlobals()
			t.Cleanup(resetGlobals)
			fallback := withoutUserCache(t)
			cli, wrapper, node := []byte("cli"), []byte("wrapper"), []byte("node")
			cliHash, wrapperHash, nodeHash := sha256.Sum256(cli), sha256.Sum256(wrapper), sha256.Sum256(node)
			Setup(Config{
				Cli:                   bytes.NewReader(cli),
				CliHash:               cliHash[:],
				RuntimeExecutable:     bytes.NewReader(wrapper),
				RuntimeExecutableHash: wrapperHash[:],
				RuntimeNode:           bytes.NewReader(node),
				RuntimeNodeHash:       nodeHash[:],
				Version:               "cache-trust-test",
			})
			binaryName, content := binaryNameForOS(), cli
			if name == "runtime" {
				binaryName, content = runtimeExecutableName(), wrapper
			}
			legacyDir := filepath.Join(os.TempDir(), "copilot-sdk", config.Version)
			if err := os.MkdirAll(legacyDir, 0755); err != nil {
				t.Fatal(err)
			}
			for artifact, data := range map[string][]byte{
				binaryNameForOS(): cli, runtimeExecutableName(): wrapper, "runtime.node": node,
			} {
				if runtime.GOOS != "windows" {
					data = []byte("legacy")
				}
				if err := os.WriteFile(filepath.Join(legacyDir, artifact), data, 0755); err != nil {
					t.Fatal(err)
				}
			}

			path, err := installBundle()
			want := filepath.Join(fallback, "copilot-sdk", config.Version, binaryName)
			if err != nil || path != want {
				t.Fatalf("cache-unavailable startup: path=%q want=%q err=%v", path, want, err)
			}
			if got, err := os.ReadFile(path); err != nil || !bytes.Equal(got, content) {
				t.Fatalf("fallback artifact: content=%q err=%v", got, err)
			}
		})
	}
}

func TestConfiguredInstallDirPreservesOverridesWithoutUserCache(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	fallback := withoutUserCache(t)
	if dir, err := configuredInstallDir(); dir != filepath.Join(fallback, "copilot-sdk") || err != nil {
		t.Fatalf("protected fallback: dir=%q err=%v", dir, err)
	}
	home := t.TempDir()
	t.Setenv("COPILOT_HOME", home)
	if dir, err := configuredInstallDir(); dir != filepath.Join(home, "cache", "copilot-sdk") || err != nil {
		t.Fatalf("COPILOT_HOME override: dir=%q err=%v", dir, err)
	}
	config.Dir = t.TempDir()
	if dir, err := configuredInstallDir(); dir != config.Dir || err != nil {
		t.Fatalf("explicit directory override: dir=%q err=%v", dir, err)
	}
	config.Dir = ""
	t.Setenv("COPILOT_HOME", "")
	for _, key := range []string{"HOME", "XDG_CACHE_HOME", "LocalAppData"} {
		t.Setenv(key, home)
	}
	cacheDir, err := os.UserCacheDir()
	if err != nil {
		t.Fatal(err)
	}
	if dir, err := configuredInstallDir(); dir != filepath.Join(cacheDir, "copilot-sdk") || err != nil {
		t.Fatalf("available user cache: dir=%q err=%v", dir, err)
	}
}

// withoutUserCache makes os.UserCacheDir fail and returns the expected fallback cache root.
func withoutUserCache(t *testing.T) string {
	t.Helper()
	tempDir := t.TempDir()
	for _, key := range []string{"TMP", "TEMP", "TMPDIR"} {
		t.Setenv(key, tempDir)
	}
	for _, key := range []string{"HOME", "XDG_CACHE_HOME", "LocalAppData", "USERPROFILE", "COPILOT_HOME"} {
		t.Setenv(key, "")
	}
	if _, err := os.UserCacheDir(); err == nil {
		t.Fatal("test requires unavailable user cache")
	}
	if runtime.GOOS == "windows" {
		return tempDir
	}
	parent, err := filepath.EvalSymlinks(tempDir)
	if err != nil {
		t.Fatal(err)
	}
	return filepath.Join(parent, fmt.Sprintf("copilot-sdk-cache-%d", os.Geteuid()))
}

func TestInstallRuntimeWritesRetainedAssets(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	cli := []byte("cli")
	wrapper := []byte("wrapper")
	node := []byte("runtime")
	assets := runtimeAssetsArchive(t, map[string]assetFixture{
		"ripgrep/bin/test-platform/rg": {content: []byte("ripgrep"), mode: 0755},
		"definitions/future.json":      {content: []byte("{}"), mode: 0644},
	})
	cliHash := sha256.Sum256(cli)
	wrapperHash := sha256.Sum256(wrapper)
	nodeHash := sha256.Sum256(node)
	assetsHash := sha256.Sum256(assets)
	Setup(Config{
		Cli:                   bytes.NewReader(cli),
		CliHash:               cliHash[:],
		RuntimeExecutable:     bytes.NewReader(wrapper),
		RuntimeExecutableHash: wrapperHash[:],
		RuntimeNode:           bytes.NewReader(node),
		RuntimeNodeHash:       nodeHash[:],
		RuntimeAssets:         bytes.NewReader(assets),
		RuntimeAssetsHash:     assetsHash[:],
		Version:               "1.2.3",
		Dir:                   tempDir,
	})

	gotWrapper, err := installRuntimeAt(tempDir)
	if err != nil {
		t.Fatal(err)
	}
	installDir := filepath.Dir(gotWrapper)
	if got, err := os.ReadFile(filepath.Join(installDir, "ripgrep", "bin", "test-platform", "rg")); err != nil || string(got) != "ripgrep" {
		t.Fatalf("ripgrep content=%q err=%v", got, err)
	}
	if got, err := os.ReadFile(filepath.Join(installDir, "definitions", "future.json")); err != nil || string(got) != "{}" {
		t.Fatalf("definition content=%q err=%v", got, err)
	}
}

type assetFixture struct {
	content []byte
	mode    int64
}

func runtimeAssetsArchive(t *testing.T, files map[string]assetFixture) []byte {
	t.Helper()
	var buffer bytes.Buffer
	gzipWriter := gzip.NewWriter(&buffer)
	tarWriter := tar.NewWriter(gzipWriter)
	for name, fixture := range files {
		header := &tar.Header{Name: name, Mode: fixture.mode, Size: int64(len(fixture.content)), Typeflag: tar.TypeReg}
		if err := tarWriter.WriteHeader(header); err != nil {
			t.Fatal(err)
		}
		if _, err := tarWriter.Write(fixture.content); err != nil {
			t.Fatal(err)
		}
	}
	if err := tarWriter.Close(); err != nil {
		t.Fatal(err)
	}
	if err := gzipWriter.Close(); err != nil {
		t.Fatal(err)
	}
	return buffer.Bytes()
}

func TestInstallRuntimeWritesAdjacentPairWithoutCLI(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	cli := []byte("cli")
	wrapper := []byte("wrapper")
	node := []byte("runtime")
	cliHash := sha256.Sum256(cli)
	wrapperHash := sha256.Sum256(wrapper)
	nodeHash := sha256.Sum256(node)
	Setup(Config{
		Cli:                   bytes.NewReader(cli),
		CliHash:               cliHash[:],
		RuntimeExecutable:     bytes.NewReader(wrapper),
		RuntimeExecutableHash: wrapperHash[:],
		RuntimeNode:           bytes.NewReader(node),
		RuntimeNodeHash:       nodeHash[:],
		Version:               "1.2.3",
		Dir:                   tempDir,
	})

	gotWrapper, err := installRuntimeAt(tempDir)
	if err != nil {
		t.Fatal(err)
	}
	if gotWrapper != filepath.Join(tempDir, "1.2.3", runtimeExecutableName()) {
		t.Fatalf("RuntimePath() = %q", gotWrapper)
	}
	if got, err := os.ReadFile(filepath.Join(filepath.Dir(gotWrapper), "runtime.node")); err != nil || !bytes.Equal(got, node) {
		t.Fatalf("runtime.node content=%q err=%v", got, err)
	}
	if cliPath := filepath.Join(filepath.Dir(gotWrapper), binaryNameForOS()); fileExists(cliPath) {
		t.Fatalf("managed runtime installation unexpectedly materialized CLI host at %q", cliPath)
	}
}

func fileExists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

func TestInstallVerifiedFileRestoresExecutablePermission(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("Windows does not use Unix execute bits")
	}
	path := filepath.Join(t.TempDir(), "copilot-runtime")
	content := []byte("wrapper")
	hash := sha256.Sum256(content)
	if err := os.WriteFile(path, content, 0644); err != nil {
		t.Fatal(err)
	}

	if err := installVerifiedFile(path, bytes.NewReader(content), hash[:], 0755, "runtime wrapper"); err != nil {
		t.Fatal(err)
	}

	info, err := os.Stat(path)
	if err != nil {
		t.Fatal(err)
	}
	if info.Mode().Perm()&0111 == 0 {
		t.Fatal("existing runtime wrapper was not made executable")
	}
}

// stagingGate holds an installer at its first read of the artifact, after it
// has missed the fast path and started staging its own copy.
type stagingGate struct {
	r       io.Reader
	staging chan struct{}
	release chan struct{}
	once    sync.Once
}

func newStagingGate(content []byte) *stagingGate {
	return &stagingGate{
		r:       bytes.NewReader(content),
		staging: make(chan struct{}),
		release: make(chan struct{}),
	}
}

func (g *stagingGate) Read(p []byte) (int, error) {
	g.once.Do(func() {
		close(g.staging)
		<-g.release
	})
	return g.r.Read(p)
}

// installWhileStaging starts an installer that stages its copy of content and
// holds it there, runs publishEarlier, then lets the staged installer publish.
func installWhileStaging(t *testing.T, path string, content []byte, publishEarlier func()) {
	t.Helper()
	hash := sha256.Sum256(content)
	late := newStagingGate(content)
	done := make(chan error, 1)
	go func() { done <- installVerifiedFile(path, late, hash[:], 0755, "CLI binary") }()
	<-late.staging
	func() {
		defer close(late.release)
		publishEarlier()
	}()
	if err := <-done; err != nil {
		t.Fatal(err)
	}
	if staged, _ := filepath.Glob(filepath.Join(filepath.Dir(path), ".copilot-runtime-pair-*.tmp")); len(staged) != 0 {
		t.Fatalf("staged copies left behind: %v", staged)
	}
}

func TestInstallVerifiedFileKeepsIdenticalFilePublishedWhileStaging(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("Windows keeps replacing by rename; a running executable cannot be replaced there")
	}
	path := filepath.Join(t.TempDir(), "copilot")
	content := []byte("bundled CLI")
	hash := sha256.Sum256(content)

	var returned os.FileInfo
	installWhileStaging(t, path, content, func() {
		if err := installVerifiedFile(path, bytes.NewReader(content), hash[:], 0755, "CLI binary"); err != nil {
			t.Fatal(err)
		}
		var err error
		if returned, err = os.Stat(path); err != nil {
			t.Fatal(err)
		}
	})

	final, err := os.Stat(path)
	if err != nil {
		t.Fatal(err)
	}
	if !os.SameFile(returned, final) {
		t.Fatal("the later installer replaced the CLI the earlier one returned; a process started from it would run an unlinked file")
	}
}

func TestInstallVerifiedFileReplacesDifferentFilePublishedWhileStaging(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("Windows does not use Unix permission bits")
	}
	content := []byte("bundled CLI")
	tests := []struct {
		name  string
		bytes []byte
		mode  os.FileMode
	}{
		{name: "different bytes", bytes: []byte("another CLI"), mode: 0755},
		{name: "same bytes, not executable", bytes: content, mode: 0644},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			path := filepath.Join(t.TempDir(), "copilot")
			installWhileStaging(t, path, content, func() {
				if err := os.WriteFile(path, tt.bytes, tt.mode); err != nil {
					t.Fatal(err)
				}
				if err := os.Chmod(path, tt.mode); err != nil {
					t.Fatal(err)
				}
			})

			got, err := os.ReadFile(path)
			if err != nil {
				t.Fatal(err)
			}
			if !bytes.Equal(got, content) {
				t.Fatalf("installed bytes = %q, want %q", got, content)
			}
			info, err := os.Stat(path)
			if err != nil {
				t.Fatal(err)
			}
			if info.Mode().Perm() != 0755 {
				t.Fatalf("installed mode = %v, want 0755", info.Mode().Perm())
			}
		})
	}
}

func mustPanic(t *testing.T, fn func()) {
	t.Helper()
	defer func() {
		if r := recover(); r == nil {
			t.Fatalf("expected panic")
		}
	}()
	fn()
}

func binaryNameForOS() string {
	name := "copilot"
	if runtime.GOOS == "windows" {
		name += ".exe"
	}
	return name
}

func TestLinuxMuslConfigSelectsAlternativeArtifacts(t *testing.T) {
	glibcCLI := strings.NewReader("glibc-cli")
	glibcRuntime := strings.NewReader("glibc-runtime")
	muslCLI := strings.NewReader("musl-cli")
	muslRuntime := strings.NewReader("musl-runtime")
	muslCLIHash := bytes.Repeat([]byte{1}, sha256.Size)
	muslRuntimeHash := bytes.Repeat([]byte{2}, sha256.Size)

	selected := linuxMuslConfig(Config{
		Cli:                     glibcCLI,
		RuntimeLib:              glibcRuntime,
		LinuxMuslCli:            muslCLI,
		LinuxMuslCliHash:        muslCLIHash,
		LinuxMuslRuntimeLib:     muslRuntime,
		LinuxMuslRuntimeLibHash: muslRuntimeHash,
	})

	if selected.Cli != muslCLI || selected.RuntimeLib != muslRuntime {
		t.Fatal("Expected Linux musl artifacts to replace the glibc artifacts")
	}
	if !bytes.Equal(selected.CliHash, muslCLIHash) || !bytes.Equal(selected.RuntimeLibHash, muslRuntimeHash) {
		t.Fatal("Expected Linux musl hashes to replace the glibc hashes")
	}
}

func TestSetupPanicsOnNilCli(t *testing.T) {
	resetGlobals()
	mustPanic(t, func() { Setup(Config{}) })
}

func TestSetupPanicsOnSecondCall(t *testing.T) {
	resetGlobals()
	hash := sha256.Sum256([]byte("ok"))
	Setup(Config{Cli: bytes.NewReader([]byte("ok")), CliHash: hash[:]})
	hash2 := sha256.Sum256([]byte("ok"))
	mustPanic(t, func() { Setup(Config{Cli: bytes.NewReader([]byte("ok")), CliHash: hash2[:]}) })
	resetGlobals()
}

func TestInstallAtWritesBinaryAndLicense(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	content := []byte("hello")
	hash := sha256.Sum256(content)
	Setup(Config{
		Cli:     bytes.NewReader(content),
		CliHash: hash[:],
		License: []byte("license"),
		Version: "1.2.3",
		Dir:     tempDir,
	})

	path := Path()

	expectedPath := filepath.Join(tempDir, "1.2.3", binaryNameForOS())
	if path != expectedPath {
		t.Fatalf("unexpected path: got %q want %q", path, expectedPath)
	}

	got, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read binary: %v", err)
	}
	if !bytes.Equal(got, content) {
		t.Fatalf("binary content mismatch")
	}

	licensePath := path + ".license"
	license, err := os.ReadFile(licensePath)
	if err != nil {
		t.Fatalf("read license: %v", err)
	}
	if string(license) != "license" {
		t.Fatalf("license content mismatch")
	}

	gotHash, err := hashFile(path)
	if err != nil {
		t.Fatalf("hash file: %v", err)
	}
	if !bytes.Equal(gotHash, hash[:]) {
		t.Fatalf("hash mismatch")
	}
}

func TestInstallAtInstallsRuntimePairAndAssetsWithoutRuntimeLib(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	wrapper := []byte("wrapper")
	node := []byte("runtime")
	assets := runtimeAssetsArchive(t, map[string]assetFixture{
		"definitions/future.json": {content: []byte("{}"), mode: 0644},
	})
	wrapperHash := sha256.Sum256(wrapper)
	nodeHash := sha256.Sum256(node)
	assetsHash := sha256.Sum256(assets)
	Setup(Config{
		Cli:                   bytes.NewReader(wrapper),
		CliHash:               wrapperHash[:],
		RuntimeExecutable:     bytes.NewReader(wrapper),
		RuntimeExecutableHash: wrapperHash[:],
		RuntimeNode:           bytes.NewReader(node),
		RuntimeNodeHash:       nodeHash[:],
		RuntimeAssets:         bytes.NewReader(assets),
		RuntimeAssetsHash:     assetsHash[:],
		Version:               "1.2.3",
		Dir:                   tempDir,
	})

	path, err := installAt(tempDir)
	if err != nil {
		t.Fatal(err)
	}
	installDir := filepath.Dir(path)
	if got, err := os.ReadFile(filepath.Join(installDir, runtimeExecutableName())); err != nil || !bytes.Equal(got, wrapper) {
		t.Fatalf("runtime wrapper content=%q err=%v", got, err)
	}
	if got, err := os.ReadFile(filepath.Join(installDir, "runtime.node")); err != nil || !bytes.Equal(got, node) {
		t.Fatalf("runtime.node content=%q err=%v", got, err)
	}
	if got, err := os.ReadFile(filepath.Join(installDir, "definitions", "future.json")); err != nil || string(got) != "{}" {
		t.Fatalf("definition content=%q err=%v", got, err)
	}
}

func TestInstallAtExistingBinaryHashMismatch(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	binaryPath := filepath.Join(tempDir, binaryNameForOS())
	if err := os.MkdirAll(filepath.Dir(binaryPath), 0755); err != nil {
		t.Fatalf("mkdir: %v", err)
	}
	if err := os.WriteFile(binaryPath, []byte("bad"), 0755); err != nil {
		t.Fatalf("write binary: %v", err)
	}

	goodHash := sha256.Sum256([]byte("good"))
	config = Config{
		Cli:     bytes.NewReader([]byte("good")),
		CliHash: goodHash[:],
	}

	_, err := installAt(tempDir)
	if err == nil || !strings.Contains(err.Error(), "hash mismatch") {
		t.Fatalf("expected hash mismatch error, got %v", err)
	}
}

func TestInstallAtRejectsCLIBytesThatDoNotMatchCliHash(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	goodHash := sha256.Sum256([]byte("good"))
	config = Config{
		Cli:     bytes.NewReader([]byte("bad")),
		CliHash: goodHash[:],
	}

	path, err := installAt(tempDir)
	if err == nil || !strings.Contains(err.Error(), "hash mismatch") {
		t.Fatalf("installAt() = %q, %v; want a hash mismatch error", path, err)
	}
	assertNoInstalledCLI(t, tempDir)
}

func TestInstallAtFailedCLIWriteCanBeRetried(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	content := []byte("good")
	hash := sha256.Sum256(content)
	config = Config{
		Cli:     io.MultiReader(bytes.NewReader(content[:2]), iotest.ErrReader(errors.New("interrupted"))),
		CliHash: hash[:],
	}

	if path, err := installAt(tempDir); err == nil {
		t.Fatalf("installAt() = %q, nil; want the read error", path)
	}
	assertNoInstalledCLI(t, tempDir)

	config.Cli = bytes.NewReader(content)
	path, err := installAt(tempDir)
	if err != nil {
		t.Fatalf("retrying installAt(): %v", err)
	}
	if got, err := os.ReadFile(path); err != nil || !bytes.Equal(got, content) {
		t.Fatalf("installed CLI content=%q err=%v", got, err)
	}
}

func TestPathRetriesAfterFailedInstall(t *testing.T) {
	resetGlobals()
	blocked := filepath.Join(t.TempDir(), "cache")
	if err := os.WriteFile(blocked, []byte("not a directory"), 0600); err != nil {
		t.Fatal(err)
	}
	content := []byte("cli")
	hash := sha256.Sum256(content)
	Setup(Config{Cli: bytes.NewReader(content), CliHash: hash[:], Dir: blocked})

	if path := Path(); path != "" {
		t.Fatalf("Path() = %q with a file as the install directory; want empty", path)
	}
	if err := os.Remove(blocked); err != nil {
		t.Fatal(err)
	}
	path := Path()
	if path == "" {
		t.Fatal("Path() stayed empty after the install directory was fixed")
	}
	if got, err := os.ReadFile(path); err != nil || !bytes.Equal(got, content) {
		t.Fatalf("installed CLI content=%q err=%v", got, err)
	}
}

func TestInstallRuntimeAssetsCanBeRetriedAfterWriteFailure(t *testing.T) {
	resetGlobals()
	installDir := t.TempDir()
	assets := runtimeAssetsArchive(t, map[string]assetFixture{
		"definitions/future.json": {content: []byte("{}"), mode: 0644},
	})
	assetsHash := sha256.Sum256(assets)
	config = Config{
		RuntimeAssets:     bytes.NewReader(assets),
		RuntimeAssetsHash: assetsHash[:],
	}
	blocked := filepath.Join(installDir, "definitions")
	if err := os.WriteFile(blocked, []byte("not a directory"), 0600); err != nil {
		t.Fatal(err)
	}

	if err := installRuntimeAssets(installDir); err == nil {
		t.Fatal("installRuntimeAssets() succeeded with a file in the way of an asset directory")
	}
	if err := os.Remove(blocked); err != nil {
		t.Fatal(err)
	}
	if err := installRuntimeAssets(installDir); err != nil {
		t.Fatalf("retrying installRuntimeAssets(): %v", err)
	}
	if got, err := os.ReadFile(filepath.Join(installDir, "definitions", "future.json")); err != nil || string(got) != "{}" {
		t.Fatalf("asset content=%q err=%v", got, err)
	}
}

// obstructingReader creates a directory at path on its first read, so the
// install that consumes it fails when it renames the artifact into place. It
// rewinds like the bundler's generated readers. It deliberately does not
// promote WriteTo, which would bypass Read.
type obstructingReader struct {
	reader io.ReadSeeker
	path   string
}

func (r *obstructingReader) Seek(offset int64, whence int) (int64, error) {
	return r.reader.Seek(offset, whence)
}

func (r *obstructingReader) Read(p []byte) (int, error) {
	if r.path != "" {
		if err := os.Mkdir(r.path, 0755); err != nil {
			return 0, err
		}
		r.path = ""
	}
	return r.reader.Read(p)
}

func TestPathRetriesRewindableArtifactAfterWriteFailure(t *testing.T) {
	for _, artifact := range []string{"cli", "node", "wrapper", "library"} {
		t.Run(artifact, func(t *testing.T) {
			resetGlobals()
			t.Cleanup(resetGlobals)
			dir := t.TempDir()
			content := []byte("artifact")
			hash := sha256.Sum256(content)
			cfg := Config{
				Cli: bytes.NewReader(content), CliHash: hash[:],
				RuntimeNode: bytes.NewReader(content), RuntimeNodeHash: hash[:],
				RuntimeExecutable: bytes.NewReader(content), RuntimeExecutableHash: hash[:],
				RuntimeLib: bytes.NewReader(content), RuntimeLibHash: hash[:],
				Dir: dir,
			}
			reader, name := &cfg.Cli, binaryNameForOS()
			switch artifact {
			case "node":
				reader, name = &cfg.RuntimeNode, "runtime.node"
			case "wrapper":
				reader, name = &cfg.RuntimeExecutable, runtimeExecutableName()
			case "library":
				reader, name = &cfg.RuntimeLib, naturalRuntimeLibName()
			}
			blocked := filepath.Join(dir, name)
			*reader = &obstructingReader{reader: bytes.NewReader(content), path: blocked}
			Setup(cfg)
			if path := Path(); path != "" {
				t.Fatalf("Path() = %q despite obstructed artifact installation", path)
			}
			if err := os.Remove(blocked); err != nil {
				t.Fatal(err)
			}
			if path := Path(); path != filepath.Join(dir, binaryNameForOS()) {
				t.Fatalf("retry Path() = %q; want installed CLI", path)
			}
			if got, err := os.ReadFile(blocked); err != nil || !bytes.Equal(got, content) {
				t.Fatalf("retried artifact content=%q err=%v", got, err)
			}
		})
	}
}

func TestRuntimePathRetriesAssetsAfterFailedPath(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	dir := t.TempDir()
	content := []byte("artifact")
	hash := sha256.Sum256(content)
	assets := runtimeAssetsArchive(t, map[string]assetFixture{
		"definitions/future.json": {content: []byte("{}"), mode: 0644},
	})
	assetsHash := sha256.Sum256(assets)
	Setup(Config{
		Cli: bytes.NewReader(content), CliHash: hash[:],
		RuntimeNode: bytes.NewReader(content), RuntimeNodeHash: hash[:],
		RuntimeExecutable: bytes.NewReader(content), RuntimeExecutableHash: hash[:],
		RuntimeAssets: bytes.NewReader(assets), RuntimeAssetsHash: assetsHash[:],
		Dir: dir,
	})
	blocked := filepath.Join(dir, "definitions")
	if err := os.WriteFile(blocked, []byte("not a directory"), 0600); err != nil {
		t.Fatal(err)
	}
	if path := Path(); path != "" {
		t.Fatalf("Path() = %q despite obstructed assets", path)
	}
	if path, err := RuntimePath(); err == nil || path != "" {
		t.Fatalf("RuntimePath() = %q, %v; want asset installation failure", path, err)
	}
	if err := os.Remove(blocked); err != nil {
		t.Fatal(err)
	}
	if path, err := RuntimePath(); err != nil || path != filepath.Join(dir, runtimeExecutableName()) {
		t.Fatalf("retry RuntimePath() = %q, %v", path, err)
	}
	if got, err := os.ReadFile(filepath.Join(dir, "definitions", "future.json")); err != nil || string(got) != "{}" {
		t.Fatalf("retried asset content=%q err=%v", got, err)
	}
}

func TestInstallLicenseAcceptsMatchingReadOnlyLicense(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	if runtime.GOOS == "windows" || os.Geteuid() == 0 {
		t.Skip("read-only permissions are not enforced here")
	}
	dir := t.TempDir()
	cli := filepath.Join(dir, "copilot")
	license := filepath.Join(dir, "copilot.license")
	if err := os.WriteFile(license, []byte("license"), 0444); err != nil {
		t.Fatal(err)
	}
	if err := os.Chmod(dir, 0555); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = os.Chmod(dir, 0755) })
	config = Config{License: []byte("license")}
	if err := installLicense(cli); err != nil {
		t.Fatalf("installLicense() with a matching read-only license: %v", err)
	}
	config = Config{License: []byte("different")}
	if err := installLicense(cli); err == nil {
		t.Fatal("installLicense() accepted a different license it could not replace")
	}
}

func TestInstallVerifiedFileRewindsToTheReadersInitialOffset(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	content := []byte("artifact")
	hash := sha256.Sum256(content)
	path := filepath.Join(t.TempDir(), "artifact")
	stream := bytes.NewReader(append([]byte("PREFIX-"), content...))
	if _, err := stream.Seek(int64(len("PREFIX-")), io.SeekStart); err != nil {
		t.Fatal(err)
	}
	reader := newRetryableReader(&obstructingReader{reader: stream, path: path})
	if err := installVerifiedFile(path, reader, hash[:], 0644, "artifact"); err == nil {
		t.Fatal("installVerifiedFile() succeeded with an obstructed destination")
	}
	if err := os.Remove(path); err != nil {
		t.Fatal(err)
	}
	// A different destination, as when COPILOT_HOME changes between attempts,
	// must still start from the reader's original offset.
	other := filepath.Join(t.TempDir(), "artifact")
	if err := installVerifiedFile(other, reader, hash[:], 0644, "artifact"); err != nil {
		t.Fatalf("retry installVerifiedFile() in another directory: %v", err)
	}
	if got, err := os.ReadFile(other); err != nil || !bytes.Equal(got, content) {
		t.Fatalf("installed content=%q err=%v", got, err)
	}
}

// plainReader hides every method but Read, like a raw zstd decoder or file.
type plainReader struct{ io.Reader }

// obstructingFile obstructs the destination on first read, like
// obstructingReader, but delegates to a real file so Close is observable.
type obstructingFile struct {
	*os.File
	path string
}

func (r *obstructingFile) Read(p []byte) (int, error) {
	if r.path != "" {
		if err := os.Mkdir(r.path, 0755); err != nil {
			return 0, err
		}
		r.path = ""
	}
	return r.File.Read(p)
}

func TestInstallVerifiedFileKeepsFileReaderOpenUntilPublished(t *testing.T) {
	content := []byte("artifact")
	hash := sha256.Sum256(content)
	dir := t.TempDir()
	source := filepath.Join(dir, "source")
	if err := os.WriteFile(source, content, 0600); err != nil {
		t.Fatal(err)
	}
	f, err := os.Open(source)
	if err != nil {
		t.Fatal(err)
	}
	defer f.Close()
	dest := filepath.Join(dir, "artifact")
	reader := newRetryableReader(&obstructingFile{File: f, path: dest})
	// The destination becomes a directory after the file was read, so the
	// first attempt fails at the rename.
	if err := installVerifiedFile(dest, reader, hash[:], 0644, "artifact"); err == nil {
		t.Fatal("installVerifiedFile() succeeded with an obstructed destination")
	}
	if err := os.Remove(dest); err != nil {
		t.Fatal(err)
	}
	if err := installVerifiedFile(dest, reader, hash[:], 0644, "artifact"); err != nil {
		t.Fatalf("retry after a failed rename: %v", err)
	}
	if got, err := os.ReadFile(dest); err != nil || !bytes.Equal(got, content) {
		t.Fatalf("installed content=%q err=%v", got, err)
	}
}

func TestInstallReleasesReplayBuffersAfterSuccess(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	content := bytes.Repeat([]byte("cli "), 4096)
	hash := sha256.Sum256(content)
	Setup(Config{Cli: &plainReader{bytes.NewReader(content)}, CliHash: hash[:], Dir: t.TempDir()})
	reader := config.Cli.(*retryableReader)
	if path := Path(); path == "" {
		t.Fatal("install failed")
	}
	if reader.buffer != nil {
		t.Fatal("replay buffer is still retained after a successful installation")
	}
}

func TestRuntimePathRetainsReadersForLaterCLIInstall(t *testing.T) {
	for _, cachedRuntime := range []bool{false, true} {
		for _, cachedCLI := range []bool{false, true} {
			t.Run(fmt.Sprintf("cachedRuntime=%t/cachedCLI=%t", cachedRuntime, cachedCLI), func(t *testing.T) {
				resetGlobals()
				t.Cleanup(resetGlobals)
				dir := t.TempDir()
				content := []byte("artifact")
				hash := sha256.Sum256(content)
				fileReader := func(name string) *os.File {
					t.Helper()
					source := filepath.Join(t.TempDir(), name)
					if err := os.WriteFile(source, content, 0600); err != nil {
						t.Fatal(err)
					}
					f, err := os.Open(source)
					if err != nil {
						t.Fatal(err)
					}
					t.Cleanup(func() { _ = f.Close() })
					return f
				}
				Setup(Config{
					Cli: fileReader("cli"), CliHash: hash[:],
					RuntimeLib: fileReader("library"), RuntimeLibHash: hash[:],
					RuntimeExecutable: fileReader("wrapper"), RuntimeExecutableHash: hash[:],
					RuntimeNode: fileReader("node"), RuntimeNodeHash: hash[:],
					Dir: dir,
				})
				if cachedRuntime {
					for _, name := range []string{runtimeExecutableName(), "runtime.node"} {
						if err := os.WriteFile(filepath.Join(dir, name), content, 0755); err != nil {
							t.Fatal(err)
						}
					}
				}
				wantCLI := filepath.Join(dir, binaryNameForOS())
				if cachedCLI {
					if err := os.WriteFile(wantCLI, content, 0755); err != nil {
						t.Fatal(err)
					}
				}
				if path, err := RuntimePath(); err != nil || path != filepath.Join(dir, runtimeExecutableName()) {
					t.Fatalf("RuntimePath() = %q, %v; want installed runtime", path, err)
				}
				if path := Path(); path != wantCLI {
					t.Fatalf("Path() after RuntimePath() = %q; want %q", path, wantCLI)
				}
				wantLib := filepath.Join(dir, naturalRuntimeLibName())
				if path := RuntimeLibPath(); path != wantLib {
					t.Fatalf("RuntimeLibPath() = %q; want %q", path, wantLib)
				}
				for _, path := range []string{wantCLI, wantLib} {
					if got, err := os.ReadFile(path); err != nil || !bytes.Equal(got, content) {
						t.Fatalf("installed artifact %q: content=%q err=%v", path, got, err)
					}
				}
			})
		}
	}
}

func TestSeekableReaderStaysOpenAcrossFailedLicenseWrite(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	content := []byte("cli")
	hash := sha256.Sum256(content)
	root := t.TempDir()
	source := filepath.Join(root, "source")
	if err := os.WriteFile(source, content, 0600); err != nil {
		t.Fatal(err)
	}
	f, err := os.Open(source)
	if err != nil {
		t.Fatal(err)
	}
	defer f.Close()
	first, second := filepath.Join(root, "first"), filepath.Join(root, "second")
	if err := os.MkdirAll(first, 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.Mkdir(filepath.Join(first, binaryNameForOS()+".license"), 0755); err != nil {
		t.Fatal(err)
	}
	Setup(Config{Cli: f, CliHash: hash[:], License: []byte("license"), Dir: first})
	if path := Path(); path != "" {
		t.Fatalf("Path() = %q although the license could not be written", path)
	}
	// Changing the install directory, as a COPILOT_HOME change would, must still
	// be able to replay the CLI from the caller's file.
	config.Dir = second
	cliPathCached = false
	if path := Path(); path == "" {
		t.Fatal("Path() could not reinstall into a new directory after the license failure")
	}
}

func TestUnusedReadersSurviveRuntimeFirstWarmCacheAndCacheSwitch(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	root := t.TempDir()
	cli, node, wrapper := []byte("cli"), []byte("node"), []byte("wrapper")
	files := map[string][]byte{"cli": cli, "node": node, "wrapper": wrapper}
	opened := map[string]*os.File{}
	for name, content := range files {
		path := filepath.Join(root, "src-"+name)
		if err := os.WriteFile(path, content, 0600); err != nil {
			t.Fatal(err)
		}
		f, err := os.Open(path)
		if err != nil {
			t.Fatal(err)
		}
		defer f.Close()
		opened[name] = f
	}
	sum := func(b []byte) []byte { h := sha256.Sum256(b); return h[:] }
	first, second := filepath.Join(root, "first"), filepath.Join(root, "second")
	if err := os.MkdirAll(first, 0755); err != nil {
		t.Fatal(err)
	}
	// A warm runtime pair in the first cache, so RuntimePath() reads nothing.
	if err := os.WriteFile(filepath.Join(first, "runtime.node"), node, 0644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(first, runtimeExecutableName()), wrapper, 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.Mkdir(filepath.Join(first, binaryNameForOS()+".license"), 0755); err != nil {
		t.Fatal(err)
	}
	Setup(Config{
		Cli: opened["cli"], CliHash: sum(cli), License: []byte("license"),
		RuntimeNode: opened["node"], RuntimeNodeHash: sum(node),
		RuntimeExecutable: opened["wrapper"], RuntimeExecutableHash: sum(wrapper),
		Dir: first,
	})
	if path, err := RuntimePath(); err != nil || path == "" {
		t.Fatalf("RuntimePath() = %q, %v", path, err)
	}
	if path := Path(); path != "" {
		t.Fatalf("Path() = %q although the license could not be written", path)
	}
	config.Dir = second
	cliPathCached = false
	if path := Path(); path == "" {
		t.Fatal("Path() could not install into a new cache after a runtime-first warm start")
	}
	for _, name := range []string{"runtime.node", runtimeExecutableName(), binaryNameForOS()} {
		if _, err := os.Stat(filepath.Join(second, name)); err != nil {
			t.Fatalf("second cache is missing %s: %v", name, err)
		}
	}
}

func TestInstallVerifiedFileRetriesNonSeekableReader(t *testing.T) {
	content := bytes.Repeat([]byte("legacy bundle "), 1024)
	hash := sha256.Sum256(content)
	path := filepath.Join(t.TempDir(), "artifact")
	reader := newRetryableReader(&plainReader{&obstructedOnce{Reader: bytes.NewReader(content), path: path}})
	if err := installVerifiedFile(path, reader, hash[:], 0644, "artifact"); err == nil {
		t.Fatal("installVerifiedFile() succeeded with an obstructed destination")
	}
	if err := os.Remove(path); err != nil {
		t.Fatal(err)
	}
	if err := installVerifiedFile(path, reader, hash[:], 0644, "artifact"); err != nil {
		t.Fatalf("retry installVerifiedFile() with a non-seekable reader: %v", err)
	}
	if got, err := os.ReadFile(path); err != nil || !bytes.Equal(got, content) {
		t.Fatalf("installed content differs (err=%v)", err)
	}
}

func TestSetupKeepsStartOnlySeekableReadersStreaming(t *testing.T) {
	content := bytes.Repeat([]byte("native runtime image "), 4096)
	var compressed bytes.Buffer
	encoder, err := zstd.NewWriter(&compressed)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := encoder.Write(content); err != nil {
		t.Fatal(err)
	}
	if err := encoder.Close(); err != nil {
		t.Fatal(err)
	}
	decoder, err := zstd.NewReader(bytes.NewReader(compressed.Bytes()))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(decoder.Close)
	wrapped, ok := newRetryableReader(&rewindableZstd{Decoder: decoder, compressed: compressed.Bytes()}).(*retryableReader)
	if !ok || wrapped.start != 0 {
		t.Fatalf("newRetryableReader() = %#v; want a streaming reader starting at 0", wrapped)
	}
	if _, err := io.CopyN(io.Discard, wrapped, 1024); err != nil {
		t.Fatal(err)
	}
	if wrapped.buffer != nil {
		t.Fatal("a start-only seekable reader was buffered in memory")
	}
}

func TestPathRetriesNonSeekableReaderConfiguredThroughSetup(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	dir := t.TempDir()
	content := bytes.Repeat([]byte("cli "), 4096)
	hash := sha256.Sum256(content)
	blocked := filepath.Join(dir, binaryNameForOS())
	Setup(Config{
		Cli:     &plainReader{&obstructedOnce{Reader: bytes.NewReader(content), path: blocked}},
		CliHash: hash[:],
		Dir:     dir,
	})
	if path := Path(); path != "" {
		t.Fatalf("Path() = %q despite an obstructed install", path)
	}
	if err := os.Remove(blocked); err != nil {
		t.Fatal(err)
	}
	if path := Path(); path != blocked {
		t.Fatalf("retry Path() = %q; want %q", path, blocked)
	}
}

type obstructedOnce struct {
	io.Reader
	path string
}

func (r *obstructedOnce) Read(p []byte) (int, error) {
	if r.path != "" {
		if err := os.Mkdir(r.path, 0755); err != nil {
			return 0, err
		}
		r.path = ""
	}
	return r.Reader.Read(p)
}

func TestInstallVerifiedFileRewindsZstdDecoderAfterWriteFailure(t *testing.T) {
	content := bytes.Repeat([]byte("native runtime image "), 4096)
	hash := sha256.Sum256(content)
	var compressed bytes.Buffer
	encoder, err := zstd.NewWriter(&compressed)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := encoder.Write(content); err != nil {
		t.Fatal(err)
	}
	if err := encoder.Close(); err != nil {
		t.Fatal(err)
	}
	decoder, err := zstd.NewReader(bytes.NewReader(compressed.Bytes()))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(decoder.Close)
	dir := t.TempDir()
	path := filepath.Join(dir, "runtime.node")
	// Obstruct the destination part-way through the stream, so the first
	// attempt fails at the rename after the decoder has been partly consumed.
	reader := &obstructingReader{
		reader: &rewindableZstd{Decoder: decoder, compressed: compressed.Bytes()},
		path:   path,
	}
	if err := installVerifiedFile(path, reader, hash[:], 0644, "runtime.node"); err == nil {
		t.Fatal("installVerifiedFile() succeeded with an obstructed destination")
	}
	if err := os.Remove(path); err != nil {
		t.Fatal(err)
	}
	if err := installVerifiedFile(path, reader, hash[:], 0644, "runtime.node"); err != nil {
		t.Fatalf("retrying installVerifiedFile(): %v", err)
	}
	if got, err := os.ReadFile(path); err != nil || !bytes.Equal(got, content) {
		t.Fatalf("installed content differs (err=%v, len=%d want %d)", err, len(got), len(content))
	}
}

// rewindableZstd mirrors the reader the bundler generates.
type rewindableZstd struct {
	*zstd.Decoder
	compressed []byte
}

func (r *rewindableZstd) Seek(offset int64, whence int) (int64, error) {
	if offset != 0 || whence != io.SeekStart {
		return 0, errors.New("only Seek(0, io.SeekStart) is supported")
	}
	return 0, r.Reset(bytes.NewReader(r.compressed))
}

func TestPathRetriesLicenseWriteBeforeCachingSuccess(t *testing.T) {
	resetGlobals()
	t.Cleanup(resetGlobals)
	dir := t.TempDir()
	content := []byte("cli")
	hash := sha256.Sum256(content)
	Setup(Config{Cli: bytes.NewReader(content), CliHash: hash[:], License: []byte("license"), Dir: dir})
	licensePath := filepath.Join(dir, binaryNameForOS()+".license")
	if err := os.Mkdir(licensePath, 0755); err != nil {
		t.Fatal(err)
	}
	if path := Path(); path != "" {
		t.Fatalf("Path() = %q although the license could not be written", path)
	}
	if err := os.Remove(licensePath); err != nil {
		t.Fatal(err)
	}
	if path := Path(); path == "" {
		t.Fatal("Path() stayed empty after the license path was cleared")
	}
	if got, err := os.ReadFile(licensePath); err != nil || string(got) != "license" {
		t.Fatalf("license content=%q err=%v", got, err)
	}
}

// assertNoInstalledCLI fails if a failed install left anything in dir other
// than the install lock.
func assertNoInstalledCLI(t *testing.T, dir string) {
	t.Helper()
	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatal(err)
	}
	for _, entry := range entries {
		if entry.Name() != ".copilot-cli.lock" {
			t.Fatalf("failed install left %q in %s", entry.Name(), dir)
		}
	}
}

func TestSanitizeVersion(t *testing.T) {
	tests := map[string]string{
		"v1.2.3+build/abc": "v1.2.3_build_abc",
		".":                "_",
		"..":               "__",
	}
	for input, want := range tests {
		if got := sanitizeVersion(input); got != want {
			t.Errorf("sanitizeVersion(%q) = %q want %q", input, got, want)
		}
	}
}

func TestInstallAtAllowsMultipleRuntimeVersions(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()

	installVersion := func(version string, cliContent, runtimeContent []byte) (string, string) {
		t.Helper()
		cliHash := sha256.Sum256(cliContent)
		runtimeHash := sha256.Sum256(runtimeContent)
		config = Config{
			Cli:            bytes.NewReader(cliContent),
			CliHash:        cliHash[:],
			RuntimeLib:     bytes.NewReader(runtimeContent),
			RuntimeLibHash: runtimeHash[:],
			Version:        version,
		}

		cliPath, err := installAt(tempDir)
		if err != nil {
			t.Fatalf("install version %s: %v", version, err)
		}
		return cliPath, runtimeLibPath
	}

	cli1, runtime1 := installVersion("1.0.0", []byte("cli-one"), []byte("runtime-one"))
	cli2, runtime2 := installVersion("2.0.0", []byte("cli-two"), []byte("runtime-two"))

	if cli1 == cli2 {
		t.Fatalf("Expected versioned CLI paths to differ, got %q", cli1)
	}
	if runtime1 == runtime2 {
		t.Fatalf("Expected versioned runtime paths to differ, got %q", runtime1)
	}
	if got, want := filepath.Base(cli1), binaryNameForOS(); got != want {
		t.Fatalf("First CLI filename = %q, want %q", got, want)
	}
	if got, want := filepath.Base(runtime1), naturalRuntimeLibName(); got != want {
		t.Fatalf("First runtime filename = %q, want %q", got, want)
	}
	if got, want := filepath.Base(filepath.Dir(cli1)), "1.0.0"; got != want {
		t.Fatalf("First CLI version directory = %q, want %q", got, want)
	}
	if filepath.Dir(cli1) != filepath.Dir(runtime1) {
		t.Fatalf("CLI and runtime were installed in different directories: %q and %q", cli1, runtime1)
	}
	if got, err := os.ReadFile(runtime1); err != nil || string(got) != "runtime-one" {
		t.Fatalf("Unexpected first runtime: content=%q err=%v", got, err)
	}
	if got, err := os.ReadFile(runtime2); err != nil || string(got) != "runtime-two" {
		t.Fatalf("Unexpected second runtime: content=%q err=%v", got, err)
	}
}

func TestInstallAtExistingBinaryInstallsMissingRuntime(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	versionDir := filepath.Join(tempDir, "1.2.3")
	if err := os.MkdirAll(versionDir, 0755); err != nil {
		t.Fatalf("mkdir: %v", err)
	}

	cliContent := []byte("cli")
	cliPath := filepath.Join(versionDir, binaryNameForOS())
	if err := os.WriteFile(cliPath, cliContent, 0755); err != nil {
		t.Fatalf("write CLI: %v", err)
	}
	cliHash := sha256.Sum256(cliContent)
	runtimeContent := []byte("runtime")
	runtimeHash := sha256.Sum256(runtimeContent)
	config = Config{
		Cli:            bytes.NewReader(cliContent),
		CliHash:        cliHash[:],
		RuntimeLib:     bytes.NewReader(runtimeContent),
		RuntimeLibHash: runtimeHash[:],
		Version:        "1.2.3",
	}

	gotCLIPath, err := installAt(tempDir)
	if err != nil {
		t.Fatalf("installAt(): %v", err)
	}
	if gotCLIPath != cliPath {
		t.Fatalf("installAt() = %q, want %q", gotCLIPath, cliPath)
	}
	if got, err := os.ReadFile(filepath.Join(versionDir, naturalRuntimeLibName())); err != nil || string(got) != "runtime" {
		t.Fatalf("Unexpected runtime: content=%q err=%v", got, err)
	}
}
