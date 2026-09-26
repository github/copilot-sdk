"Defines the Rust SDK targets shared by its independent and runtime-local graphs."

load("@rules_rust//cargo:defs.bzl", "cargo_build_script")
load("@rules_rust//rust:defs.bzl", "rust_library")
load("//src/sdk/rust/bazel/crates:defs.bzl", "aliases", "all_crate_deps", "crate_deps")

_LOCAL_RUNTIME_DEPS = [
    "@vendor__base64-0.23.1//:base64",
    "@vendor__bytes-1.12.1//:bytes",
    "@vendor__dirs-5.0.1//:dirs",
    "@vendor__futures-util-0.3.34//:futures_util",
    "@vendor__getrandom-0.4.3//:getrandom",
    "@vendor__http-1.5.0//:http",
    "@vendor__indexmap-2.14.2//:indexmap",
    "@vendor__libloading-0.9.0//:libloading",
    "@vendor__parking_lot-0.12.5//:parking_lot",
    "@vendor__regex-1.13.1//:regex",
    "@vendor__reqwest-0.13.5//:reqwest",
    "@vendor__serde-1.0.229//:serde",
    "@vendor__serde_json-1.0.151//:serde_json",
    "@vendor__tokio-1.53.1//:tokio",
    "@vendor__tokio-stream-0.1.19//:tokio_stream",
    "@vendor__tokio-tungstenite-0.28.0//:tokio_tungstenite",
    "@vendor__tokio-util-0.7.19//:tokio_util",
    "@vendor__tracing-0.1.44//:tracing",
    "@vendor__uuid-1.26.1//:uuid",
] + select({
    "@rules_rust//rust/platform:aarch64-pc-windows-msvc": [
        "@vendor__windows-sys-0.61.2//:windows_sys",
    ],
    "@rules_rust//rust/platform:x86_64-pc-windows-msvc": [
        "@vendor__windows-sys-0.61.2//:windows_sys",
    ],
    "//conditions:default": [],
})

def sdk_targets():
    """Declares the standalone SDK and CLI-specific local-runtime libraries."""

    native.exports_files([
        ".rustfmt.toml",
        "Cargo.lock",
        "Cargo.toml",
        "rust-toolchain.toml",
    ])

    sdk_features = [
        "bundled-cli",
        "bundled-in-process",
        "derive",
        "in-process",
        "local-runtime",
        "test-support",
    ]

    cargo_build_script(
        name = "build_script",
        srcs = [
            "build.rs",
            "src/cache_paths.rs",
        ] + native.glob(["build/**/*.rs"]),
        aliases = aliases(build = True),
        build_script_env = {
            "COPILOT_SKIP_CLI_DOWNLOAD": "1",
        },
        crate_features = sdk_features,
        crate_name = "build_script_build",
        crate_root = "build.rs",
        # crate_universe classifies ureq as dev-only when it is also a build dependency.
        deps = all_crate_deps(build = True) + crate_deps(["ureq"]),
    )

    rust_library(
        name = "github-copilot-sdk",
        srcs = native.glob(["src/**/*.rs"]),
        aliases = {
            "@sdk_vendor//libloading-0.9.0": "libloading",
        },
        compile_data = ["README.md"],
        crate_features = sdk_features,
        crate_name = "github_copilot_sdk",
        crate_root = "src/lib.rs",
        proc_macro_deps = all_crate_deps(proc_macro = True),
        visibility = ["//visibility:public"],
        deps = [
            ":build_script",
            "//src/sdk/rust/bazel/crates:schemars-1.2.2",
        ] + all_crate_deps(),
    )

    rust_library(
        name = "github-copilot-sdk-local-runtime",
        srcs = native.glob(["src/**/*.rs"]),
        compile_data = ["README.md"],
        crate_features = [
            "in-process",
            "local-runtime",
        ],
        crate_name = "github_copilot_sdk",
        crate_root = "src/lib.rs",
        proc_macro_deps = ["@vendor__async-trait-0.1.92//:async_trait"],
        rustc_flags = [
            "--check-cfg=cfg(docsrs)",
            "--check-cfg=cfg(has_bundled_cli)",
            "--check-cfg=cfg(has_extracted_cli)",
            "--check-cfg=cfg(test)",
            '--check-cfg=cfg(feature,values("bundled-cli","bundled-in-process","derive","in-process","local-runtime","test-support"))',
        ],
        visibility = ["//visibility:public"],
        deps = _LOCAL_RUNTIME_DEPS,
    )
