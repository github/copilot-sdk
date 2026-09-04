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
//	--check-only: Check that embedded CLI version matches the detected version from package.json without downloading. Exits with error if versions don't match.
package main

import (
	"archive/tar"
	"compress/gzip"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"go/build"
	"go/parser"
	"go/token"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"time"

	"github.com/klauspost/compress/zstd"
)

const (
	// Keep these URLs centralized so reviewers can verify all outbound calls in one place.
	sdkModule                  = "github.com/github/copilot-sdk/go"
	packageJSONURLFmt          = "https://raw.githubusercontent.com/github/copilot-sdk/%s/nodejs/package.json"
	packageLockURLFmt          = "https://raw.githubusercontent.com/github/copilot-sdk/%s/nodejs/package-lock.json"
	defaultReleaseDownloadURL  = "https://github.com/github/copilot-cli/releases/download"
	releaseDownloadURLEnv      = "COPILOT_CLI_DOWNLOAD_BASE_URL"
	defaultPackageName         = "main"
	maxChecksumManifestSize    = 1 << 20
	maxReleaseDownloadAttempts = 3
	bundleMetadataSchema       = 1
)

var releaseHTTPClient = &http.Client{Timeout: 60 * time.Second}
var releaseRetryDelay = time.Sleep
var cliVersionPattern = regexp.MustCompile(`^\d+\.\d+\.\d+(?:-[0-9A-Za-z._-]+)?$`)

// Platform info: CLI release classifier and binary name.
type platformInfo struct {
	releasePlatform string
	binaryName      string
}

// Map from GOOS/GOARCH to CLI release platform info.
var platforms = map[string]platformInfo{
	"linux/amd64":   {releasePlatform: "linux-x64", binaryName: "copilot"},
	"linux/arm64":   {releasePlatform: "linux-arm64", binaryName: "copilot"},
	"darwin/amd64":  {releasePlatform: "darwin-x64", binaryName: "copilot"},
	"darwin/arm64":  {releasePlatform: "darwin-arm64", binaryName: "copilot"},
	"windows/amd64": {releasePlatform: "win32-x64", binaryName: "copilot.exe"},
	"windows/arm64": {releasePlatform: "win32-arm64", binaryName: "copilot.exe"},
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
	// Resolve platform once to validate input and get the release classifier.
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

	pkgName, err := detectPackageName(*output, goos, goarch)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Warning: failed to detect package name: %v; using package %s\n", err, pkgName)
	}

	fmt.Printf("Building bundle for %s (CLI version %s)\n", *platform, version)

	bundle, err := buildBundle(info, version, outputPath, goos, true)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	var muslBundle bundleArtifacts
	if goos == "linux" {
		muslInfo := platformInfo{
			releasePlatform: strings.Replace(info.releasePlatform, "linux-", "linuxmusl-", 1),
			binaryName:      info.binaryName,
		}
		muslOutputPath := filepath.Join(*output, defaultOutputFileName(version, "linuxmusl", goarch, info.binaryName))
		muslBundle, err = buildBundle(
			muslInfo,
			version,
			muslOutputPath,
			goos,
			false,
		)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}
	}
	// Generate the Go file with embed directive
	if err := generateGoFile(
		goos,
		goarch,
		bundle.binaryPath,
		version,
		bundle.binaryHash,
		bundle.runtimeArtifactPath,
		bundle.runtimeHash,
		bundle.wrapperArtifactPath,
		bundle.wrapperHash,
		bundle.assetsArtifactPath,
		bundle.assetsHash,
		muslBundle.binaryPath,
		muslBundle.binaryHash,
		muslBundle.runtimeArtifactPath,
		muslBundle.runtimeHash,
		muslBundle.wrapperArtifactPath,
		muslBundle.wrapperHash,
		muslBundle.assetsArtifactPath,
		muslBundle.assetsHash,
		pkgName,
	); err != nil {
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
	version := flagValue
	if version == "" {
		detectedVersion, err := detectCLIVersion()
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error detecting CLI version: %v\n", err)
			fmt.Fprintln(os.Stderr, "Hint: specify --cli-version explicitly, or run from a Go module that depends on github.com/github/copilot-sdk/go")
			os.Exit(1)
		}
		version = detectedVersion
		fmt.Printf("Auto-detected CLI version: %s\n", version)
	}
	if err := validateCLIVersion(version); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
	return version
}

