"""Module extension exposing the Rust SDK's independent crate repositories."""

load("//src/sdk/rust/bazel/crates:defs.bzl", _crate_repositories = "crate_repositories")

def _crate_repositories_impl(module_ctx):
    direct_deps = _crate_repositories()
    return module_ctx.extension_metadata(
        root_module_direct_deps = [repo.repo for repo in direct_deps],
        root_module_direct_dev_deps = [],
    )

sdk = module_extension(
    implementation = _crate_repositories_impl,
)
