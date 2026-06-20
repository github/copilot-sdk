# copilotexperimental

`copilotexperimental` is a `go vet`-compatible analyzer that reports references
to experimental Copilot SDK APIs in consumer code.

It detects exported symbols whose doc comments contain an `Experimental:`
marker, including functions, types, methods, and struct fields.

## Install

```bash
go install github.com/github/copilot-sdk/go/copilotexperimental/cmd/copilotexperimental@latest
```

## Run

```bash
go vet -vettool=$(which copilotexperimental) ./...
```

## Suppress one diagnostic

Add `//nolint:copilotexperimental` to the same line as the reference:

```go
_ = sdk.StartCanvas() //nolint:copilotexperimental
```

## golangci-lint

The analyzer can also run through golangci-lint's custom module plugin support.
Use the analyzer name `copilotexperimental`; the same
`//nolint:copilotexperimental` suppression directive applies there as well.
