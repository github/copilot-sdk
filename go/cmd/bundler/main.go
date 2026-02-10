// Bundler downloads Copilot CLI binaries and packages them as a binary file,
// along with a Go source file that embeds the binary and metadata.
//
// Usage:
//
//	go run github.com/github/copilot-sdk/go/cmd/bundler [--platform GOOS/GOARCH] [--output DIR] [--cli-version VERSION] [--check-only]
//
//	--platform: Target platform using Go conventions (linux/amd64, linux/arm64, darwin/amd64, darwin/arm64, windows/amd64, windows/arm64). Defaults to current platform.
//	--output: Output directory for embedded artifacts. Defaults to the current directory.
//	--cli-version: CLI version to download. If not specified, automatically detects from the copilot-sdk version in go.mod.
//	--check-only: Check that embedded CLI version matches the detected version from package-lock.json without downloading. Exits with error if versions don't match.
package main

import (
	"archive/tar"
	"compress/gzip"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"

	"github.com/klauspost/compress/zstd"
)

const (
	// Keep these URLs centralized so reviewers can verify all outbound calls in one place.
	sdkModule         = "github.com/github/copilot-sdk/go"
	packageLockURLFmt = "https://raw.githubusercontent.com/github/copilot-sdk/%s/nodejs/package-lock.json"
	tarballURLFmt     = "https://registry.npmjs.org/@github/copilot-%s/-/copilot-%s-%s.tgz"
	licenseTarballFmt = "https://registry.npmjs.org/@github/copilot/-/copilot-%s.tgz"
)

// Platform info: npm package suffix, binary name
type platformInfo struct {
	npmPlatform string
	binaryName  string
}

// Map from GOOS/GOARCH to npm platform info
var platforms = map[string]platformInfo{
	"linux/amd64":   {npmPlatform: "linux-x64", binaryName: "copilot"},
	"linux/arm64":   {npmPlatform: "linux-arm64", binaryName: "copilot"},
	"darwin/amd64":  {npmPlatform: "darwin-x64", binaryName: "copilot"},
	"darwin/arm64":  {npmPlatform: "darwin-arm64", binaryName: "copilot"},
	"windows/amd64": {npmPlatform: "win32-x64", binaryName: "copilot.exe"},
	"windows/arm64": {npmPlatform: "win32-arm64", binaryName: "copilot.exe"},
}

// main is the CLI entry point.
func main() {
	platform := flag.String("platform", runtime.GOOS+"/"+runtime.GOARCH, "Target platform as GOOS/GOARCH (e.g. linux/amd64, darwin/arm64), defaults to current platform")
	output := flag.String("output", "", "Output directory for embedded artifacts. Defaults to the current directory")
	cliVersion := flag.String("cli-version", "", "CLI version to download (auto-detected from go.mod if not specified)")
	checkOnly := flag.Bool("check-only", false, "Check that embedded CLI version matches the detected version from go.mod without downloading or updating the embedded files. Exits with error if versions don't match.")
	flag.Parse()

	// Resolve version first so the default output name can include it.
	version := resolveCLIVersion(*cliVersion)
	// Resolve platform once to validate input and get the npm package mapping.
	goos, goarch, info, err := resolvePlatform(*platform)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		fmt.Fprintf(os.Stderr, "Valid platforms: %s\n", strings.Join(validPlatforms(), ", "))
		os.Exit(1)
	}

	outputPath := filepath.Join(*output, defaultOutputFileName(version, goos, goarch, info.binaryName))

	if *checkOnly {
		fmt.Printf("Check only: detected CLI version %s from go.mod\n", version)
		fmt.Printf("Check only: verifying embedded version for %s\n", *platform)

		// Check if existing embedded version matches
		if err := checkEmbeddedVersion(version, goos, goarch, *output); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

		fmt.Println("Check only: embedded version matches detected version")
		return
	}

	fmt.Printf("Building bundle for %s (CLI version %s)\n", *platform, version)

	binaryPath, sha256Hash, err := buildBundle(info, version, outputPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	// Generate the Go file with embed directive
	if err := generateGoFile(goos, goarch, binaryPath, version, sha256Hash, "main"); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	if err := ensureZstdDependency(); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
}