func validateCLIVersion(version string) error {
	if !cliVersionPattern.MatchString(version) {
		return fmt.Errorf("invalid CLI version %q", version)
	}
	return nil
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

// detectPackageName reads package clauses from files that match the target
// platform and build constraints. It returns defaultPackageName with an error
// when detection fails.
func detectPackageName(dir, goos, goarch string) (string, error) {
	if dir == "" {
		dir = "."
	}

	entries, err := os.ReadDir(dir)
	if err != nil {
		return defaultPackageName, fmt.Errorf("failed to read package directory %q: %w", dir, err)
	}

	buildContext := build.Default
	buildContext.GOOS = goos
	buildContext.GOARCH = goarch

	packageName := ""
	for _, entry := range entries {
		name := entry.Name()
		if entry.IsDir() || !strings.HasSuffix(name, ".go") ||
			strings.HasPrefix(name, ".") || strings.HasPrefix(name, "_") ||
			strings.HasSuffix(name, "_test.go") || strings.HasPrefix(name, "zcopilot_") {
			continue
		}
		matches, err := buildContext.MatchFile(dir, name)
		if err != nil {
			return defaultPackageName, fmt.Errorf("failed to evaluate build constraints in %q: %w", filepath.Join(dir, name), err)
		}
		if !matches {
			continue
		}

		path := filepath.Join(dir, name)
		file, err := parser.ParseFile(token.NewFileSet(), path, nil, parser.PackageClauseOnly)
		if err != nil {
			return defaultPackageName, fmt.Errorf("failed to parse package clause in %q: %w", path, err)
		}

		if packageName == "" {
			packageName = file.Name.Name
			continue
		}
		if packageName != file.Name.Name {
			return defaultPackageName, fmt.Errorf("multiple packages %q and %q found in %q", packageName, file.Name.Name, dir)
		}
	}

	if packageName == "" {
		return defaultPackageName, fmt.Errorf("no Go package found in %q", dir)
	}
	return packageName, nil
}

// detectCLIVersion detects the CLI version by:
// 1. Running "go list -m" to get the copilot-sdk version from the user's go.mod
// 2. Fetching package.json from the SDK repo at that version
// 3. Extracting the pinned Copilot CLI version from it
func detectCLIVersion() (string, error) {
	// Get the SDK version from the user's go.mod
	sdkVersion, err := getSDKVersion()
	if err != nil {
		return "", fmt.Errorf("failed to get SDK version: %w", err)
	}

	fmt.Printf("Found copilot-sdk %s in go.mod\n", sdkVersion)

	// Fetch package.json from the SDK repo at that version
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

// fetchCLIVersionFromRepo fetches package.json from GitHub and extracts the CLI version.
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

	url := fmt.Sprintf(packageJSONURLFmt, gitRef)
	fmt.Printf("Fetching %s...\n", url)

	resp, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("failed to fetch: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("failed to fetch package.json: %s", resp.Status)
	}

	var packageJSON struct {
		CopilotCLIVersion string `json:"copilotCliVersion"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&packageJSON); err != nil {
		return "", fmt.Errorf("failed to parse package.json: %w", err)
	}

	if packageJSON.CopilotCLIVersion == "" {
		return fetchLegacyCLIVersionFromRepo(gitRef)
	}

	return packageJSON.CopilotCLIVersion, nil
}

func fetchLegacyCLIVersionFromRepo(gitRef string) (string, error) {
	url := fmt.Sprintf(packageLockURLFmt, gitRef)
	fmt.Printf("Falling back to %s...\n", url)

	resp, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("failed to fetch legacy package-lock.json: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("failed to fetch legacy package-lock.json: %s", resp.Status)
	}

	var packageLock struct {
		Packages map[string]struct {
			Version string `json:"version"`
		} `json:"packages"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&packageLock); err != nil {
		return "", fmt.Errorf("failed to parse legacy package-lock.json: %w", err)
	}
	pkg, ok := packageLock.Packages["node_modules/@github/copilot"]
	if !ok || pkg.Version == "" {
		return "", fmt.Errorf("could not find copilotCliVersion in package.json or @github/copilot in package-lock.json")
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

func isSHA256(s string) bool {
	return len(s) == sha256.Size*2 && isHex(s)
}

func findReleaseChecksum(contents, assetName string) (string, error) {
	for line := range strings.SplitSeq(contents, "\n") {
		fields := strings.Fields(line)
		if len(fields) < 2 || strings.TrimPrefix(fields[1], "*") != assetName {
			continue
		}
		if !isSHA256(fields[0]) {
			return "", fmt.Errorf("invalid SHA-256 for %s", assetName)
		}
		return strings.ToLower(fields[0]), nil
	}
	return "", fmt.Errorf("SHA256SUMS.txt does not contain %s", assetName)
}

type bundleArtifacts struct {
	binaryPath          string
	binaryHash          []byte
	runtimeArtifactPath string
	runtimeHash         []byte
	wrapperArtifactPath string
	wrapperHash         []byte
	assetsArtifactPath  string
	assetsHash          []byte
}

type bundleMetadata struct {
	Schema       int    `json:"schema"`
	CLIVersion   string `json:"cliVersion"`
	Platform     string `json:"platform"`
	ReleaseAsset string `json:"releaseAsset"`
	ReleaseHash  string `json:"releaseHash"`
	BinaryHash   string `json:"binaryHash"`
	RuntimeHash  string `json:"runtimeHash"`
	WrapperHash  string `json:"wrapperHash"`
	AssetsHash   string `json:"assetsHash"`
	LicenseHash  string `json:"licenseHash,omitempty"`
}

// buildBundle downloads the CLI and native runtime artifacts from one platform package.
func buildBundle(info platformInfo, cliVersion, outputPath, goos string, includeLicense bool) (bundleArtifacts, error) {
	outputDir := filepath.Dir(outputPath)
	if outputDir == "" {
		outputDir = "."
	}
	runtimeArtifactPath := filepath.Join(outputDir, runtimeLibArtifactName(cliVersion, info.releasePlatform, goos))
	wrapperArtifactPath := filepath.Join(outputDir, runtimeWrapperArtifactName(cliVersion, info.releasePlatform, info.binaryName))
	assetsArtifactPath := filepath.Join(outputDir, runtimeAssetsArtifactName(cliVersion, info.releasePlatform))
	artifacts := bundleArtifacts{
		binaryPath:          outputPath,
		runtimeArtifactPath: runtimeArtifactPath,
		wrapperArtifactPath: wrapperArtifactPath,
		assetsArtifactPath:  assetsArtifactPath,
	}

	if cached, ok := loadCachedBundle(artifacts, cliVersion, info.releasePlatform, includeLicense); ok {
		// Idempotent output avoids re-downloading in CI or local rebuilds.
		fmt.Printf("Output runtime bundle for %s already exists, skipping download\n", info.releasePlatform)
		return cached, nil
	}

	// Create temp directory for download
	tempDir, err := os.MkdirTemp("", "copilot-bundler-*")
	if err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to create temp dir: %w", err)
	}
	defer os.RemoveAll(tempDir)

	binaryPath, tarballPath, releaseHash, err := downloadCLIBinary(info.releasePlatform, info.binaryName, cliVersion, tempDir)
	if err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to download CLI binary: %w", err)
	}

	if outputDir != "." {
		if err := os.MkdirAll(outputDir, 0755); err != nil {
			return bundleArtifacts{}, fmt.Errorf("failed to create output directory: %w", err)
		}
	}
	if includeLicense {
		if err := extractCLILicense(tarballPath, outputPath); err != nil {
			return bundleArtifacts{}, fmt.Errorf("failed to extract CLI license: %w", err)
		}
	}

	binaryHash, err := sha256File(binaryPath)
	if err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to hash output binary: %w", err)
	}
	if err := compressZstdFile(binaryPath, outputPath); err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to write output binary: %w", err)
	}

	rawLibPath := filepath.Join(tempDir, "runtime.node")
	if err := extractFileFromTarball(
		tarballPath,
		tempDir,
		"package/prebuilds/"+info.releasePlatform+"/runtime.node",
		"runtime.node",
	); err != nil {
		return bundleArtifacts{}, fmt.Errorf("runtime package is missing prebuilds/%s/runtime.node: %w", info.releasePlatform, err)
	}
	runtimeHash, err := sha256File(rawLibPath)
	if err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to hash runtime.node: %w", err)
	}
	if err := compressZstdFile(rawLibPath, runtimeArtifactPath); err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to write runtime.node: %w", err)
	}

	wrapperName := runtimeWrapperName(info.binaryName)
	rawWrapperPath := filepath.Join(tempDir, wrapperName)
	if err := extractFileFromTarball(
		tarballPath,
		tempDir,
		"package/prebuilds/"+info.releasePlatform+"/"+wrapperName,
		wrapperName,
	); err != nil {
		return bundleArtifacts{}, fmt.Errorf("runtime package is missing prebuilds/%s/%s: %w", info.releasePlatform, wrapperName, err)
	}
	wrapperHash, err := sha256File(rawWrapperPath)
	if err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to hash runtime wrapper: %w", err)
	}
	if err := compressZstdFile(rawWrapperPath, wrapperArtifactPath); err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to write runtime wrapper: %w", err)
	}
	if err := createRuntimeAssetsArchive(tarballPath, assetsArtifactPath, info); err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to write runtime assets: %w", err)
	}
	assetsHash, err := sha256File(assetsArtifactPath)
	if err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to hash runtime assets: %w", err)
	}
	artifacts.binaryHash = binaryHash
	artifacts.runtimeHash = runtimeHash
	artifacts.wrapperHash = wrapperHash
	artifacts.assetsHash = assetsHash
	if err := writeBundleMetadata(artifacts, cliVersion, info.releasePlatform, releaseHash, includeLicense); err != nil {
		return bundleArtifacts{}, fmt.Errorf("failed to write bundle metadata: %w", err)
	}

	fmt.Printf("Successfully created %s\n", outputPath)
	fmt.Printf("Successfully created %s\n", runtimeArtifactPath)
	fmt.Printf("Successfully created %s\n", wrapperArtifactPath)
	fmt.Printf("Successfully created %s\n", assetsArtifactPath)
	return artifacts, nil
}

