package main

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"go/parser"
	"go/token"
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestCreateRuntimeAssetsArchiveRetainsUnknownAssetsAndFiltersCLIContent(t *testing.T) {
	dir := t.TempDir()
	source := filepath.Join(dir, "package.tgz")
	output := filepath.Join(dir, "assets.tgz")
	writeTarGz(t, source, map[string]string{
		"package/prebuilds/linux-x64/runtime.node":    "runtime",
		"package/prebuilds/linux-x64/copilot-runtime": "wrapper",
		"package/ripgrep/bin/linux-x64/rg":            "ripgrep",
		"package/definitions/future.json":             "{}",
		"package/app.js":                              "excluded",
		"package/LICENSE.md":                          "excluded",
		"package/README.md":                           "excluded",
	})

	if err := createRuntimeAssetsArchive(source, output, platformInfo{
		npmPlatform: "linux-x64",
		binaryName:  "copilot",
	}); err != nil {
		t.Fatal(err)
	}

	files := readTarGz(t, output)
	if files["ripgrep/bin/linux-x64/rg"] != "ripgrep" || files["definitions/future.json"] != "{}" {
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
	if !strings.Contains(string(defaultSource), "//go:build !copilot_inprocess") {
		t.Fatal("default embed file does not exclude copilot_inprocess builds")
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
	if _, err := parser.ParseFile(token.NewFileSet(), "zcopilot_linux_amd64.go", defaultSource, parser.AllErrors); err != nil {
		t.Fatalf("default generated source is invalid: %v", err)
	}

	inProcessSource, err := os.ReadFile(filepath.Join(dir, "zcopilot_inprocess_linux_amd64.go"))
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(inProcessSource), "//go:build copilot_inprocess") {
		t.Fatal("in-process embed file does not require the copilot_inprocess tag")
	}
	if !strings.Contains(string(inProcessSource), "localEmbeddedCopilotRuntimeLib") {
		t.Fatal("in-process embed file does not include the native runtime")
	}
	if !strings.Contains(string(inProcessSource), "localEmbeddedCopilotCLILinuxMusl") {
		t.Fatal("in-process embed file does not include the Linux musl CLI")
	}
	if !strings.Contains(string(inProcessSource), "localEmbeddedCopilotRuntimeLibLinuxMusl") {
		t.Fatal("in-process embed file does not include the Linux musl runtime")
	}
	if _, err := parser.ParseFile(token.NewFileSet(), "zcopilot_inprocess_linux_amd64.go", inProcessSource, parser.AllErrors); err != nil {
		t.Fatalf("in-process generated source is invalid: %v", err)
	}
}
