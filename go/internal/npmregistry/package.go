package npmregistry

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"crypto/sha512"
	"encoding/base64"
	"fmt"
	"hash"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
)

const registryURL = "https://registry.npmjs.org"

// TarballURL returns the registry tarball URL for an npm package version.
func TarballURL(packageName, version string) (string, error) {
	if packageName == "" || version == "" {
		return "", fmt.Errorf("npm package name and version are required")
	}
	baseName := packageName
	if scope, name, scoped := strings.Cut(packageName, "/"); scoped {
		if !strings.HasPrefix(scope, "@") || len(scope) == 1 || name == "" || strings.Contains(name, "/") {
			return "", fmt.Errorf("invalid npm package name %q", packageName)
		}
		baseName = name
	} else if strings.ContainsAny(packageName, `\`) {
		return "", fmt.Errorf("invalid npm package name %q", packageName)
	}
	return fmt.Sprintf(
		"%s/%s/-/%s-%s.tgz",
		registryURL,
		escapePackagePath(packageName),
		url.PathEscape(baseName),
		url.PathEscape(version),
	), nil
}

func escapePackagePath(packageName string) string {
	parts := strings.Split(packageName, "/")
	for index := range parts {
		parts[index] = url.PathEscape(parts[index])
	}
	return strings.Join(parts, "/")
}

// Download writes a registry tarball to destination and verifies an optional
// npm SHA-512 integrity value.
func Download(client *http.Client, tarballURL, integrity string, destination io.Writer) error {
	if client == nil {
		client = http.DefaultClient
	}
	response, err := client.Get(tarballURL)
	if err != nil {
		return fmt.Errorf("downloading %s: %w", tarballURL, err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return fmt.Errorf("downloading %s: %s", tarballURL, response.Status)
	}

	writer := destination
	var expectedHash []byte
	var digest hash.Hash
	if integrity != "" {
		expectedHash, err = sha512Integrity(integrity)
		if err != nil {
			return err
		}
		digest = sha512.New()
		writer = io.MultiWriter(destination, digest)
	}
	if _, err := io.Copy(writer, response.Body); err != nil {
		return fmt.Errorf("saving %s: %w", tarballURL, err)
	}
	if digest != nil && !bytes.Equal(digest.Sum(nil), expectedHash) {
		return fmt.Errorf("integrity check failed for %s", tarballURL)
	}
	return nil
}

func sha512Integrity(integrity string) ([]byte, error) {
	for candidate := range strings.FieldsSeq(integrity) {
		encoded, ok := strings.CutPrefix(candidate, "sha512-")
		if !ok {
			continue
		}
		hash, err := base64.StdEncoding.DecodeString(encoded)
		if err == nil && len(hash) == sha512.Size {
			return hash, nil
		}
	}
	return nil, fmt.Errorf("invalid sha512 npm integrity %q", integrity)
}

// ExtractPackage safely extracts package/ entries from an npm tarball.
func ExtractPackage(tarballPath, destination string) error {
	return visitTarball(tarballPath, func(header *tar.Header, reader io.Reader) (bool, error) {
		if !strings.HasPrefix(header.Name, "package/") {
			return false, nil
		}
		relativePath := filepath.Clean(filepath.FromSlash(strings.TrimPrefix(header.Name, "package/")))
		if relativePath == "." {
			return false, nil
		}
		if !filepath.IsLocal(relativePath) {
			return false, fmt.Errorf("unsafe archive path %q", header.Name)
		}
		path := filepath.Join(destination, relativePath)
		switch header.Typeflag {
		case tar.TypeDir:
			return false, os.MkdirAll(path, 0755)
		case tar.TypeReg:
			return false, writeFile(path, reader, os.FileMode(header.Mode))
		case tar.TypeSymlink, tar.TypeLink:
			return false, fmt.Errorf("unsafe archive link %q", header.Name)
		default:
			return false, nil
		}
	})
}

// ExtractFile extracts one regular file from a tarball to destination.
func ExtractFile(tarballPath, targetPath, destination string) error {
	found := false
	err := visitTarball(tarballPath, func(header *tar.Header, reader io.Reader) (bool, error) {
		if header.Name != targetPath {
			return false, nil
		}
		if header.Typeflag != tar.TypeReg {
			return false, fmt.Errorf("archive entry %q is not a regular file", targetPath)
		}
		found = true
		return true, writeFile(destination, reader, os.FileMode(header.Mode))
	})
	if err != nil {
		return err
	}
	if !found {
		return fmt.Errorf("file %q not found in tarball", targetPath)
	}
	return nil
}

func visitTarball(tarballPath string, visit func(*tar.Header, io.Reader) (bool, error)) error {
	file, err := os.Open(tarballPath)
	if err != nil {
		return err
	}
	defer file.Close()
	gzipReader, err := gzip.NewReader(file)
	if err != nil {
		return fmt.Errorf("creating gzip reader: %w", err)
	}
	defer gzipReader.Close()
	tarReader := tar.NewReader(gzipReader)
	for {
		header, err := tarReader.Next()
		if err == io.EOF {
			return nil
		}
		if err != nil {
			return fmt.Errorf("reading tarball: %w", err)
		}
		done, err := visit(header, tarReader)
		if err != nil {
			return err
		}
		if done {
			return nil
		}
	}
}

func writeFile(path string, reader io.Reader, mode os.FileMode) error {
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
		return err
	}
	mode &= 0777
	if mode == 0 {
		mode = 0644
	}
	file, err := os.OpenFile(path, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, mode)
	if err != nil {
		return err
	}
	_, copyErr := io.Copy(file, reader)
	closeErr := file.Close()
	if copyErr != nil {
		os.Remove(path)
		return copyErr
	}
	if closeErr != nil {
		os.Remove(path)
		return closeErr
	}
	return nil
}
