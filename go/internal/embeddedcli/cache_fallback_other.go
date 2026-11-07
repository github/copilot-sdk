//go:build !unix

package embeddedcli

import "os"

func fallbackCacheDir() (string, error) {
	return os.TempDir(), nil
}
