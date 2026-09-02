// Package testcli installs a compatible Copilot CLI for integration tests.
package testcli

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/ffihost"
	"github.com/github/copilot-sdk/go/internal/flock"
	"github.com/github/copilot-sdk/go/internal/npmregistry"
)

const (
	testSDKModule         = "github.com/github/copilot-sdk/go"
	testPackageLockURLFmt = "https://raw.githubusercontent.com/github/copilot-sdk/%s/nodejs/package-lock.json"
)

// Setup installs the Copilot runtime package compatible with this SDK into the
// user cache and sets COPILOT_CLI_PATH. It returns any setup failure and panics
// when called outside a test binary. Downstream test packages should call Setup
// from TestMain before running their tests.
func Setup() error {
	if !testing.Testing() {
		panic("testcli.Setup may only be called from a test binary")
	}
	return setup()
}

func setup() error {
	// An explicitly provisioned runtime takes precedence and avoids all discovery
	// and download work.
	if configured := os.Getenv("COPILOT_CLI_PATH"); configured != "" {
		if !testRegularFile(configured) {
			return fmt.Errorf("COPILOT_CLI_PATH %q is not a file", configured)
		}
		return nil
	}

	// Platform names match the suffixes of the optional Copilot npm packages.
	platform := ffihost.PrebuildsFolder()
	if platform == "" {
		return fmt.Errorf("unsupported Copilot runtime platform %s/%s", runtime.GOOS, runtime.GOARCH)
	}
	metadata, err := compatibleTestRuntimeMetadata(platform)
	if err != nil {
		return err
	}
	cacheDir, err := os.UserCacheDir()
	if err != nil {
		cacheDir = os.TempDir()
	}
	// Cache each exact platform package independently so SDK versions can coexist.
	installDir := filepath.Join(cacheDir, "copilot-sdk", "test-runtime", metadata.version, platform)
	if cliPath, ok := installedTestRuntimePath(installDir, platform, metadata); ok {
		return os.Setenv("COPILOT_CLI_PATH", cliPath)
	}
	if err := os.MkdirAll(filepath.Dir(installDir), 0755); err != nil {
		return fmt.Errorf("creating Copilot test runtime cache: %w", err)
	}
	// Multiple test binaries may start concurrently and target the same cache entry.
	release, err := flock.Acquire(installDir + ".lock")
	if err != nil {
		return fmt.Errorf("locking Copilot test runtime cache: %w", err)
	}
	defer release()
	// Another process may have completed the installation while this process waited.
	if cliPath, ok := installedTestRuntimePath(installDir, platform, metadata); ok {
		return os.Setenv("COPILOT_CLI_PATH", cliPath)
	}
	if err := os.RemoveAll(installDir); err != nil {
		return fmt.Errorf("removing stale Copilot test runtime: %w", err)
	}
	// Build the runtime in a sibling directory so Rename publishes only a complete
	// installation to readers that do not hold the lock.
	stagingDir, err := os.MkdirTemp(filepath.Dir(installDir), "."+platform+"-*")
	if err != nil {
		return fmt.Errorf("creating Copilot test runtime staging directory: %w", err)
	}
	defer os.RemoveAll(stagingDir)
	archivePath, err := downloadTestRuntime(filepath.Dir(installDir), platform, metadata)
	if err != nil {
		return err
	}
	defer os.Remove(archivePath)
	if err := npmregistry.ExtractPackage(archivePath, stagingDir); err != nil {
		return fmt.Errorf("extracting @github/copilot-%s@%s: %w", platform, metadata.version, err)
	}
	cliPath := filepath.Join(stagingDir, testCLIBinaryName())
	if runtime.GOOS != "windows" {
		if err := os.Chmod(cliPath, 0755); err != nil {
			return fmt.Errorf("making Copilot test runtime executable: %w", err)
		}
	}
	// package.json proves the version, while this marker binds the cache entry to
	// the exact archive integrity pinned by the SDK lockfile.
	if err := os.WriteFile(filepath.Join(stagingDir, ".integrity"), []byte(metadata.integrity+"\n"), 0644); err != nil {
		return fmt.Errorf("writing Copilot test runtime integrity: %w", err)
	}
	cliPath, ok := installedTestRuntimePath(stagingDir, platform, metadata)
	if !ok {
		return fmt.Errorf("download did not contain a complete @github/copilot-%s@%s package", platform, metadata.version)
	}
	if err := os.Rename(stagingDir, installDir); err != nil {
		return fmt.Errorf("installing Copilot test runtime: %w", err)
	}
	cliPath = filepath.Join(installDir, testCLIBinaryName())
	if err := os.Setenv("COPILOT_CLI_PATH", cliPath); err != nil {
		return fmt.Errorf("configuring Copilot test runtime: %w", err)
	}
	return nil
}

type testModuleInfo struct {
	Version string          `json:"Version"`
	Dir     string          `json:"Dir"`
	Replace *testModuleInfo `json:"Replace"`
}

type testRuntimeMetadata struct {
	version   string
	integrity string
}

