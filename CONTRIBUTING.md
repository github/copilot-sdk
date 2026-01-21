## Contributing

[fork]: https://github.com/github/copilot-sdk/fork
[pr]: https://github.com/github/copilot-sdk/compare

Hi there! We're thrilled that you'd like to contribute to this project. Your help is essential for keeping it great.

Contributions to this project are [released](https://help.github.com/articles/github-terms-of-service/#6-contributions-under-repository-license) to the public under the [project's open source license](LICENSE).

Please note that this project is released with a [Contributor Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.

## Prerequisites for running and testing code

This is a multi-language SDK repository. Install the tools for the SDK(s) you plan to work on:

### All SDKs
1. Install [just](https://github.com/casey/just) command runner

### Node.js/TypeScript SDK
1. Install [Node.js](https://nodejs.org/) (v18+)
1. Install dependencies: `cd nodejs && npm ci`

### Python SDK
1. Install [Python 3.8+](https://www.python.org/downloads/)
1. Install [uv](https://github.com/astral-sh/uv)
1. Install dependencies: `cd python && uv pip install -e ".[dev]"`

### Go SDK
1. Install [Go 1.23+](https://go.dev/doc/install)
1. Install [golangci-lint](https://golangci-lint.run/welcome/install/#local-installation)
1. Install dependencies: `cd go && go mod download`

### .NET SDK
1. Install [.NET 8.0+](https://dotnet.microsoft.com/download)
1. Install dependencies: `cd dotnet && dotnet restore`

### Rust SDK
1. Install [Rust 1.75+](https://www.rust-lang.org/tools/install) (2021 edition)
1. Install dependencies: `cd rust && cargo build`

## Submitting a pull request

1. [Fork][fork] and clone the repository
1. Install dependencies for the SDK(s) you're modifying (see above)
1. Make sure the tests pass on your machine (see commands below)
1. Make sure linter passes on your machine (see commands below)
1. Create a new branch: `git checkout -b my-branch-name`
1. Make your change, add tests, and make sure the tests and linter still pass
1. Push to your fork and [submit a pull request][pr]
1. Pat yourself on the back and wait for your pull request to be reviewed and merged.

### Running tests and linters

Use `just` to run tests and linters across all SDKs or for specific languages:

```bash
# All SDKs
just test          # Run all tests
just lint          # Run all linters
just format        # Format all code

# Individual SDKs
just test-nodejs   # Node.js tests
just test-python   # Python tests
just test-go       # Go tests
just test-dotnet   # .NET tests
just test-rust     # Rust tests

just lint-nodejs   # Node.js linting
just lint-python   # Python linting
just lint-go       # Go linting
just lint-dotnet   # .NET linting
just lint-rust     # Rust linting
```

Or run commands directly in each SDK directory:

```bash
# Node.js
cd nodejs && npm test && npm run lint

# Python
cd python && uv run pytest && uv run ruff check .

# Go
cd go && go test ./... && golangci-lint run ./...

# .NET
cd dotnet && dotnet test test/GitHub.Copilot.SDK.Test.csproj

# Rust
cd rust && cargo test && cargo clippy -- -D warnings
```

Here are a few things you can do that will increase the likelihood of your pull request being accepted:

- Write tests.
- Keep your change as focused as possible. If there are multiple changes you would like to make that are not dependent upon each other, consider submitting them as separate pull requests.
- Write a [good commit message](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).

## Resources

- [How to Contribute to Open Source](https://opensource.guide/how-to-contribute/)
- [Using Pull Requests](https://help.github.com/articles/about-pull-requests/)
- [GitHub Help](https://help.github.com)
