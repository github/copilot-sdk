#[cfg(feature = "runtime")]
#[path = "build/in_process.rs"]
mod implementation;

#[cfg(not(feature = "runtime"))]
#[path = "build/openssl_linkage.rs"]
mod openssl_linkage;

#[cfg(feature = "runtime")]
#[allow(dead_code)]
#[path = "src/platform_dirs/mod.rs"]
mod platform_dirs;

fn main() {
    println!("cargo::rustc-check-cfg=cfg(copilot_cli_host)");
    println!("cargo::rerun-if-env-changed=COPILOT_REPOSITORY_CLI_HOST");
    if std::env::var("COPILOT_REPOSITORY_CLI_HOST").as_deref() == Ok("1") {
        println!("cargo::rustc-cfg=copilot_cli_host");
    }
    #[cfg(feature = "runtime")]
    implementation::main();
    #[cfg(not(feature = "runtime"))]
    {
        openssl_linkage::require_target_provider(false);
        println!("cargo::rustc-check-cfg=cfg(has_bundled_cli)");
        println!("cargo::rustc-check-cfg=cfg(has_extracted_cli)");
        println!("cargo::rustc-check-cfg=cfg(sdk_compiled_protocol)");
    }
}
