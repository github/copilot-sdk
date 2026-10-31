"Defines the Rust SDK targets shared by its independent and runtime-local graphs."

load("@rules_rust//cargo:defs.bzl", "cargo_build_script")
load("@rules_rust//rust:defs.bzl", "rust_library", "rust_test")
load("//src/sdk/rust/bazel/crates:defs.bzl", "aliases", "all_crate_deps", "crate_deps")

_LOCAL_RUNTIME_DEPS = [
    "@vendor__base64-0.23.1//:base64",
    "@vendor__bytes-1.12.1//:bytes",
    "@vendor__futures-util-0.3.34//:futures_util",
    "@vendor__getrandom-0.4.3//:getrandom",
    "@vendor__http-1.5.0//:http",
    "@vendor__indexmap-2.14.2//:indexmap",
    "@vendor__libloading-0.9.0//:libloading",
    "@vendor__parking_lot-0.12.5//:parking_lot",
    "@vendor__regex-1.13.1//:regex",
    "@vendor__serde-1.0.229//:serde",
    "@vendor__serde_json-1.0.154//:serde_json",
    "@vendor//tokio",
    "@vendor__tokio-stream-0.1.19//:tokio_stream",
    "@vendor__tokio-util-0.7.20//:tokio_util",
    "@vendor__tracing-0.1.44//:tracing",
    "@vendor__uuid-1.29.0//:uuid",
] + select({
    "@rules_rust//rust/platform:aarch64-pc-windows-msvc": [
        "@vendor__windows-sys-0.61.2//:windows_sys",
    ],
    "@rules_rust//rust/platform:x86_64-pc-windows-msvc": [
        "@vendor__windows-sys-0.61.2//:windows_sys",
    ],
    "//conditions:default": [],
})

def sdk_targets(name = "platform_dirs_srcs"):
    """Declares the standalone SDK and CLI-specific local-runtime libraries.

    Args:
        name: Name of the shared platform-directory source filegroup.
    """

    native.exports_files([
        ".rustfmt.toml",
        "Cargo.lock",
        "Cargo.toml",
        "rust-toolchain.toml",
        "src/platform_dirs/mod.rs",
    ])

    native.filegroup(
        name = name,
        srcs = native.glob(["src/platform_dirs/**/*.rs"]),
        visibility = ["//visibility:public"],
    )

    sdk_features = [
        "bundled-cli",
        "bundled-in-process",
        "derive",
        "in-process",
        "local-runtime",
        "network-forwarding",
        "runtime",
        "test-support",
    ]

    cargo_build_script(
        name = "build_script",
        srcs = [
            "build.rs",
            "src/cache_paths.rs",
        ] + native.glob(["build/**/*.rs", "src/platform_dirs/**/*.rs"]),
        aliases = aliases(build = True),
        build_script_env = {
            "COPILOT_SKIP_CLI_DOWNLOAD": "1",
            "OPENSSL_NO_VENDOR": "1",
            "OPENSSL_STATIC": "0",
        },
        crate_features = sdk_features,
        crate_name = "build_script_build",
        crate_root = "build.rs",
        # crate_universe can classify shared build/dev dependencies as dev-only.
        # These selects are resolved on cargo_build_script's execution platform.
        deps = all_crate_deps(build = True) + crate_deps(["ureq"]) + select({
            "@rules_rust//rust/platform:aarch64-pc-windows-msvc": [],
            "@rules_rust//rust/platform:x86_64-pc-windows-msvc": [],
            "@rules_rust//rust/platform:aarch64-apple-darwin": crate_deps(["rustls"]),
            "@rules_rust//rust/platform:x86_64-apple-darwin": crate_deps(["rustls"]),
            "@rules_rust//rust/platform:aarch64-unknown-linux-gnu": crate_deps(["openssl"]),
            "@rules_rust//rust/platform:x86_64-unknown-linux-gnu": crate_deps(["openssl"]),
            "//conditions:default": crate_deps(["sha2"]),
        }),
        link_deps = select({
            "@rules_rust//rust/platform:aarch64-unknown-linux-gnu": ["@sdk_vendor__openssl-sys-0.9.117//:openssl_sys"],
            "@rules_rust//rust/platform:x86_64-unknown-linux-gnu": ["@sdk_vendor__openssl-sys-0.9.117//:openssl_sys"],
            "//conditions:default": [],
        }),
    )

    rust_library(
        name = "github-copilot-sdk",
        srcs = native.glob(["src/**/*.rs"]),
        compile_data = ["README.md"],
        crate_features = sdk_features,
        crate_name = "github_copilot_sdk",
        crate_root = "src/lib.rs",
        proc_macro_deps = all_crate_deps(proc_macro = True),
        visibility = ["//visibility:public"],
        deps = [":build_script"] + all_crate_deps() + crate_deps(["libloading", "schemars"]),
    )

    rust_library(
        name = "github-copilot-sdk-local-runtime",
        srcs = native.glob(["src/**/*.rs"]),
        compile_data = ["README.md"],
        crate_features = [
            "in-process",
            "local-runtime",
            "runtime",
        ],
        crate_name = "github_copilot_sdk",
        crate_root = "src/lib.rs",
        proc_macro_deps = ["@vendor__async-trait-0.1.92//:async_trait"],
        rustc_flags = [
            "--check-cfg=cfg(docsrs)",
            "--check-cfg=cfg(has_bundled_cli)",
            "--check-cfg=cfg(has_extracted_cli)",
            "--check-cfg=cfg(test)",
            '--check-cfg=cfg(feature,values("bundled-cli","bundled-in-process","derive","in-process","local-runtime","network-forwarding","runtime","test-support"))',
        ],
        visibility = ["//visibility:public"],
        deps = _LOCAL_RUNTIME_DEPS,
    )

    for test in ["api_types", "session_events", "legacy_options"]:
        rust_test(
            name = "sdk_" + test + "_test",
            srcs = ["tests/" + test + "_test.rs"],
            crate_root = "tests/" + test + "_test.rs",
            edition = "2024",
            deps = [
                ":github-copilot-sdk-local-runtime",
                "@vendor__serde_json-1.0.154//:serde_json",
            ],
        )

    rust_test(
        name = "sdk_kerberos_auth_test",
        srcs = ["tests/kerberos_auth_test.rs"],
        crate_root = "tests/kerberos_auth_test.rs",
        edition = "2024",
        deps = [
            ":github-copilot-sdk-local-runtime",
            "@vendor//tokio",
            "@vendor__serde_json-1.0.154//:serde_json",
        ],
    )