// resolvePlatform validates the platform flag and returns GOOS/GOARCH and mapping info.
func resolvePlatform(platform string) (string, string, platformInfo, error) {
	goos, goarch, ok := strings.Cut(platform, "/")
	if !ok || goos == "" || goarch == "" {
		return "", "", platformInfo{}, fmt.Errorf("invalid platform %q", platform)
	}
	info, ok := platforms[platform]
	if !ok {
		return "", "", platformInfo{}, fmt.Errorf("invalid platform %q", platform)
	}
	return goos, goarch, info, nil
}

// resolveCLIVersion determines the CLI version from the flag or repo metadata.
func resolveCLIVersion(flagValue string) string {
	if flagValue != "" {
		return flagValue
	}
	version, err := detectCLIVersion()
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error detecting CLI version: %v\n", err)
		fmt.Fprintln(os.Stderr, "Hint: specify --cli-version explicitly, or run from a Go module that depends on github.com/github/copilot-sdk/go")
		os.Exit(1)
	}
	fmt.Printf("Auto-detected CLI version: %s\n", version)
	return version
}

// defaultOutputFileName builds the default bundle filename for a platform.
func defaultOutputFileName(version, goos, goarch, binaryName string) string {
	base := strings.TrimSuffix(binaryName, filepath.Ext(binaryName))
	ext := filepath.Ext(binaryName)
	return fmt.Sprintf("z%s_%s_%s_%s%s.zst", base, version, goos, goarch, ext)
}

// validPlatforms returns valid platform keys for error messages.
func validPlatforms() []string {
	result := make([]string, 0, len(platforms))
	for p := range platforms {
		result = append(result, p)
	}
	return result
}

// detectCLIVersion detects the CLI version by:
// 1. Running "go list -m" to get the copilot-sdk version from the user's go.mod
// 2. Fetching the package-lock.json from the SDK repo at that version
// 3. Extracting the @github/copilot CLI version from it
func detectCLIVersion() (string, error) {
	// Get the SDK version from the user's go.mod
	sdkVersion, err := getSDKVersion()
	if err != nil {
		return "", fmt.Errorf("failed to get SDK version: %w", err)
	}

	fmt.Printf("Found copilot-sdk %s in go.mod\n", sdkVersion)

	// Fetch package-lock.json from the SDK repo at that version
	cliVersion, err := fetchCLIVersionFromRepo(sdkVersion)
	if err != nil {
		return "", fmt.Errorf("failed to fetch CLI version: %w", err)
	}

	return cliVersion, nil
}

// getSDKVersion runs "go list -m" to get the copilot-sdk version from go.mod
func getSDKVersion() (string, error) {
	cmd := exec.Command("go", "list", "-m", "-f", "{{.Version}}", sdkModule)
	output, err := cmd.Output()
	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			return "", fmt.Errorf("go list failed: %s", string(exitErr.Stderr))
		}
		return "", err
	}

	version := strings.TrimSpace(string(output))
	if version == "" {
		return "", fmt.Errorf("module %s not found in go.mod", sdkModule)
	}

	return version, nil
}