func loadCachedBundle(artifacts bundleArtifacts, cliVersion, platform string, includeLicense bool) (bundleArtifacts, bool) {
	requiredPaths := []string{
		artifacts.binaryPath,
		artifacts.runtimeArtifactPath,
		artifacts.wrapperArtifactPath,
		artifacts.assetsArtifactPath,
		bundleMetadataPath(artifacts.binaryPath),
	}
	if includeLicense {
		requiredPaths = append(requiredPaths, licensePathForOutput(artifacts.binaryPath))
	}
	if !filesExist(requiredPaths...) {
		return bundleArtifacts{}, false
	}

	contents, err := os.ReadFile(bundleMetadataPath(artifacts.binaryPath))
	if err != nil {
		return bundleArtifacts{}, false
	}
	var metadata bundleMetadata
	if err := json.Unmarshal(contents, &metadata); err != nil ||
		metadata.Schema != bundleMetadataSchema ||
		metadata.CLIVersion != cliVersion ||
		metadata.Platform != platform ||
		metadata.ReleaseAsset != releaseAssetName(cliVersion, platform) ||
		!isSHA256(metadata.ReleaseHash) ||
		(includeLicense && !isSHA256(metadata.LicenseHash)) {
		return bundleArtifacts{}, false
	}

	binaryHash, err := sha256FileFromCompressed(artifacts.binaryPath)
	if err != nil || hex.EncodeToString(binaryHash) != metadata.BinaryHash {
		return bundleArtifacts{}, false
	}
	runtimeHash, err := sha256FileFromCompressed(artifacts.runtimeArtifactPath)
	if err != nil || hex.EncodeToString(runtimeHash) != metadata.RuntimeHash {
		return bundleArtifacts{}, false
	}
	wrapperHash, err := sha256FileFromCompressed(artifacts.wrapperArtifactPath)
	if err != nil || hex.EncodeToString(wrapperHash) != metadata.WrapperHash {
		return bundleArtifacts{}, false
	}
	assetsHash, err := sha256File(artifacts.assetsArtifactPath)
	if err != nil || hex.EncodeToString(assetsHash) != metadata.AssetsHash {
		return bundleArtifacts{}, false
	}
	if includeLicense {
		licenseHash, err := sha256File(licensePathForOutput(artifacts.binaryPath))
		if err != nil || hex.EncodeToString(licenseHash) != metadata.LicenseHash {
			return bundleArtifacts{}, false
		}
	}

	artifacts.binaryHash = binaryHash
	artifacts.runtimeHash = runtimeHash
	artifacts.wrapperHash = wrapperHash
	artifacts.assetsHash = assetsHash
	return artifacts, true
}

