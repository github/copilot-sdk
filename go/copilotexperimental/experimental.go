// Package copilotexperimental provides a go/analysis analyzer that reports
// references to experimental Copilot SDK APIs.
package copilotexperimental

import (
	"go/ast"
	"go/token"
	"strings"

	"golang.org/x/tools/go/analysis"
)

const (
	analyzerName         = "copilotexperimental"
	experimentalMarker   = "Experimental:"
	suppressionDirective = "nolint:copilotexperimental"
)

// Doc describes the analyzer.
const Doc = `report references to experimental Copilot SDK APIs

The analyzer marks declarations whose doc comments contain an "Experimental:"
marker and reports downstream references to those objects.

Suppress an individual diagnostic by adding //nolint:copilotexperimental to the
same line as the reference.`

type experimentalFact struct{}

func (*experimentalFact) AFact() {}

func (*experimentalFact) String() string { return "experimental" }

// Analyzer reports cross-package references to experimental Copilot SDK APIs.
var Analyzer = &analysis.Analyzer{
	Name:      analyzerName,
	Doc:       Doc,
	Run:       run,
	FactTypes: []analysis.Fact{(*experimentalFact)(nil)},
}

func run(pass *analysis.Pass) (any, error) {
	exportFacts(pass)
	reportUses(pass)
	return nil, nil
}

func exportFacts(pass *analysis.Pass) {
	mark := func(id *ast.Ident) {
		if id == nil {
			return
		}
		if obj := pass.TypesInfo.Defs[id]; obj != nil {
			pass.ExportObjectFact(obj, &experimentalFact{})
		}
	}

	for _, file := range pass.Files {
		for _, decl := range file.Decls {
			switch decl := decl.(type) {
			case *ast.FuncDecl:
				if hasExperimentalMarker(decl.Doc) {
					mark(decl.Name)
				}
			case *ast.GenDecl:
				groupExperimental := len(decl.Specs) == 1 && hasExperimentalMarker(decl.Doc)
				for _, spec := range decl.Specs {
					switch spec := spec.(type) {
					case *ast.TypeSpec:
						if groupExperimental || hasExperimentalMarker(spec.Doc) {
							mark(spec.Name)
						}
						markStructFields(pass, spec)
					case *ast.ValueSpec:
						if groupExperimental || hasExperimentalMarker(spec.Doc) {
							for _, name := range spec.Names {
								mark(name)
							}
						}
					}
				}
			}
		}
	}
}

func markStructFields(pass *analysis.Pass, spec *ast.TypeSpec) {
	structType, ok := spec.Type.(*ast.StructType)
	if !ok || structType.Fields == nil {
		return
	}

	for _, field := range structType.Fields.List {
		if !hasExperimentalMarker(field.Doc, field.Comment) {
			continue
		}
		for _, name := range field.Names {
			if obj := pass.TypesInfo.Defs[name]; obj != nil {
				pass.ExportObjectFact(obj, &experimentalFact{})
			}
		}
	}
}

func reportUses(pass *analysis.Pass) {
	for _, file := range pass.Files {
		suppressions := collectSuppressions(pass, file)

		ast.Inspect(file, func(node ast.Node) bool {
			id, ok := node.(*ast.Ident)
			if !ok || suppressions.contains(pass, id.Pos()) {
				return true
			}

			obj := pass.TypesInfo.Uses[id]
			if obj == nil || obj.Pkg() == nil || obj.Pkg() == pass.Pkg {
				return true
			}

			var fact experimentalFact
			if !pass.ImportObjectFact(obj, &fact) {
				return true
			}

			pass.Reportf(
				id.Pos(),
				"use of experimental API '%s' — opt in with //%s",
				obj.Name(),
				suppressionDirective,
			)
			return true
		})
	}
}

func hasExperimentalMarker(groups ...*ast.CommentGroup) bool {
	for _, group := range groups {
		if group == nil {
			continue
		}
		for _, line := range strings.Split(group.Text(), "\n") {
			if strings.HasPrefix(strings.TrimSpace(line), experimentalMarker) {
				return true
			}
		}
	}
	return false
}

type suppressionIndex map[int]struct{}

func collectSuppressions(pass *analysis.Pass, file *ast.File) suppressionIndex {
	lines := make(suppressionIndex)
	for _, group := range file.Comments {
		for _, comment := range group.List {
			if hasSuppressionDirective(comment.Text) {
				line := pass.Fset.PositionFor(comment.Slash, false).Line
				lines[line] = struct{}{}
			}
		}
	}
	return lines
}

func (index suppressionIndex) contains(pass *analysis.Pass, pos token.Pos) bool {
	line := pass.Fset.PositionFor(pos, false).Line
	_, ok := index[line]
	return ok
}

func hasSuppressionDirective(text string) bool {
	text = normalizeCommentText(text)
	if !strings.HasPrefix(text, "nolint:") {
		return false
	}

	directives := strings.TrimSpace(strings.TrimPrefix(text, "nolint:"))
	if directives == "" {
		return false
	}

	field := strings.Fields(directives)[0]
	for _, directive := range strings.Split(field, ",") {
		if strings.TrimSpace(directive) == analyzerName {
			return true
		}
	}
	return false
}

func normalizeCommentText(text string) string {
	text = strings.TrimSpace(text)
	text = strings.TrimPrefix(text, "//")
	text = strings.TrimPrefix(text, "/*")
	text = strings.TrimSuffix(text, "*/")
	return strings.TrimSpace(text)
}
