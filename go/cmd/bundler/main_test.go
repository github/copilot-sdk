package main

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"crypto/sha256"
	"fmt"
	"go/parser"
	"go/token"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)

func TestFindReleaseChecksum(t *testing.T) {
	expected := strings.Repeat("a", 64)
	checksums := strings.Join([]string{
		strings.Repeat("b", 64) + "  other.tgz",
		expected + " *github-copilot-1.2.3-linux-x64.tgz",
	}, "\n")

	got, err := findReleaseChecksum(checksums, "github-copilot-1.2.3-linux-x64.tgz")
	if err != nil {
		t.Fatal(err)
	}
	if got != expected {
		t.Fatalf("findReleaseChecksum() = %q, want %q", got, expected)
	}

	if _, err := findReleaseChecksum(checksums, "missing.tgz"); err == nil {
		t.Fatal("findReleaseChecksum() succeeded for a missing asset")
	}
}

func TestReleaseAssetName(t *testing.T) {
	for _, test := range []struct {
		platform string
		want     string
	}{
		{platform: "linux-x64", want: "github-copilot-1.2.3-linux-x64.tgz"},
		{platform: "linuxmusl-arm64", want: "github-copilot-1.2.3-linuxmusl-arm64.tgz"},
		{platform: "win32-x64", want: "github-copilot-1.2.3-win32-x64.tgz"},
	} {
		t.Run(test.platform, func(t *testing.T) {
			if got := releaseAssetName("1.2.3", test.platform); got != test.want {
				t.Fatalf("releaseAssetName() = %q, want %q", got, test.want)
			}
		})
	}
}

func TestValidateCLIVersion(t *testing.T) {
	for _, version := range []string{"1.2.3", "1.2.3-4", "1.2.3-preview.1"} {
		if err := validateCLIVersion(version); err != nil {
			t.Errorf("validateCLIVersion(%q) returned %v", version, err)
		}
	}
	for _, version := range []string{"", "v1.2.3", "1.2", "../1.2.3", "1.2.3/asset"} {
		if err := validateCLIVersion(version); err == nil {
			t.Errorf("validateCLIVersion(%q) succeeded", version)
		}
	}
}

func TestGetReleaseURLRetriesTransientFailures(t *testing.T) {
	var attempts atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		if attempts.Add(1) == 1 {
			http.Error(w, "try again", http.StatusServiceUnavailable)
			return
		}
		_, _ = w.Write([]byte("ok"))
	}))
	defer server.Close()

	oldDelay := releaseRetryDelay
	releaseRetryDelay = func(time.Duration) {}
	defer func() { releaseRetryDelay = oldDelay }()

	resp, err := getReleaseURL(server.URL)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if got := attempts.Load(); got != 2 {
		t.Fatalf("attempts = %d, want 2", got)
	}
}

func TestGetReleaseURLDoesNotRetryPermanentFailure(t *testing.T) {
	var attempts atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts.Add(1)
		http.NotFound(w, r)
	}))
	defer server.Close()

	if _, err := getReleaseURL(server.URL); err == nil {
		t.Fatal("getReleaseURL() succeeded for HTTP 404")
	}
	if got := attempts.Load(); got != 1 {
		t.Fatalf("attempts = %d, want 1", got)
	}
}

func TestDownloadCLIBinaryUsesVerifiedReleaseAsset(t *testing.T) {
	dir := t.TempDir()
	archivePath := filepath.Join(dir, "source.tgz")
	writeTarGz(t, archivePath, map[string]string{"package/copilot": "binary"})
	archive, err := os.ReadFile(archivePath)
	if err != nil {
		t.Fatal(err)
	}
	assetName := "github-copilot-1.2.3-linux-x64.tgz"
	checksum := sha256.Sum256(archive)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/v1.2.3/SHA256SUMS.txt":
			fmt.Fprintf(w, "%x  %s\n", checksum, assetName)
		case "/v1.2.3/" + assetName:
			_, _ = w.Write(archive)
		default:
			http.NotFound(w, r)
		}
	}))
	defer server.Close()
	t.Setenv(releaseDownloadURLEnv, server.URL+"/")

	binaryPath, downloadedArchivePath, archiveChecksum, err := downloadCLIBinary("linux-x64", "copilot", "1.2.3", t.TempDir())
	if err != nil {
		t.Fatal(err)
	}
	binary, err := os.ReadFile(binaryPath)
	if err != nil {
		t.Fatal(err)
	}
	if string(binary) != "binary" {
		t.Fatalf("binary contents = %q, want %q", binary, "binary")
	}
	if filepath.Base(downloadedArchivePath) != assetName {
		t.Fatalf("archive name = %q, want %q", filepath.Base(downloadedArchivePath), assetName)
	}
	if archiveChecksum != fmt.Sprintf("%x", checksum) {
		t.Fatalf("archive checksum = %q, want %x", archiveChecksum, checksum)
	}
}

