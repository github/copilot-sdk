package main

import (
	"fmt"

	copilot "github.com/github/copilot-sdk/go"
)

var positional = copilot.PermissionRequestRead{"Read a file", nil, "/workspace/file.txt", nil, nil, nil, nil, nil}

func main() {
	copy := positional
	equal := positional == copy
	requests := map[copilot.PermissionRequestRead]bool{positional: true}
	if !equal || !requests[copy] || positional.Intention != "Read a file" || positional.Path != "/workspace/file.txt" {
		panic("read request value changed")
	}
	fmt.Println("External positional literals, equality, and map keys work.")
}
