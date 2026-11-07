package embeddedcli

import (
	"os"
	"testing"
)

func TestTemporaryCachePermissions(t *testing.T) {
	const uid = uint32(1001)
	for _, test := range []struct {
		name    string
		owner   uint32
		mode    os.FileMode
		private bool
	}{
		{"private directory", uid, os.ModeDir | 0700, true},
		{"readable cache", uid, os.ModeDir | 0755, false},
		{"writable cache", uid, os.ModeDir | 0770, false},
		{"foreign owner", uid + 1, os.ModeDir | 0700, false},
		{"file", uid, 0700, false},
		{"symlink", uid, os.ModeSymlink | 0777, false},
	} {
		t.Run(test.name, func(t *testing.T) {
			if got := privateTemporaryCache(test.owner, test.mode, uid); got != test.private {
				t.Fatalf("private cache trust = %t, want %t", got, test.private)
			}
		})
	}
}