func TestDownloadCLIBinaryRejectsChecksumMismatch(t *testing.T) {
	dir := t.TempDir()
	archivePath := filepath.Join(dir, "source.tgz")
	writeTarGz(t, archivePath, map[string]string{"package/copilot": "binary"})
	archive, err := os.ReadFile(archivePath)
	if err != nil {
		t.Fatal(err)
	}
	assetName := "github-copilot-1.2.3-linux-x64.tgz"
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/v1.2.3/SHA256SUMS.txt":
			fmt.Fprintf(w, "%s  %s\n", strings.Repeat("0", 64), assetName)
		case "/v1.2.3/" + assetName:
			_, _ = w.Write(archive)
		default:
			http.NotFound(w, r)
		}
	}))
	defer server.Close()
	t.Setenv(releaseDownloadURLEnv, server.URL)

	destination := t.TempDir()
	_, downloadedArchivePath, _, err := downloadCLIBinary("linux-x64", "copilot", "1.2.3", destination)
	if err == nil || !strings.Contains(err.Error(), "checksum mismatch") {
		t.Fatalf("downloadCLIBinary() error = %v, want checksum mismatch", err)
	}
	if downloadedArchivePath != "" {
		t.Fatalf("downloaded archive path = %q, want empty", downloadedArchivePath)
	}
	if _, err := os.Stat(filepath.Join(destination, assetName)); !os.IsNotExist(err) {
		t.Fatalf("unverified archive was not removed: %v", err)
	}
}

func TestExtractCLILicenseFromReleaseArchive(t *testing.T) {
	dir := t.TempDir()
	archivePath := filepath.Join(dir, "release.tgz")
	writeTarGz(t, archivePath, map[string]string{"package/LICENSE.md": "license text"})
	outputPath := filepath.Join(dir, "zcopilot.zst")

	if err := extractCLILicense(archivePath, outputPath); err != nil {
		t.Fatal(err)
	}
	license, err := os.ReadFile(licensePathForOutput(outputPath))
	if err != nil {
		t.Fatal(err)
	}
	if string(license) != "license text" {
		t.Fatalf("license contents = %q, want %q", license, "license text")
	}
}

func TestBuildBundleRefreshesCorruptCache(t *testing.T) {
	dir := t.TempDir()
	archivePath := filepath.Join(dir, "source.tgz")
	writeTarGz(t, archivePath, map[string]string{
		"package/copilot":                             "binary",
		"package/LICENSE.md":                          "license",
		"package/prebuilds/linux-x64/runtime.node":    "runtime",
		"package/prebuilds/linux-x64/copilot-runtime": "wrapper",
		"package/copilot-sdk/extension.js":            "extension",
	})
	archive, err := os.ReadFile(archivePath)
	if err != nil {
		t.Fatal(err)
	}
	assetName := releaseAssetName("1.2.3", "linux-x64")
	checksum := sha256.Sum256(archive)
	var requests atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requests.Add(1)
		switch r.URL.Path {
		case "/v1.2.3/SHA256SUMS.txt":
			fmt.Fprintf(w, "%x  %s\n", checksum, assetName)
		case "/v1.2.3/" + assetName:
			_, _ = w.Write(archive)
		default:
			http.NotFound(w, r)
		}
	}))
	defer server.Close()
	t.Setenv(releaseDownloadURLEnv, server.URL)

	outputPath := filepath.Join(t.TempDir(), "zcopilot.zst")
	info := platformInfo{releasePlatform: "linux-x64", binaryName: "copilot"}
	bundle, err := buildBundle(info, "1.2.3", outputPath, "linux", true)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := buildBundle(info, "1.2.3", outputPath, "linux", true); err != nil {
		t.Fatal(err)
	}
	if got := requests.Load(); got != 2 {
		t.Fatalf("release requests = %d, want 2 when reusing valid cache", got)
	}
	if err := os.WriteFile(bundle.assetsArtifactPath, []byte("corrupt"), 0644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(licensePathForOutput(outputPath), []byte("corrupt"), 0644); err != nil {
		t.Fatal(err)
	}

	if _, err := buildBundle(info, "1.2.3", outputPath, "linux", true); err != nil {
		t.Fatal(err)
	}
	if got := requests.Load(); got != 4 {
		t.Fatalf("release requests = %d, want 4 after rebuilding corrupt cache", got)
	}
	license, err := os.ReadFile(licensePathForOutput(outputPath))
	if err != nil {
		t.Fatal(err)
	}
	if string(license) != "license" {
		t.Fatalf("license contents = %q, want %q", license, "license")
	}
}