func compatibleTestRuntimeMetadata(platform string) (testRuntimeMetadata, error) {
	// Query the effective module so local replace directives are reflected in both
	// the source directory and version used below.
	output, err := exec.Command("go", "list", "-m", "-json", testSDKModule).CombinedOutput()
	if err != nil {
		return testRuntimeMetadata{}, fmt.Errorf("locating %s: %w: %s", testSDKModule, err, strings.TrimSpace(string(output)))
	}
	var module testModuleInfo
	if err := json.Unmarshal(output, &module); err != nil {
		return testRuntimeMetadata{}, fmt.Errorf("parsing Go module information: %w", err)
	}
	// A source checkout or local replacement has the monorepo's Node lockfile next
	// to the Go module. Prefer it so local SDK changes use their matching runtime.
	moduleDir := module.Dir
	if module.Replace != nil && module.Replace.Dir != "" {
		moduleDir = module.Replace.Dir
	}
	if moduleDir != "" {
		lockPath := filepath.Join(filepath.Dir(moduleDir), "nodejs", "package-lock.json")
		if lockFile, err := os.Open(lockPath); err == nil {
			defer lockFile.Close()
			return parseTestRuntimeMetadata(lockFile, platform)
		}
	}

	// Published Go module archives do not contain the sibling Node project, so use
	// the SDK version to read the same lockfile from the repository.
	version := module.Version
	if module.Replace != nil && module.Replace.Version != "" {
		version = module.Replace.Version
	}
	if version == "" {
		return testRuntimeMetadata{}, fmt.Errorf("could not resolve a published version for %s", testSDKModule)
	}
	// A pseudo-version ends in the source commit hash, which is the exact Git ref
	// corresponding to that module build.
	gitRef := version
	if index := strings.LastIndexByte(version, '-'); index >= 0 {
		suffix := version[index+1:]
		if len(suffix) == 12 && testIsHex(suffix) {
			gitRef = suffix
		}
	}
	response, err := testSetupHTTPClient.Get(fmt.Sprintf(testPackageLockURLFmt, gitRef))
	if err != nil {
		return testRuntimeMetadata{}, fmt.Errorf("fetching compatible Copilot CLI version: %w", err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return testRuntimeMetadata{}, fmt.Errorf("fetching compatible Copilot CLI version: %s", response.Status)
	}
	return parseTestRuntimeMetadata(response.Body, platform)
}

var testSetupHTTPClient = &http.Client{Timeout: 10 * time.Minute}

func parseTestRuntimeMetadata(reader io.Reader, platform string) (testRuntimeMetadata, error) {
	var packageLock struct {
		Packages map[string]struct {
			Version   string `json:"version"`
			Integrity string `json:"integrity"`
		} `json:"packages"`
	}
	if err := json.NewDecoder(reader).Decode(&packageLock); err != nil {
		return testRuntimeMetadata{}, fmt.Errorf("parsing package-lock.json: %w", err)
	}
	// The platform package entry supplies both the downloadable version and npm's
	// SHA-512 SRI value used to authenticate the archive.
	packageName := "node_modules/@github/copilot-" + platform
	entry := packageLock.Packages[packageName]
	if entry.Version == "" || entry.Integrity == "" {
		return testRuntimeMetadata{}, fmt.Errorf("%s is not pinned with integrity in package-lock.json", packageName)
	}
	return testRuntimeMetadata{version: entry.Version, integrity: entry.Integrity}, nil
}

func installedTestRuntimePath(packageDir, platform string, expected testRuntimeMetadata) (string, bool) {
	packageJSON, err := os.Open(filepath.Join(packageDir, "package.json"))
	if err != nil {
		return "", false
	}
	defer packageJSON.Close()
	var metadata struct {
		Version string `json:"version"`
	}
	if json.NewDecoder(packageJSON).Decode(&metadata) != nil || metadata.Version != expected.version {
		return "", false
	}
	integrity, err := os.ReadFile(filepath.Join(packageDir, ".integrity"))
	if err != nil || strings.TrimSpace(string(integrity)) != expected.integrity {
		return "", false
	}
	cliPath := filepath.Join(packageDir, testCLIBinaryName())
	if !testRegularFile(cliPath) || !testRegularFile(filepath.Join(packageDir, "prebuilds", platform, "runtime.node")) {
		return "", false
	}
	return cliPath, true
}

func testCLIBinaryName() string {
	if runtime.GOOS == "windows" {
		return "copilot.exe"
	}
	return "copilot"
}

func testRegularFile(path string) bool {
	info, err := os.Stat(path)
	return err == nil && info.Mode().IsRegular()
}

func testIsHex(value string) bool {
	for _, char := range value {
		if (char < '0' || char > '9') && (char < 'a' || char > 'f') && (char < 'A' || char > 'F') {
			return false
		}
	}
	return true
}

func downloadTestRuntime(destination, platform string, metadata testRuntimeMetadata) (string, error) {
	url, err := npmregistry.TarballURL("@github/copilot-"+platform, metadata.version)
	if err != nil {
		return "", err
	}
	// Download verifies the lockfile integrity while streaming to a temporary file;
	// extraction never sees an unverified archive.
	archive, err := os.CreateTemp(destination, ".copilot-runtime-*.tgz")
	if err != nil {
		return "", fmt.Errorf("creating Copilot test runtime archive: %w", err)
	}
	archivePath := archive.Name()
	if err := npmregistry.Download(testSetupHTTPClient, url, metadata.integrity, archive); err != nil {
		archive.Close()
		os.Remove(archivePath)
		return "", err
	}
	if err := archive.Close(); err != nil {
		os.Remove(archivePath)
		return "", fmt.Errorf("closing Copilot test runtime archive: %w", err)
	}
	return archivePath, nil
}
