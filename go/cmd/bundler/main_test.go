package main

import (
	"go/parser"
	"go/token"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestGenerateGoFileEmbedsRuntimeWrapperPair(t *testing.T) {
	dir := t.TempDir()
	binaryPath := filepath.Join(dir, "copilot.zst")
	runtimePath := filepath.Join(dir, "runtime.node.zst")
	wrapperPath := filepath.Join(dir, "copilot-runtime.zst")
	muslBinaryPath := filepath.Join(dir, "copilot-musl.zst")
	muslRuntimePath := filepath.Join(dir, "runtime-musl.node.zst")
	muslWrapperPath := filepath.Join(dir, "copilot-runtime-musl.zst")
	for _, path := range []string{
		binaryPath,
		licensePathForOutput(binaryPath),
		runtimePath,
		wrapperPath,
		muslBinaryPath,
		muslRuntimePath,
		muslWrapperPath,
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
		muslBinaryPath,
		hash,
		muslRuntimePath,
		hash,
		muslWrapperPath,
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