func TestCreateRuntimeAssetsArchiveRetainsUnknownAssetsAndFiltersCLIContent(t *testing.T) {
	dir := t.TempDir()
	source := filepath.Join(dir, "package.tgz")
	output := filepath.Join(dir, "assets.tgz")
	writeTarGz(t, source, map[string]string{
		"package/prebuilds/linux-x64/runtime.node":    "runtime",
		"package/prebuilds/linux-x64/copilot-runtime": "wrapper",
		"package/ripgrep/bin/linux-x64/rg":            "ripgrep",
		"package/definitions/future.json":             "{}",
		"package/copilot-sdk/extension.js":            "extension",
		"package/preloads/extension_bootstrap.mjs":    "preload",
		"package/sdk/factory.js":                      "factory",
		"package/app.js":                              "excluded",
		"package/LICENSE.md":                          "excluded",
		"package/README.md":                           "excluded",
	})

	if err := createRuntimeAssetsArchive(source, output, platformInfo{
		releasePlatform: "linux-x64",
		binaryName:      "copilot",
	}); err != nil {
		t.Fatal(err)
	}

	files := readTarGz(t, output)
	if files["ripgrep/bin/linux-x64/rg"] != "ripgrep" ||
		files["definitions/future.json"] != "{}" ||
		files["copilot-sdk/extension.js"] != "extension" ||
		files["preloads/extension_bootstrap.mjs"] != "preload" ||
		files["sdk/factory.js"] != "factory" {
		t.Fatalf("retained assets = %#v", files)
	}
	for _, excluded := range []string{
		"runtime.node", "copilot-runtime", "app.js", "LICENSE.md", "README.md",
	} {
		if _, ok := files[excluded]; ok {
			t.Fatalf("excluded asset %q was retained", excluded)
		}
	}
}

func TestHostlessRuntimePathRejectsUnsafePaths(t *testing.T) {
	for _, name := range []string{
		"package/../escape",
		"package/./asset",
		"package//asset",
		"package/C:/asset",
		`package/assets\..\escape`,
	} {
		t.Run(name, func(t *testing.T) {
			if destination, ok := hostlessRuntimePath(name, "linux-x64", "copilot-runtime"); ok {
				t.Fatalf("hostlessRuntimePath() = %q, true; want rejected", destination)
			}
		})
	}
}

func writeTarGz(t *testing.T, path string, files map[string]string) {
	t.Helper()
	var buffer bytes.Buffer
	gzipWriter := gzip.NewWriter(&buffer)
	tarWriter := tar.NewWriter(gzipWriter)
	for name, content := range files {
		header := &tar.Header{Name: name, Mode: 0755, Size: int64(len(content)), Typeflag: tar.TypeReg}
		if err := tarWriter.WriteHeader(header); err != nil {
			t.Fatal(err)
		}
		if _, err := tarWriter.Write([]byte(content)); err != nil {
			t.Fatal(err)
		}
	}
	if err := tarWriter.Close(); err != nil {
		t.Fatal(err)
	}
	if err := gzipWriter.Close(); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, buffer.Bytes(), 0644); err != nil {
		t.Fatal(err)
	}
}

func readTarGz(t *testing.T, path string) map[string]string {
	t.Helper()
	file, err := os.Open(path)
	if err != nil {
		t.Fatal(err)
	}
	defer file.Close()
	gzipReader, err := gzip.NewReader(file)
	if err != nil {
		t.Fatal(err)
	}
	files := map[string]string{}
	tarReader := tar.NewReader(gzipReader)
	for {
		header, err := tarReader.Next()
		if err == io.EOF {
			return files
		}
		if err != nil {
			t.Fatal(err)
		}
		content, err := io.ReadAll(tarReader)
		if err != nil {
			t.Fatal(err)
		}
		files[header.Name] = string(content)
	}
}

func TestDetectPackageName(t *testing.T) {
	dir := t.TempDir()
	files := map[string]string{
		"app_linux.go":                "package application\n",
		"app_test.go":                 "package application_test\n",
		"app_windows.go":              "package windowsapplication\n",
		"tagged.go":                   "//go:build windows\n\npackage windowsapplication\n",
		"zcopilot_linux_amd64.go":     "package main\n",
		"_ignored.go":                 "package ignored\n",
		"zcopilot_inprocess_linux.go": "package main\n",
	}
	for name, content := range files {
		if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0644); err != nil {
			t.Fatal(err)
		}
	}

	for _, test := range []struct {
		goos string
		want string
	}{
		{goos: "linux", want: "application"},
		{goos: "windows", want: "windowsapplication"},
	} {
		t.Run(test.goos, func(t *testing.T) {
			got, err := detectPackageName(dir, test.goos, "amd64")
			if err != nil {
				t.Fatal(err)
			}
			if got != test.want {
				t.Fatalf("detectPackageName() = %q, want %q", got, test.want)
			}
		})
	}
}

