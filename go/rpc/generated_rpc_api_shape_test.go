package rpc

import (
	"bytes"
	"go/ast"
	"go/format"
	"go/parser"
	"go/token"
	"path/filepath"
	"runtime"
	"testing"
)

var (
	_ ExternalToolResult      = ExternalToolStringResult("")
	_ ExternalToolResult      = (*ExternalToolTextResultForLlm)(nil)
	_ FilterMapping           = FilterMappingEnumMap{}
	_ FilterMapping           = FilterMappingStringMarkdown
	_ McpServerConfig         = (*McpServerConfigHTTP)(nil)
	_ McpServerConfig         = (*McpServerConfigLocal)(nil)
	_ UIElicitationFieldValue = UIElicitationStringValue("")
	_ UIElicitationFieldValue = UIElicitationStringArrayValue(nil)
	_ UIElicitationFieldValue = UIElicitationBooleanValue(false)
	_ UIElicitationFieldValue = UIElicitationNumberValue(0)
)

func TestGeneratedRPCAPIShape(t *testing.T) {
	file, fileSet := parseGeneratedRPC(t)

	assertInterfaceType(t, file, "ExternalToolResult")
	assertTypeExpr(t, fileSet, findTypeSpec(t, file, "ExternalToolStringResult").Type, "string")
	assertStructFieldType(t, file, fileSet, "HandlePendingToolCallRequest", "Result", "ExternalToolResult")

	assertInterfaceType(t, file, "FilterMapping")
	assertTypeExpr(t, fileSet, findTypeSpec(t, file, "FilterMappingEnumMap").Type, "map[string]FilterMappingValue")

	assertInterfaceType(t, file, "McpServerConfig")
	assertStructFieldType(t, file, fileSet, "McpConfigAddRequest", "Config", "McpServerConfig")
	assertStructFieldType(t, file, fileSet, "McpConfigList", "Servers", "map[string]McpServerConfig")
	assertStructFieldType(t, file, fileSet, "McpConfigUpdateRequest", "Config", "McpServerConfig")
	assertStructFieldType(t, file, fileSet, "McpServerConfigHTTP", "FilterMapping", "FilterMapping")
	assertStructFieldType(t, file, fileSet, "McpServerConfigLocal", "FilterMapping", "FilterMapping")

	assertInterfaceType(t, file, "UIElicitationFieldValue")
	assertTypeExpr(t, fileSet, findTypeSpec(t, file, "UIElicitationStringArrayValue").Type, "[]string")
	assertStructFieldType(t, file, fileSet, "UIElicitationResponse", "Content", "map[string]UIElicitationFieldValue")
}

func parseGeneratedRPC(t *testing.T) (*ast.File, *token.FileSet) {
	t.Helper()
	_, currentFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("locate test file")
	}
	fileSet := token.NewFileSet()
	file, err := parser.ParseFile(fileSet, filepath.Join(filepath.Dir(currentFile), "zrpc.go"), nil, 0)
	if err != nil {
		t.Fatalf("parse zrpc.go: %v", err)
	}
	return file, fileSet
}

func findTypeSpec(t *testing.T, file *ast.File, typeName string) *ast.TypeSpec {
	t.Helper()
	for _, decl := range file.Decls {
		genDecl, ok := decl.(*ast.GenDecl)
		if !ok || genDecl.Tok != token.TYPE {
			continue
		}
		for _, spec := range genDecl.Specs {
			typeSpec, ok := spec.(*ast.TypeSpec)
			if ok && typeSpec.Name.Name == typeName {
				return typeSpec
			}
		}
	}
	t.Fatalf("type %s not found", typeName)
	return nil
}

func assertInterfaceType(t *testing.T, file *ast.File, typeName string) {
	t.Helper()
	if _, ok := findTypeSpec(t, file, typeName).Type.(*ast.InterfaceType); !ok {
		t.Fatalf("type %s has unexpected AST node %T", typeName, findTypeSpec(t, file, typeName).Type)
	}
}

func assertStructFieldType(t *testing.T, file *ast.File, fileSet *token.FileSet, structName, fieldName, want string) {
	t.Helper()
	structType, ok := findTypeSpec(t, file, structName).Type.(*ast.StructType)
	if !ok {
		t.Fatalf("type %s is %T, want struct", structName, findTypeSpec(t, file, structName).Type)
	}
	for _, field := range structType.Fields.List {
		for _, name := range field.Names {
			if name.Name == fieldName {
				assertTypeExpr(t, fileSet, field.Type, want)
				return
			}
		}
	}
	t.Fatalf("field %s.%s not found", structName, fieldName)
}

func assertTypeExpr(t *testing.T, fileSet *token.FileSet, expr ast.Expr, want string) {
	t.Helper()
	var buffer bytes.Buffer
	if err := format.Node(&buffer, fileSet, expr); err != nil {
		t.Fatalf("format type expression: %v", err)
	}
	if got := buffer.String(); got != want {
		t.Fatalf("type expression = %s, want %s", got, want)
	}
}