// fetchCLIVersionFromRepo fetches package-lock.json from GitHub and extracts the CLI version.
func fetchCLIVersionFromRepo(sdkVersion string) (string, error) {
	// Convert Go module version to Git ref
	// v0.1.0 -> v0.1.0
	// v0.1.0-beta.1 -> v0.1.0-beta.1
	// v0.0.0-20240101120000-abcdef123456 -> abcdef123456 (pseudo-version)
	gitRef := sdkVersion

	// Pseudo-versions end with a 12-character commit hash.
	// Format: vX.Y.Z-yyyymmddhhmmss-abcdefabcdef
	if idx := strings.LastIndex(sdkVersion, "-"); idx != -1 {
		suffix := sdkVersion[idx+1:]
		// Use the commit hash when present so we fetch the exact source snapshot.
		if len(suffix) == 12 && isHex(suffix) {
			gitRef = suffix
		}
	}

	url := fmt.Sprintf(packageLockURLFmt, gitRef)
	fmt.Printf("Fetching %s...\n", url)

	resp, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("failed to fetch: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("failed to fetch package-lock.json: %s", resp.Status)
	}

	var packageLock struct {
		Packages map[string]struct {
			Version string `json:"version"`
		} `json:"packages"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&packageLock); err != nil {
		return "", fmt.Errorf("failed to parse package-lock.json: %w", err)
	}

	pkg, ok := packageLock.Packages["node_modules/@github/copilot"]
	if !ok || pkg.Version == "" {
		return "", fmt.Errorf("could not find @github/copilot version in package-lock.json")
	}

	return pkg.Version, nil
}

// isHex returns true if s contains only hexadecimal characters.
func isHex(s string) bool {
	for _, c := range s {
		if (c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F') {
			return false
		}
	}
	return true
}

// buildBundle downloads the CLI binary and writes it to outputPath.
func buildBundle(info platformInfo, cliVersion, outputPath string) (string, []byte, error) {
	outputDir := filepath.Dir(outputPath)
	if outputDir == "" {
		outputDir = "."
	}

	// Check if output already exists
	if _, err := os.Stat(outputPath); err == nil {
		// Idempotent output avoids re-downloading in CI or local rebuilds.
		fmt.Printf("Output %s already exists, skipping download\n", outputPath)
		sha256Hash, err := sha256FileFromCompressed(outputPath)
		if err != nil {
			return "", nil, fmt.Errorf("failed to hash existing output: %w", err)
		}
		if err := downloadCLILicense(cliVersion, outputPath); err != nil {
			return "", nil, fmt.Errorf("failed to download CLI license: %w", err)
		}
		return outputPath, sha256Hash, nil
	}
	// Create temp directory for download
	tempDir, err := os.MkdirTemp("", "copilot-bundler-*")
	if err != nil {
		return "", nil, fmt.Errorf("failed to create temp dir: %w", err)
	}
	defer os.RemoveAll(tempDir)

	// Download the binary
	binaryPath, err := downloadCLIBinary(info.npmPlatform, info.binaryName, cliVersion, tempDir)
	if err != nil {
		return "", nil, fmt.Errorf("failed to download CLI binary: %w", err)
	}

	// Create output directory if needed
	if outputDir != "." {
		if err := os.MkdirAll(outputDir, 0755); err != nil {
			return "", nil, fmt.Errorf("failed to create output directory: %w", err)
		}
	}

	sha256Hash, err := sha256File(binaryPath)
	if err != nil {
		return "", nil, fmt.Errorf("failed to hash output binary: %w", err)
	}
	if err := compressZstdFile(binaryPath, outputPath); err != nil {
		return "", nil, fmt.Errorf("failed to write output binary: %w", err)
	}
	if err := downloadCLILicense(cliVersion, outputPath); err != nil {
		return "", nil, fmt.Errorf("failed to download CLI license: %w", err)
	}
	fmt.Printf("Successfully created %s\n", outputPath)
	return outputPath, sha256Hash, nil
}

// generateGoFile creates a Go source file that embeds the binary and metadata.
func generateGoFile(goos, goarch, binaryPath, cliVersion string, sha256Hash []byte, pkgName string) error {
	// Generate Go file path: zcopilot_linux_amd64.go (without version)
	binaryName := filepath.Base(binaryPath)
	licenseName := licenseFileName(binaryName)
	goFileName := fmt.Sprintf("zcopilot_%s_%s.go", goos, goarch)
	goFilePath := filepath.Join(filepath.Dir(binaryPath), goFileName)
	hashBase64 := ""
	if len(sha256Hash) > 0 {
		hashBase64 = base64.StdEncoding.EncodeToString(sha256Hash)
	}

	content := fmt.Sprintf(`// Code generated by copilot-sdk bundler; DO NOT EDIT.

package %s

import (
	"bytes"
	"io"
	"encoding/base64"
	_ "embed"

	"github.com/github/copilot-sdk/go/embeddedcli"
	"github.com/klauspost/compress/zstd"
)

//go:embed %s
var localEmbeddedCopilotCLI []byte

//go:embed %s
var localEmbeddedCopilotCLILicense []byte


func init() {
	embeddedcli.Setup(embeddedcli.Config{
		Cli: cliReader(),
		License: localEmbeddedCopilotCLILicense,
		Version: %q,
		CliHash: mustDecodeBase64(%q),
	})
}

func cliReader() io.Reader {
	r, err := zstd.NewReader(bytes.NewReader(localEmbeddedCopilotCLI))
	if err != nil {
		panic("failed to create zstd reader: " + err.Error())
	}
	return r
}

func mustDecodeBase64(s string) []byte {
	b, err := base64.StdEncoding.DecodeString(s)
	if err != nil {
		panic("failed to decode base64: " + err.Error())
	}
	return b
}
`, pkgName, binaryName, licenseName, cliVersion, hashBase64)

	if err := os.WriteFile(goFilePath, []byte(content), 0644); err != nil {
		return err
	}

	fmt.Printf("Generated %s\n", goFilePath)
	return nil
}

// downloadCLIBinary downloads the npm tarball and extracts the CLI binary.
func downloadCLIBinary(npmPlatform, binaryName, cliVersion, destDir string) (string, error) {
	tarballURL := fmt.Sprintf(tarballURLFmt, npmPlatform, npmPlatform, cliVersion)

	fmt.Printf("Downloading from %s...\n", tarballURL)

	resp, err := http.Get(tarballURL)
	if err != nil {
		return "", fmt.Errorf("failed to download: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("failed to download: %s", resp.Status)
	}

	// Save tarball to temp file
	tarballPath := filepath.Join(destDir, fmt.Sprintf("copilot-%s-%s.tgz", npmPlatform, cliVersion))
	tarballFile, err := os.Create(tarballPath)
	if err != nil {
		return "", fmt.Errorf("failed to create tarball file: %w", err)
	}

	if _, err := io.Copy(tarballFile, resp.Body); err != nil {
		tarballFile.Close()
		return "", fmt.Errorf("failed to save tarball: %w", err)
	}
	if err := tarballFile.Close(); err != nil {
		return "", fmt.Errorf("failed to close tarball file: %w", err)
	}

	// Extract only the CLI binary to avoid unpacking the full package tree.
	binaryPath := filepath.Join(destDir, binaryName)
	if err := extractFileFromTarball(tarballPath, destDir, "package/"+binaryName, binaryName); err != nil {
		return "", fmt.Errorf("failed to extract binary: %w", err)
	}

	// Verify binary exists
	if _, err := os.Stat(binaryPath); err != nil {
		return "", fmt.Errorf("binary not found after extraction: %w", err)
	}

	// Make executable on Unix
	if !strings.HasSuffix(binaryName, ".exe") {
		if err := os.Chmod(binaryPath, 0755); err != nil {
			return "", fmt.Errorf("failed to chmod binary: %w", err)
		}
	}

	stat, err := os.Stat(binaryPath)
	if err != nil {
		return "", fmt.Errorf("failed to stat binary: %w", err)
	}
	sizeMB := float64(stat.Size()) / 1024 / 1024
	fmt.Printf("Downloaded %s (%.1f MB)\n", binaryName, sizeMB)

	return binaryPath, nil
}

// downloadCLILicense downloads the @github/copilot package and writes its license next to outputPath.
func downloadCLILicense(cliVersion, outputPath string) error {
	outputDir := filepath.Dir(outputPath)
	if outputDir == "" {
		outputDir = "."
	}
	licensePath := licensePathForOutput(outputPath)
	if _, err := os.Stat(licensePath); err == nil {
		return nil
	}

	licenseURL := fmt.Sprintf(licenseTarballFmt, cliVersion)
	resp, err := http.Get(licenseURL)
	if err != nil {
		return fmt.Errorf("failed to download license tarball: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("failed to download license tarball: %s", resp.Status)
	}

	gzReader, err := gzip.NewReader(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to create gzip reader: %w", err)
	}
	defer gzReader.Close()

	tarReader := tar.NewReader(gzReader)
	for {
		header, err := tarReader.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return fmt.Errorf("failed to read tar: %w", err)
		}
		switch header.Name {
		case "package/LICENSE.md", "package/LICENSE":
			licenseName := filepath.Base(licensePath)
			if err := extractFileFromTarballStream(tarReader, outputDir, licenseName, os.FileMode(header.Mode)); err != nil {
				return fmt.Errorf("failed to write license: %w", err)
			}
			return nil
		}
	}

	return fmt.Errorf("license file not found in tarball")
}

func licensePathForOutput(outputPath string) string {
	if strings.HasSuffix(outputPath, ".zst") {
		return strings.TrimSuffix(outputPath, ".zst") + ".license"
	}
	return outputPath + ".license"
}

func licenseFileName(binaryName string) string {
	if strings.HasSuffix(binaryName, ".zst") {
		return strings.TrimSuffix(binaryName, ".zst") + ".license"
	}
	return binaryName + ".license"
}

// extractFileFromTarballStream writes the current tar entry to disk.
func extractFileFromTarballStream(r io.Reader, destDir, outputName string, mode os.FileMode) error {
	outPath := filepath.Join(destDir, outputName)
	outFile, err := os.OpenFile(outPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, mode)
	if err != nil {
		return fmt.Errorf("failed to create output file: %w", err)
	}
	if _, err := io.Copy(outFile, r); err != nil {
		if cerr := outFile.Close(); cerr != nil {
			return fmt.Errorf("failed to extract license: copy error: %v; close error: %w", err, cerr)
		}
		return fmt.Errorf("failed to extract license: %w", err)
	}
	return outFile.Close()
}

// extractFileFromTarball extracts a single file from a .tgz into destDir with a new name.
func extractFileFromTarball(tarballPath, destDir, targetPath, outputName string) error {
	file, err := os.Open(tarballPath)
	if err != nil {
		return err
	}
	defer file.Close()

	gzReader, err := gzip.NewReader(file)
	if err != nil {
		return fmt.Errorf("failed to create gzip reader: %w", err)
	}
	defer gzReader.Close()

	tarReader := tar.NewReader(gzReader)

	for {
		header, err := tarReader.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return fmt.Errorf("failed to read tar: %w", err)
		}

		if header.Name == targetPath {
			outPath := filepath.Join(destDir, outputName)
			outFile, err := os.OpenFile(outPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, os.FileMode(header.Mode))
			if err != nil {
				return fmt.Errorf("failed to create output file: %w", err)
			}

			if _, err := io.Copy(outFile, tarReader); err != nil {
				if cerr := outFile.Close(); cerr != nil {
					return fmt.Errorf("failed to extract binary (copy error: %v, close error: %v)", err, cerr)
				}
				return fmt.Errorf("failed to extract binary: %w", err)
			}
			if err := outFile.Close(); err != nil {
				return fmt.Errorf("failed to close output file: %w", err)
			}
			return nil
		}
	}

	return fmt.Errorf("file %q not found in tarball", targetPath)
}

// compressZstdFile compresses src into dst using zstd.
func compressZstdFile(src, dst string) error {
	srcFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer srcFile.Close()

	dstFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer dstFile.Close()

	writer, err := zstd.NewWriter(dstFile)
	if err != nil {
		return err
	}
	defer writer.Close()

	if _, err := io.Copy(writer, srcFile); err != nil {
		return err
	}
	return writer.Close()
}

// sha256HexFileFromCompressed returns SHA-256 of the decompressed zstd stream.
func sha256FileFromCompressed(path string) ([]byte, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	reader, err := zstd.NewReader(file)
	if err != nil {
		return nil, err
	}
	defer reader.Close()

	h := sha256.New()
	if _, err := io.Copy(h, reader); err != nil {
		return nil, err
	}
	return h.Sum(nil), nil
}

// sha256File returns the SHA-256 hash of a file as raw bytes.
func sha256File(path string) ([]byte, error) {
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

// ensureZstdDependency makes sure the module has the zstd dependency for generated code.
func ensureZstdDependency() error {
	cmd := exec.Command("go", "mod", "tidy")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("failed to add zstd dependency: %w\n%s", err, strings.TrimSpace(string(output)))
	}
	return nil
}

// checkEmbeddedVersion checks if an embedded CLI version exists and compares it with the detected version.
func checkEmbeddedVersion(detectedVersion, goos, goarch, outputDir string) error {
	// Look for the generated Go file for this platform
	goFileName := fmt.Sprintf("zcopilot_%s_%s.go", goos, goarch)
	goFilePath := filepath.Join(outputDir, goFileName)

	data, err := os.ReadFile(goFilePath)
	if err != nil {
		if os.IsNotExist(err) {
			// No existing embedded version, nothing to check
			return nil
		}
		return fmt.Errorf("failed to read existing Go file: %w", err)
	}

	// Extract version from the generated file
	// Looking for: Version: "x.y.z",
	re := regexp.MustCompile(`Version:\s*"([^"]+)"`)
	matches := re.FindSubmatch(data)
	if matches == nil {
		// Can't parse version, skip check
		return nil
	}

	embeddedVersion := string(matches[1])
	fmt.Printf("Found existing embedded version: %s\n", embeddedVersion)

	// Compare versions
	if embeddedVersion != detectedVersion {
		return fmt.Errorf("embedded version %s does not match detected version %s - update required", embeddedVersion, detectedVersion)
	}

	fmt.Printf("Embedded version is up to date (%s)\n", embeddedVersion)
	return nil
}
