"Defines the Rust SDK targets shared by its independent and runtime-local graphs."

load("@rules_cc//cc:cc_library.bzl", "cc_library")
load("@rules_rust//rust:defs.bzl", "rust_doc", "rust_doc_test")
load("//bazel:rust_rules.bzl", "copilot_rust_binary_explicit", "copilot_rust_build_script", "copilot_rust_library_explicit", "copilot_rust_test")
load("//src/sdk/rust/bazel/crates:defs.bzl", "aliases", "all_crate_deps", "crate_deps")

_FEATURES = {
    "default": ["bundled-cli"],
    "bundled-cli": ["runtime", "network-forwarding"],
    "bundled-in-process": ["bundled-cli", "in-process"],
    "derive": [],
    "in-process": ["runtime"],
    "local-runtime": ["in-process"],
    "network-forwarding": [],
    "runtime": [],
    "test-support": [],
}

# Each feature also has an isolated build, so an all-features success cannot
# hide a missing cfg gate or an accidental dependency on another feature.
_PROFILES = dict({
    "default": ["default"],
    "external": [],
    "all-features": _FEATURES.keys(),
    "default-derive": ["default", "derive"],
    "local-runtime-derive": ["local-runtime", "derive"],
    "subprocess": ["runtime", "test-support", "derive", "network-forwarding"],
    "inprocess": ["local-runtime", "test-support", "derive", "network-forwarding"],
    "lean-local-runtime": ["local-runtime", "test-support", "derive"],
    "external-test": ["test-support"],
    "default-test": ["default", "test-support"],
}, **{feature: [feature] for feature in _FEATURES})

_TEST_PROFILES = [
    "default-test",
    "subprocess",
    "inprocess",
    "lean-local-runtime",
    "external-test",
]

def _features(requested):
    features = {feature: True for feature in requested}
    for _ in _FEATURES:
        for feature in features.keys():
            features.update({dependency: True for dependency in _FEATURES[feature]})
    return sorted(features.keys())

def _platform_deps(windows = [], macos = [], linux = [], other = []):
    return select({
        "@rules_rust//rust/platform:aarch64-pc-windows-msvc": windows,
        "@rules_rust//rust/platform:x86_64-pc-windows-msvc": windows,
        "@rules_rust//rust/platform:aarch64-apple-darwin": macos,
        "@rules_rust//rust/platform:x86_64-apple-darwin": macos,
        "@rules_rust//rust/platform:aarch64-unknown-linux-gnu": linux,
        "@rules_rust//rust/platform:x86_64-unknown-linux-gnu": linux,
        "//conditions:default": other,
    })

def _sdk_deps(features):
    deps = crate_deps([
        "base64",
        "bytes",
        "futures-util",
        "getrandom",
        "http",
        "indexmap",
        "parking_lot",
        "regex",
        "serde",
        "serde_json",
        "tokio",
        "tokio-stream",
        "tokio-util",
        "tracing",
        "uuid",
    ]) + _platform_deps(windows = crate_deps(["windows-sys"]))
    if "in-process" in features:
        deps += crate_deps(["libloading"])
    if "derive" in features:
        deps += crate_deps(["schemars"])
    if "bundled-cli" in features:
        deps += crate_deps(["flate2", "tar"]) + _platform_deps(windows = crate_deps(["zip"]))
    if "network-forwarding" in features:
        deps += crate_deps(["reqwest", "tokio-tungstenite"]) + _platform_deps(
            windows = crate_deps(["native-tls"]),
            linux = crate_deps(["native-tls", "openssl-sys"]),
            macos = crate_deps(["rustls-platform-verifier", "tokio-rustls"]),
        )
    return deps

