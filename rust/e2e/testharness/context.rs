//! Test context for E2E tests.

use copilot_sdk::{ClientOptions, CopilotClient};
use std::env;
use std::path::PathBuf;
use std::sync::OnceLock;
use tempfile::TempDir;

static CLI_PATH: OnceLock<Option<String>> = OnceLock::new();

/// Get the path to the Copilot CLI.
///
/// Checks the `COPILOT_CLI_PATH` environment variable first, then looks for the CLI
/// in the sibling nodejs directory's node_modules.
pub fn cli_path() -> Option<String> {
    CLI_PATH
        .get_or_init(|| {
            // Check environment variable first
            if let Ok(path) = env::var("COPILOT_CLI_PATH") {
                if !path.is_empty() {
                    return Some(path);
                }
            }

            // Look for CLI in sibling nodejs directory's node_modules
            let mut path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
            path.push("../nodejs/node_modules/@github/copilot/index.js");

            if let Ok(abs_path) = path.canonicalize() {
                if abs_path.exists() {
                    return abs_path.to_str().map(|s| s.to_string());
                }
            }

            None
        })
        .clone()
}

/// Test context for E2E tests.
///
/// Provides isolated directories and configuration for testing.
pub struct TestContext {
    /// Path to the Copilot CLI.
    pub cli_path: String,
    /// Temporary home directory.
    pub home_dir: TempDir,
    /// Temporary work directory.
    pub work_dir: TempDir,
}

impl TestContext {
    /// Create a new test context.
    ///
    /// # Panics
    ///
    /// Panics if the CLI is not found.
    pub fn new() -> Self {
        let cli = cli_path().expect(
            "CLI not found. Run 'npm install' in the nodejs directory first, or set COPILOT_CLI_PATH.",
        );

        let home_dir = TempDir::new().expect("Failed to create temp home dir");
        let work_dir = TempDir::new().expect("Failed to create temp work dir");

        Self {
            cli_path: cli,
            home_dir,
            work_dir,
        }
    }

    /// Get environment variables configured for isolated testing.
    pub fn env(&self) -> Vec<(String, String)> {
        let mut env = Vec::new();

        // Copy current environment
        for (key, value) in std::env::vars() {
            env.push((key, value));
        }

        // Add overrides for isolated testing
        env.push((
            "XDG_CONFIG_HOME".to_string(),
            self.home_dir.path().to_str().unwrap().to_string(),
        ));
        env.push((
            "XDG_STATE_HOME".to_string(),
            self.home_dir.path().to_str().unwrap().to_string(),
        ));

        env
    }

    /// Create a CopilotClient configured for this test context.
    pub fn new_client(&self) -> CopilotClient {
        CopilotClient::new(Some(ClientOptions {
            cli_path: Some(self.cli_path.clone()),
            cwd: Some(self.work_dir.path().to_str().unwrap().to_string()),
            env: Some(self.env()),
            ..Default::default()
        }))
    }
}

impl Default for TestContext {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_cli_path_returns_some_or_none() {
        // This test just ensures the function doesn't panic
        let _ = cli_path();
    }
}
