package testcli

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"crypto/sha512"
	"encoding/base64"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"sync/atomic"
	"testing"

	"github.com/github/copilot-sdk/go/internal/ffihost"
)

func TestSetupFailsWhenUserCacheIsUnavailable(t *testing.T) {
	previousUserCacheDir := testUserCacheDir
	testUserCacheDir = func() (string, error) { return "", errors.New("cache unavailable") }
	t.Cleanup(func() { testUserCacheDir = previousUserCacheDir })
	t.Setenv("COPILOT_CLI_PATH", "")

	err := Setup()
	if err == nil || !strings.Contains(err.Error(), "locating user cache directory: cache unavailable") {
		t.Fatalf("Setup() error = %v", err)
	}
}

func TestSetupInstallsAndReusesRuntime(t *testing.T) {
	platform := ffihost.PrebuildsFolder()
	if platform == "" {
		t.Skipf("unsupported test platform %s/%s", runtime.GOOS, runtime.GOARCH)
	}
	version := "1.2.3"
	archive := testRuntimeArchive(t, platform, version)
	digest := sha512.Sum512(archive)
	integrity := "sha512-" + base64.StdEncoding.EncodeToString(digest[:])

	moduleRoot := t.TempDir()
	moduleDir := filepath.Join(moduleRoot, "go")
	lockDir := filepath.Join(moduleRoot, "nodejs")
	if err := os.MkdirAll(moduleDir, 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(lockDir, 0755); err != nil {
		t.Fatal(err)
	}
	lockData, err := json.Marshal(map[string]any{
		"packages": map[string]any{
			"node_modules/@github/copilot-" + platform: map[string]string{
				"version":   version,
				"integrity": integrity,
			},
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(lockDir, "package-lock.json"), lockData, 0644); err != nil {
		t.Fatal(err)
	}
	moduleData, err := json.Marshal(testModuleInfo{Version: "v1.0.0", Dir: moduleDir})
	if err != nil {
		t.Fatal(err)
	}

	var requestCount atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		requestCount.Add(1)
		writer.Write(archive)
	}))
	defer server.Close()

	previousHTTPClient := testSetupHTTPClient
	previousUserCacheDir := testUserCacheDir
	previousListSDKModule := testListSDKModule
	previousRuntimeTarballURL := testRuntimeTarballURL
	testSetupHTTPClient = server.Client()
	cacheDir := t.TempDir()
	testUserCacheDir = func() (string, error) { return cacheDir, nil }
	testListSDKModule = func() ([]byte, error) { return moduleData, nil }
	testRuntimeTarballURL = func(packageName, packageVersion string) (string, error) {
		if packageName != "@github/copilot-"+platform || packageVersion != version {
			t.Fatalf("runtime package = %s@%s", packageName, packageVersion)
		}
		return server.URL + "/runtime.tgz", nil
	}
	t.Cleanup(func() {
		testSetupHTTPClient = previousHTTPClient
		testUserCacheDir = previousUserCacheDir
		testListSDKModule = previousListSDKModule
		testRuntimeTarballURL = previousRuntimeTarballURL
	})
	t.Setenv("COPILOT_CLI_PATH", "")

	if err := Setup(); err != nil {
		t.Fatal(err)
	}
	expectedCLIPath := filepath.Join(cacheDir, "copilot-sdk", "test-runtime", version, platform, testCLIBinaryName())
	if got := os.Getenv("COPILOT_CLI_PATH"); got != expectedCLIPath {
		t.Fatalf("COPILOT_CLI_PATH = %q, want %q", got, expectedCLIPath)
	}
	if content, err := os.ReadFile(expectedCLIPath); err != nil || string(content) != "cli" {
		t.Fatalf("installed CLI content = %q, err = %v", content, err)
	}
	runtimePath, err := ffihost.ResolveLibraryPath(expectedCLIPath)
	if err != nil {
		t.Fatal(err)
	}
	expectedRuntimePath := filepath.Join(filepath.Dir(expectedCLIPath), "prebuilds", platform, "runtime.node")
	if runtimePath != expectedRuntimePath {
		t.Fatalf("runtime path = %q, want %q", runtimePath, expectedRuntimePath)
	}
	if got := requestCount.Load(); got != 1 {
		t.Fatalf("download requests = %d, want 1", got)
	}

	if err := os.Unsetenv("COPILOT_CLI_PATH"); err != nil {
		t.Fatal(err)
	}
	if err := Setup(); err != nil {
		t.Fatal(err)
	}
	if got := os.Getenv("COPILOT_CLI_PATH"); got != expectedCLIPath {
		t.Fatalf("cached COPILOT_CLI_PATH = %q, want %q", got, expectedCLIPath)
	}
	if got := requestCount.Load(); got != 1 {
		t.Fatalf("download requests after cache reuse = %d, want 1", got)
	}
}

