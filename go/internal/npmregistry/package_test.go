package npmregistry

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"crypto/sha512"
	"encoding/base64"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestTarballURL(t *testing.T) {
	got, err := TarballURL("@github/copilot-win32-x64", "1.2.3-0")
	if err != nil {
		t.Fatal(err)
	}
	want := "https://registry.npmjs.org/@github/copilot-win32-x64/-/copilot-win32-x64-1.2.3-0.tgz"
	if got != want {
		t.Fatalf("TarballURL() = %q, want %q", got, want)
	}
}

func TestDownloadVerifiesIntegrity(t *testing.T) {
	content := []byte("package archive")
	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Write(content)
	}))
	defer server.Close()
	hash := sha512.Sum512(content)
	integrity := "sha512-" + base64.StdEncoding.EncodeToString(hash[:])

	var destination bytes.Buffer
	if err := Download(server.Client(), server.URL, integrity, &destination); err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(destination.Bytes(), content) {
		t.Fatalf("Download() wrote %q", destination.Bytes())
	}

	wrongHash := sha512.Sum512([]byte("different"))
	err := Download(server.Client(), server.URL, "sha512-"+base64.StdEncoding.EncodeToString(wrongHash[:]), &bytes.Buffer{})
	if err == nil || !strings.Contains(err.Error(), "integrity check failed") {
		t.Fatalf("Download() error = %v", err)
	}
}

func TestExtractPackage(t *testing.T) {
	archivePath := writeTestTarball(t, map[string]testTarEntry{
		"package/package.json":                     {content: "{}"},
		"package/prebuilds/linux-x64/runtime.node": {content: "runtime"},
		"outside": {content: "ignored"},
	})
	destination := t.TempDir()
	if err := ExtractPackage(archivePath, destination); err != nil {
		t.Fatal(err)
	}
	content, err := os.ReadFile(filepath.Join(destination, "prebuilds", "linux-x64", "runtime.node"))
	if err != nil {
		t.Fatal(err)
	}
	if string(content) != "runtime" {
		t.Fatalf("extracted runtime = %q", content)
	}
	if _, err := os.Stat(filepath.Join(destination, "outside")); !os.IsNotExist(err) {
		t.Fatalf("outside entry was extracted: %v", err)
	}
}

func TestExtractPackageRejectsUnsafeEntries(t *testing.T) {
	for name, entry := range map[string]testTarEntry{
		"path traversal": {name: "package/../../outside", content: "unsafe"},
		"link":           {name: "package/link", entryType: tar.TypeSymlink, linkName: "../outside"},
	} {
		t.Run(name, func(t *testing.T) {
			archivePath := writeTestTarball(t, map[string]testTarEntry{"entry": entry})
			if err := ExtractPackage(archivePath, t.TempDir()); err == nil {
				t.Fatal("ExtractPackage() accepted an unsafe entry")
			}
		})
	}
}

type testTarEntry struct {
	name      string
	content   string
	entryType byte
	linkName  string
}

func writeTestTarball(t *testing.T, entries map[string]testTarEntry) string {
	t.Helper()
	path := filepath.Join(t.TempDir(), "package.tgz")
	file, err := os.Create(path)
	if err != nil {
		t.Fatal(err)
	}
	gzipWriter := gzip.NewWriter(file)
	tarWriter := tar.NewWriter(gzipWriter)
	for defaultName, entry := range entries {
		name := entry.name
		if name == "" {
			name = defaultName
		}
		entryType := entry.entryType
		if entryType == 0 {
			entryType = tar.TypeReg
		}
		header := &tar.Header{
			Name:     name,
			Mode:     0644,
			Size:     int64(len(entry.content)),
			Typeflag: entryType,
			Linkname: entry.linkName,
		}
		if err := tarWriter.WriteHeader(header); err != nil {
			t.Fatal(err)
		}
		if entry.content != "" {
			if _, err := tarWriter.Write([]byte(entry.content)); err != nil {
				t.Fatal(err)
			}
		}
	}
	if err := tarWriter.Close(); err != nil {
		t.Fatal(err)
	}
	if err := gzipWriter.Close(); err != nil {
		t.Fatal(err)
	}
	if err := file.Close(); err != nil {
		t.Fatal(err)
	}
	return path
}