func writeBundleMetadata(artifacts bundleArtifacts, cliVersion, platform, releaseHash string, includeLicense bool) error {
	metadata := bundleMetadata{
		Schema:       bundleMetadataSchema,
		CLIVersion:   cliVersion,
		Platform:     platform,
		ReleaseAsset: releaseAssetName(cliVersion, platform),
		ReleaseHash:  releaseHash,
		BinaryHash:   hex.EncodeToString(artifacts.binaryHash),
		RuntimeHash:  hex.EncodeToString(artifacts.runtimeHash),
		WrapperHash:  hex.EncodeToString(artifacts.wrapperHash),
		AssetsHash:   hex.EncodeToString(artifacts.assetsHash),
	}
	if includeLicense {
		licenseHash, err := sha256File(licensePathForOutput(artifacts.binaryPath))
		if err != nil {
			return err
		}
		metadata.LicenseHash = hex.EncodeToString(licenseHash)
	}
	contents, err := json.MarshalIndent(metadata, "", "  ")
	if err != nil {
		return err
	}
	contents = append(contents, '\n')
	return os.WriteFile(bundleMetadataPath(artifacts.binaryPath), contents, 0644)
}

func bundleMetadataPath(outputPath string) string {
	return outputPath + ".bundle.json"
}

func filesExist(paths ...string) bool {
	for _, path := range paths {
		if _, err := os.Stat(path); err != nil {
			return false
		}
	}
	return true
}