def _sdk_profile(profile, features):
    name = "sdk_" + profile
    flags = [
        "--check-cfg=cfg(test,docsrs,has_bundled_cli,has_extracted_cli,sdk_compiled_protocol,copilot_cli_host)",
        "--check-cfg=cfg(feature,values(%s))" % ",".join(['"%s"' % feature for feature in _FEATURES]),
    ]
    build_deps = all_crate_deps(build = True)
    if "runtime" in features:
        build_deps += crate_deps(["ureq"]) + _platform_deps(
            linux = crate_deps(["openssl"]),
            macos = crate_deps(["rustls"]),
            other = crate_deps(["sha2"]),
        )
    copilot_rust_build_script(
        name = name + "_build_script",
        srcs = ["build.rs", "src/cache_paths.rs", "src/cache_paths/unix.rs"] + native.glob([
            "build/**/*.rs",
            "src/platform_dirs/**/*.rs",
        ]),
        aliases = aliases(build = True),
        build_script_env = {
            # Acquisition is tested separately using staged Cargo consumer
            # archives. Bazel actions must never download or mutate host caches.
            "COPILOT_SKIP_CLI_DOWNLOAD": "1",
            "OPENSSL_NO_VENDOR": "1",
            "OPENSSL_STATIC": "0",
        },
        crate_features = features,
        crate_name = (name + "_build_script").replace("-", "_"),
        crate_root = "build.rs",
        deps = build_deps,
        link_deps = _platform_deps(
            linux = crate_deps(["openssl-sys"]) if "network-forwarding" in features else [],
        ),
        tags = ["manual"],
    )
    deps = [":" + name + "_build_script"] + _sdk_deps(features)
    copilot_rust_library_explicit(
        name = name,
        srcs = native.glob(["src/**/*.rs"]),
        compile_data = ["README.md"],
        crate_features = features,
        crate_name = "github_copilot_sdk",
        crate_root = "src/lib.rs",
        edition = "2024",
        proc_macro_deps = all_crate_deps(proc_macro = True),
        # Sixteen simultaneous SDK compilations exhausted a 64 GiB runner.
        rustc_cpu = 2,
        rustc_flags = flags,
        tags = ["manual"],
        deps = deps,
    )
    rust_doc(
        name = name + "_doc",
        crate = ":" + name,
        crate_features = features,
        rustdoc_flags = ["-Dwarnings"],
        tags = ["manual"],
    )
    if profile not in _TEST_PROFILES:
        return

    fixture_env = {}
    fixtures = []
    for fixture, source in ([] if profile == "external-test" else [
        ("copilot-extension-test-fixture", "tests/fixtures/extension_fixture.rs"),
        ("copilot-host-crash-fixture", "tests/fixtures/host_crash_fixture.rs"),
    ]) + ([("copilot-connectors-runtime-fixture", "tests/fixtures/connector-runtime/src/main.rs")] if "in-process" in features else []):
        target = name + "_" + fixture
        copilot_rust_binary_explicit(
            name = target,
            srcs = native.glob(["tests/fixtures/**/*.rs"]),
            crate_features = features,
            crate_root = source,
            edition = "2024",
            proc_macro_deps = all_crate_deps(proc_macro = True, proc_macro_dev = True),
            tags = ["manual"],
            deps = [":" + name] + all_crate_deps(normal = True, normal_dev = True),
        )
        fixtures.append(":" + target)
        fixture_env["CARGO_BIN_EXE_" + fixture] = "$(execpath :" + target + ")"

    tests = []
    for source in native.glob(["tests/*.rs"]):
        test = source.removeprefix("tests/").removesuffix(".rs")

        # Cargo's external-stream CI selection intentionally only compiles this
        # integration test; the remaining suites exercise runtime management.
        if profile == "external-test" and test != "protocol_version_test":
            continue
        if test == "network_test" and "runtime" not in features:
            continue
        tests.append((test, source))
    if profile != "external-test":
        tests.append(("lib", "src/lib.rs"))
    for test, source in tests:
        needs_fixtures = test in ["e2e", "extension_launch_provider_runtime_test"]
        copilot_rust_test(
            name = name + "_" + test,
            srcs = native.glob(["src/**/*.rs", "tests/**/*.rs", "build/**/*.rs"]) + ["build.rs"],
            compile_data = ["README.md"] + native.glob(["tests/**/*.json"]),
            crate_features = features,
            crate_name = (name + "_" + test).replace("-", "_"),
            crate_root = source,
            data = fixtures if needs_fixtures else [],
            edition = "2024",
            proc_macro_deps = all_crate_deps(proc_macro = True, proc_macro_dev = True),
            rustc_env = dict({
                # The host runner executes from the checkout root. Avoid baking
                # an action sandbox or a machine-specific checkout into outputs.
                "CARGO": "cargo",
                "CARGO_MANIFEST_DIR": "src/sdk/rust",
            }, **(fixture_env if needs_fixtures else {})),
            rustc_cpu = 2 if test == "lib" else 0,
            rustc_flags = flags,
            tags = ["manual"],
            deps = ([":" + name] if test != "lib" else [":" + name + "_build_script"]) + all_crate_deps(normal = True, normal_dev = True),
        )
    examples = []
    if profile != "external-test":
        for source in native.glob(["examples/*.rs"]):
            example = source.removeprefix("examples/").removesuffix(".rs")
            target = name + "_example_" + example
            copilot_rust_binary_explicit(
                name = target,
                srcs = native.glob(["examples/**/*.rs"]),
                crate_features = features,
                crate_root = source,
                edition = "2024",
                proc_macro_deps = all_crate_deps(proc_macro = True, proc_macro_dev = True),
                tags = ["manual"],
                deps = [":" + name] + all_crate_deps(normal = True, normal_dev = True),
            )
            examples.append(":" + target)
        copilot_rust_test(
            name = name + "_example_session_fs_test",
            srcs = native.glob(["examples/session_fs*.rs", "examples/session_fs/**/*.rs"]),
            crate_features = features,
            crate_root = "examples/session_fs.rs",
            edition = "2024",
            proc_macro_deps = all_crate_deps(proc_macro = True, proc_macro_dev = True),
            tags = ["manual"],
            deps = [":" + name] + all_crate_deps(normal = True, normal_dev = True),
        )
        tests.append(("example_session_fs_test", "examples/session_fs.rs"))
        rust_doc_test(
            name = name + "_doctest",
            crate = ":" + name,
            crate_features = features,
            deps = all_crate_deps(normal_dev = True) + _platform_deps(
                linux = [":sdk_doctest_openssl"],
            ),
            proc_macro_deps = all_crate_deps(proc_macro = True, proc_macro_dev = True),
            tags = ["manual"],
        )
    native.filegroup(
        name = name + "_tests",
        testonly = True,
        # These crates are empty outside their source-level platform cfgs.
        # Keep compiling them in all_targets, without treating zero tests as a run.
        srcs = [
            ":" + name + "_" + test
            for test, _ in tests
            if test not in ["runtime_host_e2e", "extension_launch_provider_runtime_test"]
        ] + (_platform_deps(
            linux = [":" + name + "_runtime_host_e2e", ":" + name + "_extension_launch_provider_runtime_test"],
            macos = [":" + name + "_extension_launch_provider_runtime_test"],
        ) if profile != "external-test" else []),
        tags = ["manual"],
    )
    native.filegroup(
        name = name + "_all_targets",
        testonly = True,
        srcs = [":" + name, ":" + name + "_build_script_"] + fixtures + examples + [":" + name + "_" + test for test, _ in tests],
        tags = ["manual"],
    )

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
    "@vendor//uuid",
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
        "clippy.toml",
        "rust-toolchain.toml",
        "src/platform_dirs/mod.rs",
    ])

    native.filegroup(
        name = name,
        srcs = native.glob(["src/platform_dirs/**/*.rs"]),
        visibility = ["//visibility:public"],
    )

    # The src/ahp-host inventory test reads the generated wire surface.
    native.filegroup(
        name = "generated_srcs",
        srcs = native.glob(["src/generated/**/*.rs"]),
        visibility = ["//src/ahp-host:__subpackages__"],
    )

    # Cargo build-script search-path files contain execroot-relative paths,
    # which the legacy rustdoc runner cannot relocate into its runfiles tree.
    # A declared C++ linker input keeps this path in the relocatable argv.
    cc_library(
        name = "sdk_doctest_openssl",
        additional_linker_inputs = ["//bazel:system_openssl"],
        linkopts = ["-L$(execpath //bazel:system_openssl)/lib"],
        tags = ["manual"],
        target_compatible_with = ["@platforms//os:linux"],
    )

    for profile, requested in _PROFILES.items():
        _sdk_profile(profile, _features(requested))
    native.alias(
        name = "github-copilot-sdk",
        actual = ":sdk_default",
        visibility = ["//visibility:public"],
    )
    native.filegroup(
        name = "feature-builds",
        srcs = [":sdk_" + profile for profile in _PROFILES],
        tags = ["manual"],
    )

    copilot_rust_library_explicit(
        name = "github-copilot-sdk-local-runtime",
        srcs = native.glob(["src/**/*.rs"]),
        compile_data = ["README.md"],
        crate_features = [
            "in-process",
            "local-runtime",
            "runtime",
        ] + select({
            # Test-support client methods for the src/ahp-host crates' tests.
            "//bazel:dependency_test_features_enabled": ["test-support"],
            "//conditions:default": [],
        }),
        crate_name = "github_copilot_sdk",
        crate_root = "src/lib.rs",
        proc_macro_deps = ["@vendor__async-trait-0.1.92//:async_trait"],
        rustc_cpu = 2,
        rustc_flags = [
            "--cfg=copilot_cli_host",
            "--check-cfg=cfg(copilot_cli_host)",
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
        copilot_rust_test(
            name = "sdk_" + test + "_test",
            srcs = ["tests/" + test + "_test.rs"],
            crate_root = "tests/" + test + "_test.rs",
            edition = "2024",
            deps = [
                ":github-copilot-sdk-local-runtime",
                "@vendor__serde_json-1.0.154//:serde_json",
            ],
        )

    copilot_rust_test(
        name = "sdk_network_test",
        srcs = [
            "tests/network_test.rs",
            "tests/support/fake_runtime.rs",
        ],
        crate_root = "tests/network_test.rs",
        edition = "2024",
        deps = [
            ":github-copilot-sdk-local-runtime",
            "@vendor//tokio",
            "@vendor__serde_json-1.0.154//:serde_json",
        ],
    )
