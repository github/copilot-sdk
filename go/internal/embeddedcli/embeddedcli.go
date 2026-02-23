package embeddedcli

import (
	"bytes"
	"crypto/sha256"
	"fmt"
	"io"
	"os"
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
// system cache directory. Version is used to suffix the installed binary name to
// allow multiple versions to coexist. License, when provided, is written next
// to the installed binary.
type Config struct {
	Cli     io.Reader
	CliHash []byte

	License []byte

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

var Path = sync.OnceValue(func() string {
	setupMu.Lock()
	defer setupMu.Unlock()
	if !setupDone {
		return ""
	}
	pathInitialized = true
	path := install()
	return path
})

var (
	config          Config
	setupMu         sync.Mutex
	setupDone       bool
	pathInitialized bool
)

func install() (path string) {
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
	installDir := config.Dir
	if installDir == "" {
		var err error
		if installDir, err = os.UserCacheDir(); err != nil {
			// Fall back to temp dir if UserCacheDir is unavailable
			installDir = os.TempDir()
		}
		installDir = filepath.Join(installDir, "copilot-sdk")
	}
	path, err := installAt(installDir)
	if err != nil {
		logError("installing in configured directory", err)
		return ""
	}
	return path
}

func installAt(installDir string) (string, error) {
	if err := os.MkdirAll(installDir, 0755); err != nil {
		return "", fmt.Errorf("creating install directory: %w", err)
	}
	version := sanitizeVersion(config.Version)
	lockName := ".copilot-cli.lock"
	if version != "" {
		lockName = fmt.Sprintf(".copilot-cli-%s.lock", version)
	}

	// Best effort to prevent concurrent installs.
	if release, _ := flock.Acquire(filepath.Join(installDir, lockName)); release != nil {
		defer release()
	}

	binaryName := "copilot"
	if runtime.GOOS == "windows" {
		binaryName += ".exe"
	}
	finalPath := versionedBinaryPath(installDir, binaryName, version)

	if _, err := os.Stat(finalPath); err == nil {
		existingHash, err := hashFile(finalPath)
		if err != nil {
			return "", fmt.Errorf("hashing existing binary: %w", err)
		}
		if !bytes.Equal(existingHash, config.CliHash) {
			return "", fmt.Errorf("existing binary hash mismatch")
		}
		return finalPath, nil
	}

	f, err := os.OpenFile(finalPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0755)
	if err != nil {
		return "", fmt.Errorf("creating binary file: %w", err)
	}
	_, err = io.Copy(f, config.Cli)
	if err1 := f.Close(); err1 != nil && err == nil {
		err = err1
	}
	if closer, ok := config.Cli.(io.Closer); ok {
		closer.Close()
	}
	if err != nil {
		return "", fmt.Errorf("writing binary file: %w", err)
	}
	if len(config.License) > 0 {
		licensePath := finalPath + ".license"
		if err := os.WriteFile(licensePath, config.License, 0644); err != nil {
			return "", fmt.Errorf("writing license file: %w", err)
		}
	}
	return finalPath, nil
}

// versionedBinaryPath builds the unpacked binary filename with an optional version suffix.
func versionedBinaryPath(dir, binaryName, version string) string {
	if version == "" {
		return filepath.Join(dir, binaryName)
	}
	base := strings.TrimSuffix(binaryName, filepath.Ext(binaryName))
	ext := filepath.Ext(binaryName)
	return filepath.Join(dir, fmt.Sprintf("%s_%s%s", base, version, ext))
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
	return b.String()
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