// runtimeLibArtifactName builds the compressed runtime-library artifact filename.
func runtimeLibArtifactName(version, releasePlatform, goos string) string {
	return fmt.Sprintf("zcopilotruntime_%s_%s.%s.zst", version, releasePlatform, runtimeLibExt(goos))
}

func runtimeWrapperArtifactName(version, releasePlatform, binaryName string) string {
	return fmt.Sprintf("zcopilotruntimewrapper_%s_%s_%s.zst", version, releasePlatform, runtimeWrapperName(binaryName))
}

func runtimeAssetsArtifactName(version, releasePlatform string) string {
	return fmt.Sprintf("zcopilotruntimeassets_%s_%s.tgz", version, releasePlatform)
}

func runtimeWrapperName(binaryName string) string {
	if filepath.Ext(binaryName) == ".exe" {
		return "copilot-runtime.exe"
	}
	return "copilot-runtime"
}

var hostlessExcludedTopLevel = map[string]bool{
	"app.js": true, "assets": true, "changelog.json": true, "copilot": true, "copilot.exe": true,
	"foundry-local-sdk": true, "index.js": true, "napi-oop-runtime": true, "LICENSE.md": true,
	"npm-loader.js": true, "package.json": true, "pvrecorder": true, "queries": true, "README.md": true,
	"sea-loader.js": true, "webview": true,
}