func TestDetectPackageNameFallsBackForMultiplePackages(t *testing.T) {
	dir := t.TempDir()
	for name, content := range map[string]string{
		"one.go": "package one\n",
		"two.go": "package two\n",
	} {
		if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0644); err != nil {
			t.Fatal(err)
		}
	}

	got, err := detectPackageName(dir, "linux", "amd64")
	if err == nil {
		t.Fatal("detectPackageName() succeeded for a directory containing multiple packages")
	}
	if got != defaultPackageName {
		t.Fatalf("detectPackageName() = %q, want fallback %q", got, defaultPackageName)
	}
}

func TestGenerateGoFileEmbedsRuntimeWrapperPair(t *testing.T) {
	dir := t.TempDir()
	binaryPath := filepath.Join(dir, "copilot.zst")
	runtimePath := filepath.Join(dir, "runtime.node.zst")
	wrapperPath := filepath.Join(dir, "copilot-runtime.zst")
	assetsPath := filepath.Join(dir, "runtime-assets.tgz")
	muslBinaryPath := filepath.Join(dir, "copilot-musl.zst")
	muslRuntimePath := filepath.Join(dir, "runtime-musl.node.zst")
	muslWrapperPath := filepath.Join(dir, "copilot-runtime-musl.zst")
	muslAssetsPath := filepath.Join(dir, "runtime-assets-musl.tgz")
	legacyInProcessPath := filepath.Join(dir, "zcopilot_inprocess_linux_amd64.go")
	for _, path := range []string{
		binaryPath,
		licensePathForOutput(binaryPath),
		runtimePath,
		wrapperPath,
		assetsPath,
		muslBinaryPath,
		muslRuntimePath,
		muslWrapperPath,
		muslAssetsPath,
		legacyInProcessPath,
	} {
		if err := os.WriteFile(path, []byte("test"), 0644); err != nil {
			t.Fatal(err)
		}
	}

	hash := make([]byte, 32)
	if err := generateGoFile(
		"linux",
		"amd64",
		binaryPath,
		"1.2.3",
		hash,
		runtimePath,
		hash,
		wrapperPath,
		hash,
		assetsPath,
		hash,
		muslBinaryPath,
		hash,
		muslRuntimePath,
		hash,
		muslWrapperPath,
		hash,
		muslAssetsPath,
		hash,
		"main",
	); err != nil {
		t.Fatal(err)
	}

	defaultSource, err := os.ReadFile(filepath.Join(dir, "zcopilot_linux_amd64.go"))
	if err != nil {
		t.Fatal(err)
	}
	if strings.Contains(string(defaultSource), "//go:build") {
		t.Fatal("platform embed file contains an unnecessary build constraint")
	}
	if !strings.Contains(string(defaultSource), "localEmbeddedCopilotRuntimeExecutable") {
		t.Fatal("default embed file does not include the runtime wrapper")
	}
	if !strings.Contains(string(defaultSource), "RuntimeNode:") {
		t.Fatal("default embed file does not configure runtime.node")
	}
	if !strings.Contains(string(defaultSource), "RuntimeAssets:") {
		t.Fatal("default embed file does not configure retained runtime assets")
	}
	if !strings.Contains(string(defaultSource), "localEmbeddedCopilotCLILinuxMusl") {
		t.Fatal("default embed file does not include the Linux musl CLI")
	}
	if !strings.Contains(string(defaultSource), "localEmbeddedCopilotRuntimeLibLinuxMusl") {
		t.Fatal("default embed file does not include the Linux musl runtime")
	}
	if !strings.Contains(string(defaultSource), "func zstdReader(data []byte) io.Reader") {
		t.Fatal("generated embed file does not define a shared zstd reader")
	}
	for _, obsolete := range []string{"func cliReader()", "func runtimeLibReader()", "func linuxMuslCLIReader()"} {
		if strings.Contains(string(defaultSource), obsolete) {
			t.Fatalf("generated embed file contains obsolete reader %q", obsolete)
		}
	}
	if _, err := parser.ParseFile(token.NewFileSet(), "zcopilot_linux_amd64.go", defaultSource, parser.AllErrors); err != nil {
		t.Fatalf("default generated source is invalid: %v", err)
	}

	if _, err := os.Stat(legacyInProcessPath); !os.IsNotExist(err) {
		t.Fatalf("legacy in-process embed file was not removed: %v", err)
	}
}
