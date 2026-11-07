package embeddedcli

import "os"

func privateTemporaryCache(owner uint32, mode os.FileMode, uid uint32) bool {
	return mode.IsDir() && owner == uid && mode.Perm()&0077 == 0
}