func hostlessRuntimePath(name, releasePlatform, wrapperName string) (string, bool) {
	if strings.Contains(name, `\`) {
		return "", false
	}
	relative, ok := strings.CutPrefix(name, "package/")
	if !ok || relative == "" {
		return "", false
	}
	parts := strings.Split(relative, "/")
	for _, part := range parts {
		if part == "" || part == "." || part == ".." || strings.Contains(part, ":") {
			return "", false
		}
	}
	topLevel := parts[0]
	fileName := parts[len(parts)-1]
	if hostlessExcludedTopLevel[topLevel] ||
		(strings.HasPrefix(topLevel, "tree-sitter") && strings.HasSuffix(topLevel, ".wasm")) ||
		(strings.HasPrefix(topLevel, "voice-") && strings.HasSuffix(topLevel, ".js")) ||
		fileName == "cli-native.node" || fileName == "runtime.node" || fileName == wrapperName ||
		strings.HasPrefix(fileName, "copilot-runtime-bin") {
		return "", false
	}
	for _, part := range parts {
		if part == "mediaremote-adapter" {
			return "", false
		}
	}
	if topLevel == "prebuilds" {
		if len(parts) < 3 || parts[1] != releasePlatform {
			return "", false
		}
		return strings.Join(parts[2:], "/"), true
	}
	return relative, true
}

func createRuntimeAssetsArchive(tarballPath, outputPath string, info platformInfo) error {
	sourceFile, err := os.Open(tarballPath)
	if err != nil {
		return err
	}
	defer sourceFile.Close()
	gzipReader, err := gzip.NewReader(sourceFile)
	if err != nil {
		return err
	}
	defer gzipReader.Close()
	outputFile, err := os.Create(outputPath)
	if err != nil {
		return err
	}
	defer outputFile.Close()
	gzipWriter := gzip.NewWriter(outputFile)
	tarWriter := tar.NewWriter(gzipWriter)
	count := 0
	sourceTar := tar.NewReader(gzipReader)
	for {
		header, err := sourceTar.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		if header.Typeflag != tar.TypeReg {
			continue
		}
		destination, include := hostlessRuntimePath(
			header.Name,
			info.releasePlatform,
			runtimeWrapperName(info.binaryName),
		)
		if !include {
			continue
		}
		outputHeader := &tar.Header{
			Name: destination, Mode: header.Mode, Size: header.Size, Typeflag: tar.TypeReg,
			Uid: 0, Gid: 0,
		}
		if err := tarWriter.WriteHeader(outputHeader); err != nil {
			return err
		}
		if _, err := io.Copy(tarWriter, sourceTar); err != nil {
			return err
		}
		count++
	}
	if count == 0 {
		return fmt.Errorf("runtime package contains no retained assets")
	}
	if err := tarWriter.Close(); err != nil {
		return err
	}
	return gzipWriter.Close()
}

// runtimeLibExt returns the shared-library extension for the target OS.
func runtimeLibExt(goos string) string {
	switch goos {
	case "windows":
		return "dll"
	case "darwin":
		return "dylib"
	default:
		return "so"
	}
}

// generateGoFile creates one platform-specific source file containing the CLI
// and runtime artifacts used by both managed and in-process connections.
func generateGoFile(
	goos,
	goarch,
	binaryPath,
	cliVersion string,
	sha256Hash []byte,
	runtimeArtifactPath string,
	runtimeHash []byte,
	wrapperArtifactPath string,
	wrapperHash []byte,
	assetsArtifactPath string,
	assetsHash []byte,
	muslBinaryPath string,
	muslBinaryHash []byte,
	muslRuntimeArtifactPath string,
	muslRuntimeHash []byte,
	muslWrapperArtifactPath string,
	muslWrapperHash []byte,
	muslAssetsArtifactPath string,
	muslAssetsHash []byte,
	pkgName string,
) error {
	binaryName := filepath.Base(binaryPath)
	licenseName := licenseFileName(binaryName)
	hashBase64 := ""
	if len(sha256Hash) > 0 {
		hashBase64 = base64.StdEncoding.EncodeToString(sha256Hash)
	}

	outputDir := filepath.Dir(binaryPath)
	sourcePath := filepath.Join(outputDir, fmt.Sprintf("zcopilot_%s_%s.go", goos, goarch))
	content := generatedGoFileContent(
		pkgName,
		binaryName,
		licenseName,
		cliVersion,
		hashBase64,
		runtimeArtifactPath,
		runtimeHash,
		wrapperArtifactPath,
		wrapperHash,
		assetsArtifactPath,
		assetsHash,
		muslBinaryPath,
		muslBinaryHash,
		muslRuntimeArtifactPath,
		muslRuntimeHash,
		muslWrapperArtifactPath,
		muslWrapperHash,
		muslAssetsArtifactPath,
		muslAssetsHash,
	)
	if err := os.WriteFile(sourcePath, []byte(content), 0644); err != nil {
		return err
	}

	legacyInProcessPath := filepath.Join(outputDir, fmt.Sprintf("zcopilot_inprocess_%s_%s.go", goos, goarch))
	if err := os.Remove(legacyInProcessPath); err != nil && !os.IsNotExist(err) {
		return err
	}

	fmt.Printf("Generated %s\n", sourcePath)
	return nil
}

func generatedGoFileContent(
	pkgName,
	binaryName,
	licenseName,
	cliVersion,
	hashBase64,
	runtimeArtifactPath string,
	runtimeHash []byte,
	wrapperArtifactPath string,
	wrapperHash []byte,
	assetsArtifactPath string,
	assetsHash []byte,
	muslBinaryPath string,
	muslBinaryHash []byte,
	muslRuntimeArtifactPath string,
	muslRuntimeHash []byte,
	muslWrapperArtifactPath string,
	muslWrapperHash []byte,
	muslAssetsArtifactPath string,
	muslAssetsHash []byte,
) string {
	runtimeEmbed := ""
	runtimeConfig := ""
	if runtimeArtifactPath != "" && wrapperArtifactPath != "" && assetsArtifactPath != "" {
		runtimeArtifactName := filepath.Base(runtimeArtifactPath)
		runtimeHashBase64 := base64.StdEncoding.EncodeToString(runtimeHash)
		wrapperArtifactName := filepath.Base(wrapperArtifactPath)
		wrapperHashBase64 := base64.StdEncoding.EncodeToString(wrapperHash)
		assetsArtifactName := filepath.Base(assetsArtifactPath)
		assetsHashBase64 := base64.StdEncoding.EncodeToString(assetsHash)
		runtimeEmbed = fmt.Sprintf(`
//go:embed %s
var localEmbeddedCopilotRuntimeLib []byte

//go:embed %s
var localEmbeddedCopilotRuntimeExecutable []byte

//go:embed %s
var localEmbeddedCopilotRuntimeAssets []byte
`, runtimeArtifactName, wrapperArtifactName, assetsArtifactName)
		runtimeConfig = fmt.Sprintf(`
		RuntimeLib:            zstdReader(localEmbeddedCopilotRuntimeLib),
		RuntimeLibHash:        mustDecodeBase64(%q),
		RuntimeNode:           zstdReader(localEmbeddedCopilotRuntimeLib),
		RuntimeNodeHash:       mustDecodeBase64(%q),
		RuntimeExecutable:     zstdReader(localEmbeddedCopilotRuntimeExecutable),
		RuntimeExecutableHash: mustDecodeBase64(%q),
		RuntimeAssets:         bytes.NewReader(localEmbeddedCopilotRuntimeAssets),
		RuntimeAssetsHash:     mustDecodeBase64(%q),`, runtimeHashBase64, runtimeHashBase64, wrapperHashBase64, assetsHashBase64)
	}

	muslEmbed := ""
	muslConfig := ""
	if muslBinaryPath != "" && muslRuntimeArtifactPath != "" && muslWrapperArtifactPath != "" && muslAssetsArtifactPath != "" {
		muslBinaryName := filepath.Base(muslBinaryPath)
		muslBinaryHashBase64 := base64.StdEncoding.EncodeToString(muslBinaryHash)
		muslRuntimeName := filepath.Base(muslRuntimeArtifactPath)
		muslRuntimeHashBase64 := base64.StdEncoding.EncodeToString(muslRuntimeHash)
		muslWrapperName := filepath.Base(muslWrapperArtifactPath)
		muslWrapperHashBase64 := base64.StdEncoding.EncodeToString(muslWrapperHash)
		muslAssetsName := filepath.Base(muslAssetsArtifactPath)
		muslAssetsHashBase64 := base64.StdEncoding.EncodeToString(muslAssetsHash)
		muslEmbed = fmt.Sprintf(`
//go:embed %s
var localEmbeddedCopilotCLILinuxMusl []byte

//go:embed %s
var localEmbeddedCopilotRuntimeLibLinuxMusl []byte

//go:embed %s
var localEmbeddedCopilotRuntimeExecutableLinuxMusl []byte

//go:embed %s
var localEmbeddedCopilotRuntimeAssetsLinuxMusl []byte
`, muslBinaryName, muslRuntimeName, muslWrapperName, muslAssetsName)
		muslConfig = fmt.Sprintf(`
		LinuxMuslCli:                   zstdReader(localEmbeddedCopilotCLILinuxMusl),
		LinuxMuslCliHash:               mustDecodeBase64(%q),
		LinuxMuslRuntimeLib:            zstdReader(localEmbeddedCopilotRuntimeLibLinuxMusl),
		LinuxMuslRuntimeLibHash:        mustDecodeBase64(%q),
		LinuxMuslRuntimeNode:           zstdReader(localEmbeddedCopilotRuntimeLibLinuxMusl),
		LinuxMuslRuntimeNodeHash:       mustDecodeBase64(%q),
		LinuxMuslRuntimeExecutable:     zstdReader(localEmbeddedCopilotRuntimeExecutableLinuxMusl),
		LinuxMuslRuntimeExecutableHash: mustDecodeBase64(%q),
		LinuxMuslRuntimeAssets:         bytes.NewReader(localEmbeddedCopilotRuntimeAssetsLinuxMusl),
		LinuxMuslRuntimeAssetsHash:     mustDecodeBase64(%q),`, muslBinaryHashBase64, muslRuntimeHashBase64, muslRuntimeHashBase64, muslWrapperHashBase64, muslAssetsHashBase64)
	}

	return fmt.Sprintf(`// Code generated by copilot-sdk bundler; DO NOT EDIT.

package %s

import (
	"bytes"
	"encoding/base64"
	_ "embed"
	"io"

	"github.com/github/copilot-sdk/go/embeddedcli"
	"github.com/klauspost/compress/zstd"
)

//go:embed %s
var localEmbeddedCopilotCLI []byte

//go:embed %s
var localEmbeddedCopilotCLILicense []byte
%s
%s

func init() {
	embeddedcli.Setup(embeddedcli.Config{
		Cli: zstdReader(localEmbeddedCopilotCLI),
		License: localEmbeddedCopilotCLILicense,
		Version: %q,
		CliHash: mustDecodeBase64(%q),%s%s
	})
}

func zstdReader(data []byte) io.Reader {
	r, err := zstd.NewReader(bytes.NewReader(data))
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
`, pkgName, binaryName, licenseName, runtimeEmbed, muslEmbed, cliVersion, hashBase64, runtimeConfig, muslConfig)
}

// downloadCLIBinary downloads and verifies the CLI release archive, then
// extracts the CLI binary. It returns the extracted binary path and archive path
// so callers can extract the runtime artifacts from the same verified archive.
func downloadCLIBinary(releasePlatform, binaryName, cliVersion, destDir string) (string, string, string, error) {
	assetName := releaseAssetName(cliVersion, releasePlatform)
	releaseURL := fmt.Sprintf("%s/v%s", releaseDownloadBaseURL(), cliVersion)
	expectedChecksum, err := downloadReleaseChecksum(releaseURL, assetName)
	if err != nil {
		return "", "", "", err
	}
	tarballURL := releaseURL + "/" + assetName

	fmt.Printf("Downloading from %s...\n", tarballURL)

	resp, err := getReleaseURL(tarballURL)
	if err != nil {
		return "", "", "", err
	}
	defer resp.Body.Close()

	// Save tarball to temp file
	tarballPath := filepath.Join(destDir, assetName)
	tarballFile, err := os.Create(tarballPath)
	if err != nil {
		return "", "", "", fmt.Errorf("failed to create tarball file: %w", err)
	}

	hasher := sha256.New()
	if _, err := io.Copy(io.MultiWriter(tarballFile, hasher), resp.Body); err != nil {
		tarballFile.Close()
		return "", "", "", fmt.Errorf("failed to save tarball: %w", err)
	}
	if err := tarballFile.Close(); err != nil {
		return "", "", "", fmt.Errorf("failed to close tarball file: %w", err)
	}
	actualChecksum := hex.EncodeToString(hasher.Sum(nil))
	if actualChecksum != expectedChecksum {
		_ = os.Remove(tarballPath)
		return "", "", "", fmt.Errorf("checksum mismatch for %s: expected %s, got %s", assetName, expectedChecksum, actualChecksum)
	}
	fmt.Printf("Integrity verified for %s\n", assetName)

	// Extract only the CLI binary to avoid unpacking the full package tree.
	binaryPath := filepath.Join(destDir, binaryName)
	if err := extractFileFromTarball(tarballPath, destDir, "package/"+binaryName, binaryName); err != nil {
		return "", "", "", fmt.Errorf("failed to extract binary: %w", err)
	}

	// Verify binary exists
	if _, err := os.Stat(binaryPath); err != nil {
		return "", "", "", fmt.Errorf("binary not found after extraction: %w", err)
	}

	// Make executable on Unix
	if !strings.HasSuffix(binaryName, ".exe") {
		if err := os.Chmod(binaryPath, 0755); err != nil {
			return "", "", "", fmt.Errorf("failed to chmod binary: %w", err)
		}
	}

	stat, err := os.Stat(binaryPath)
	if err != nil {
		return "", "", "", fmt.Errorf("failed to stat binary: %w", err)
	}
	sizeMB := float64(stat.Size()) / 1024 / 1024
	fmt.Printf("Downloaded %s (%.1f MB)\n", binaryName, sizeMB)

	return binaryPath, tarballPath, actualChecksum, nil
}

func releaseAssetName(cliVersion, platform string) string {
	return fmt.Sprintf("github-copilot-%s-%s.tgz", cliVersion, platform)
}

func releaseDownloadBaseURL() string {
	if value := strings.TrimRight(os.Getenv(releaseDownloadURLEnv), "/"); value != "" {
		return value
	}
	return defaultReleaseDownloadURL
}

func getReleaseURL(url string) (*http.Response, error) {
	var lastErr error
	for attempt := range maxReleaseDownloadAttempts {
		resp, err := releaseHTTPClient.Get(url)
		if err == nil {
			if resp.StatusCode == http.StatusOK {
				return resp, nil
			}
			status := resp.Status
			_ = resp.Body.Close()
			lastErr = fmt.Errorf("server returned %s", status)
			if !isRetriableHTTPStatus(resp.StatusCode) {
				return nil, fmt.Errorf("failed to download %s: %w", url, lastErr)
			}
		} else {
			lastErr = err
		}

		if attempt+1 < maxReleaseDownloadAttempts {
			releaseRetryDelay(time.Duration(1<<attempt) * time.Second)
		}
	}
	return nil, fmt.Errorf("failed to download %s after %d attempts: %w", url, maxReleaseDownloadAttempts, lastErr)
}

func isRetriableHTTPStatus(status int) bool {
	return status == http.StatusRequestTimeout || status == http.StatusTooManyRequests || status >= 500
}

func downloadReleaseChecksum(releaseURL, assetName string) (string, error) {
	checksumsURL := releaseURL + "/SHA256SUMS.txt"
	resp, err := getReleaseURL(checksumsURL)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	contents, err := io.ReadAll(io.LimitReader(resp.Body, maxChecksumManifestSize+1))
	if err != nil {
		return "", fmt.Errorf("failed to read checksums: %w", err)
	}
	if len(contents) > maxChecksumManifestSize {
		return "", fmt.Errorf("SHA256SUMS.txt exceeds %d bytes", maxChecksumManifestSize)
	}
	return findReleaseChecksum(string(contents), assetName)
}

// extractCLILicense writes the license from a verified release archive next to outputPath.
func extractCLILicense(tarballPath, outputPath string) error {
	outputDir := filepath.Dir(outputPath)
	if outputDir == "" {
		outputDir = "."
	}
	licensePath := licensePathForOutput(outputPath)

	sourceFile, err := os.Open(tarballPath)
	if err != nil {
		return fmt.Errorf("failed to open release archive: %w", err)
	}
	defer sourceFile.Close()

	gzReader, err := gzip.NewReader(sourceFile)
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
	if before, ok := strings.CutSuffix(outputPath, ".zst"); ok {
		return before + ".license"
	}
	return outputPath + ".license"
}

func licenseFileName(binaryName string) string {
	if before, ok := strings.CutSuffix(binaryName, ".zst"); ok {
		return before + ".license"
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