func TestParseTestRuntimeMetadata(t *testing.T) {
	metadata, err := parseTestRuntimeMetadata(strings.NewReader(`{"packages":{"node_modules/@github/copilot-linux-x64":{"version":"1.2.3","integrity":"sha512-test"}}}`), "linux-x64")
	if err != nil {
		t.Fatal(err)
	}
	if metadata.version != "1.2.3" || metadata.integrity != "sha512-test" {
		t.Fatalf("parseTestRuntimeMetadata() = %#v", metadata)
	}
}

func TestInstalledTestRuntimePathRejectsStaleRuntime(t *testing.T) {
	packageDir := t.TempDir()
	platform := "linux-x64"
	if err := os.MkdirAll(filepath.Join(packageDir, "prebuilds", platform), 0755); err != nil {
		t.Fatal(err)
	}
	for path, content := range map[string]string{
		filepath.Join(packageDir, "package.json"):                        `{"version":"1.2.3"}`,
		filepath.Join(packageDir, testCLIBinaryName()):                   "cli",
		filepath.Join(packageDir, "prebuilds", platform, "runtime.node"): "runtime",
		filepath.Join(packageDir, ".integrity"):                          "sha512-current\n",
	} {
		if err := os.WriteFile(path, []byte(content), 0755); err != nil {
			t.Fatal(err)
		}
	}

	if _, ok := installedTestRuntimePath(packageDir, platform, testRuntimeMetadata{version: "1.2.3", integrity: "sha512-current"}); !ok {
		t.Fatal("installedTestRuntimePath() rejected a matching runtime")
	}
	if _, ok := installedTestRuntimePath(packageDir, platform, testRuntimeMetadata{version: "2.0.0", integrity: "sha512-current"}); ok {
		t.Fatal("installedTestRuntimePath() accepted a stale version")
	}
	if _, ok := installedTestRuntimePath(packageDir, platform, testRuntimeMetadata{version: "1.2.3", integrity: "sha512-new"}); ok {
		t.Fatal("installedTestRuntimePath() accepted stale integrity")
	}
}

func testRuntimeArchive(t *testing.T, platform, version string) []byte {
	t.Helper()
	var buffer bytes.Buffer
	gzipWriter := gzip.NewWriter(&buffer)
	tarWriter := tar.NewWriter(gzipWriter)
	entries := []struct {
		name    string
		content string
		mode    int64
	}{
		{name: "package/package.json", content: `{"version":"` + version + `"}`, mode: 0644},
		{name: "package/" + testCLIBinaryName(), content: "cli", mode: 0755},
		{name: "package/prebuilds/" + platform + "/runtime.node", content: "runtime", mode: 0644},
	}
	for _, entry := range entries {
		header := &tar.Header{
			Name:     entry.name,
			Mode:     entry.mode,
			Size:     int64(len(entry.content)),
			Typeflag: tar.TypeReg,
		}
		if err := tarWriter.WriteHeader(header); err != nil {
			t.Fatal(err)
		}
		if _, err := tarWriter.Write([]byte(entry.content)); err != nil {
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
