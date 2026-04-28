use std::collections::HashSet;
use std::env;
use std::ffi::OsStr;
use std::path::{Path, PathBuf};

use serde::Serialize;
use tracing::warn;

use crate::Error;

/// How the copilot binary was resolved.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize)]
#[serde(rename_all = "snake_case")]
pub enum BinarySource {
    /// Extracted from the build-time embedded binary.
    Bundled,
    /// Set via `COPILOT_CLI_PATH` environment variable.
    EnvOverride,
    /// Found on PATH or standard search locations.
    Local,
}

/// Find the `copilot` CLI binary on the system.
///
/// Checks `COPILOT_CLI_PATH` env var first, then searches PATH and common
/// install locations (homebrew, nvm, nodenv, fnm, volta, cargo, etc.).
/// Use `COPILOT_CLI_NAME` to override the binary name (default: `copilot`).
pub fn copilot_binary() -> Result<PathBuf, Error> {
    copilot_binary_with_source().map(|(path, _)| path)
}

/// Like [`copilot_binary`] but also reports how the binary was resolved.
pub fn copilot_binary_with_source() -> Result<(PathBuf, BinarySource), Error> {
    if let Ok(value) = env::var("COPILOT_CLI_PATH") {
        let candidate = PathBuf::from(value);
        if candidate.is_file() {
            return Ok((candidate, BinarySource::EnvOverride));
        }
        if candidate.is_dir()
            && let Some(found) = find_copilot_in_dir(&candidate)
        {
            return Ok((found, BinarySource::EnvOverride));
        }
        warn!(path = %candidate.display(), "COPILOT_CLI_PATH set but not usable");
    }

    if let Some(path) = crate::embeddedcli::path() {
        return Ok((path, BinarySource::Bundled));
    }

    for dir in standard_search_paths() {
        if let Some(found) = find_copilot_in_dir(&dir) {
            return Ok((found, BinarySource::Local));
        }
    }

    Err(Error::BinaryNotFound {
        name: "copilot",
        hint: "ensure the Copilot CLI is installed and on PATH, or set COPILOT_CLI_PATH. use COPILOT_CLI_NAME to override the binary name (default: copilot)",
    })
}

/// Find the `copilot` CLI binary using only the current PATH entries.
///
/// This is intentionally narrower than [`copilot_binary`]: it does not honor
/// override env vars and does not search inferred install locations.
pub fn copilot_binary_on_path() -> Result<PathBuf, Error> {
    if let Some(found) = find_executable_in_path(
        env::var_os("PATH").as_deref(),
        &literal_copilot_executable_names(),
    ) {
        return Ok(found);
    }

    Err(Error::BinaryNotFound {
        name: "copilot",
        hint: "ensure the `copilot` command is installed and available on PATH",
    })
}

/// Build an extended `PATH` by prepending `extra` dirs to the standard
/// search paths (current PATH + common install locations).
pub fn extended_path(extra: &[PathBuf]) -> Option<std::ffi::OsString> {
    let mut paths = SearchPaths::new();
    for p in extra {
        paths.push(p.clone());
    }
    paths.append_standard();
    if paths.is_empty() {
        return None;
    }
    env::join_paths(paths).ok()
}

fn copilot_executable_names() -> Vec<String> {
    let base = env::var("COPILOT_CLI_NAME").unwrap_or_else(|_| "copilot".to_string());
    executable_names_for_base(&base)
}

fn literal_copilot_executable_names() -> Vec<String> {
    executable_names_for_base("copilot")
}

fn executable_names_for_base(base: &str) -> Vec<String> {
    #[cfg(target_os = "windows")]
    {
        vec![
            format!("{}.exe", base),
            format!("{}.cmd", base),
            format!("{}.bat", base),
        ]
    }
    #[cfg(not(target_os = "windows"))]
    {
        vec![base.to_string()]
    }
}

fn find_executable(dir: &Path, names: &[impl AsRef<std::ffi::OsStr>]) -> Option<PathBuf> {
    if dir.as_os_str().is_empty() {
        return None;
    }
    names
        .iter()
        .map(|n| dir.join(n.as_ref()))
        .find(|c| c.is_file())
}

fn find_copilot_in_dir(dir: &Path) -> Option<PathBuf> {
    find_executable(dir, &copilot_executable_names())
}

fn find_executable_in_path(
    path_env: Option<&OsStr>,
    names: &[impl AsRef<std::ffi::OsStr>],
) -> Option<PathBuf> {
    let path_env = path_env?;
    for dir in env::split_paths(path_env) {
        if let Some(found) = find_executable(&dir, names) {
            return Some(found);
        }
    }
    None
}

/// Ordered, deduplicated collection of directory paths to search for binaries.
///
/// Paths are stored in insertion order. Duplicates and empty paths are
/// silently dropped on `push`. Implements `Iterator` so it can be passed
/// directly to `env::join_paths` or used in a `for` loop.
struct SearchPaths {
    seen: HashSet<PathBuf>,
    paths: Vec<PathBuf>,
}

impl SearchPaths {
    fn new() -> Self {
        Self {
            seen: HashSet::new(),
            paths: Vec::new(),
        }
    }

    /// Add a path if it hasn't been seen before. Empty paths are ignored.
    fn push(&mut self, path: PathBuf) {
        if !path.as_os_str().is_empty() && self.seen.insert(path.clone()) {
            self.paths.push(path);
        }
    }

    fn is_empty(&self) -> bool {
        self.paths.is_empty()
    }

    /// Append the standard search paths: current PATH, home-relative dirs,
    /// version manager paths (nvm, nodenv, fnm), and platform-specific dirs.
    fn append_standard(&mut self) {
        if let Some(existing) = env::var_os("PATH") {
            for p in env::split_paths(&existing) {
                self.push(p);
            }
        }

        if let Some(home) = dirs::home_dir() {
            self.push(home.join(".local/bin"));
            self.push(home.join(".cargo/bin"));
            self.push(home.join(".bun/bin"));
            self.push(home.join(".npm-global/bin"));
            self.push(home.join(".yarn/bin"));
            self.push(home.join(".volta/bin"));
            self.push(home.join(".asdf/shims"));
            self.push(home.join("bin"));
        }

        // Platform-specific standard dirs come before version-manager paths
        // so that the system-installed node (e.g. /opt/homebrew/bin/node)
        // takes precedence over arbitrary old versions found under
        // ~/.nvm/versions, ~/.nodenv/versions, etc.
        #[cfg(target_os = "macos")]
        {
            self.push(PathBuf::from("/opt/homebrew/bin"));
            self.push(PathBuf::from("/usr/local/bin"));
            self.push(PathBuf::from("/usr/bin"));
            self.push(PathBuf::from("/bin"));
            self.push(PathBuf::from("/usr/sbin"));
            self.push(PathBuf::from("/sbin"));
        }

        #[cfg(target_os = "linux")]
        {
            self.push(PathBuf::from("/usr/local/bin"));
            self.push(PathBuf::from("/usr/bin"));
            self.push(PathBuf::from("/bin"));
            self.push(PathBuf::from("/snap/bin"));
        }

        #[cfg(target_os = "windows")]
        {
            if let Some(appdata) = env::var_os("APPDATA") {
                self.push(PathBuf::from(appdata).join("npm"));
            }
            if let Some(local) = env::var_os("LOCALAPPDATA") {
                let local = PathBuf::from(local);
                self.push(local.join("Programs"));
                // User-scope winget install of Git for Windows.
                self.push(local.join("Programs").join("Git").join("cmd"));
                self.push(local.join("Programs").join("Git").join("bin"));
            }
            // Git for Windows standard machine-scope install locations.
            for env_var in ["ProgramFiles", "ProgramW6432", "ProgramFiles(x86)"] {
                if let Some(program_files) = env::var_os(env_var) {
                    let program_files = PathBuf::from(program_files);
                    self.push(program_files.join("Git").join("cmd"));
                    self.push(program_files.join("Git").join("bin"));
                }
            }
        }

        // Version manager paths are a fallback for binary discovery —
        // they enumerate every installed version, so an arbitrary old
        // node/copilot can appear first if filesystem ordering is unlucky.
        for p in collect_nvm_paths() {
            self.push(p);
        }
        for p in collect_nodenv_paths() {
            self.push(p);
        }
        for p in collect_fnm_paths() {
            self.push(p);
        }
    }
}

impl IntoIterator for SearchPaths {
    type IntoIter = std::vec::IntoIter<PathBuf>;
    type Item = PathBuf;

    fn into_iter(self) -> Self::IntoIter {
        self.paths.into_iter()
    }
}

/// Collect standard search paths for binary resolution.
fn standard_search_paths() -> SearchPaths {
    let mut paths = SearchPaths::new();
    paths.append_standard();
    paths
}

fn collect_nvm_paths() -> Vec<PathBuf> {
    let mut paths = Vec::new();
    let nvm_dir = env::var_os("NVM_DIR")
        .map(PathBuf::from)
        .or_else(|| dirs::home_dir().map(|home| home.join(".nvm")));
    let Some(nvm_dir) = nvm_dir else {
        return paths;
    };
    let versions_dir = nvm_dir.join("versions").join("node");
    let entries = match std::fs::read_dir(&versions_dir) {
        Ok(entries) => entries,
        Err(_) => return paths,
    };
    for entry in entries.flatten() {
        let path = entry.path();
        if path.is_dir() {
            paths.push(path.join("bin"));
        }
    }
    paths
}

fn collect_nodenv_paths() -> Vec<PathBuf> {
    let mut paths = Vec::new();
    let root = env::var_os("NODENV_ROOT")
        .map(PathBuf::from)
        .or_else(|| dirs::home_dir().map(|home| home.join(".nodenv")));
    let Some(root) = root else {
        return paths;
    };
    let versions_dir = root.join("versions");
    let entries = match std::fs::read_dir(&versions_dir) {
        Ok(entries) => entries,
        Err(_) => return paths,
    };
    for entry in entries.flatten() {
        let path = entry.path();
        if path.is_dir() {
            paths.push(path.join("bin"));
        }
    }
    paths
}

fn fnm_root_candidates_from(
    fnm_dir: Option<PathBuf>,
    xdg_data_home: Option<PathBuf>,
    home: Option<PathBuf>,
) -> Vec<PathBuf> {
    let mut roots = SearchPaths::new();

    if let Some(fnm_dir) = fnm_dir.filter(|path| !path.as_os_str().is_empty()) {
        roots.push(fnm_dir);
    }

    if let Some(xdg_data_home) = xdg_data_home.filter(|path| !path.as_os_str().is_empty()) {
        roots.push(xdg_data_home.join("fnm"));
    }

    if let Some(home) = home {
        roots.push(home.join(".local").join("share").join("fnm"));
        roots.push(home.join(".fnm"));
    }

    roots.paths
}

fn collect_fnm_paths() -> Vec<PathBuf> {
    let roots = fnm_root_candidates_from(
        env::var_os("FNM_DIR").map(PathBuf::from),
        env::var_os("XDG_DATA_HOME").map(PathBuf::from),
        dirs::home_dir(),
    );

    let mut paths = SearchPaths::new();
    for root in &roots {
        paths.push(root.join("aliases").join("default").join("bin"));

        let versions_dir = root.join("node-versions");
        let entries = match std::fs::read_dir(&versions_dir) {
            Ok(entries) => entries,
            Err(_) => continue,
        };
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_dir() {
                paths.push(path.join("installation").join("bin"));
            }
        }
    }

    paths.paths
}

#[cfg(test)]
mod tests {
    use std::path::{Path, PathBuf};
    use std::{env, fs};

    use serial_test::serial;
    use tempfile::tempdir;

    use super::{
        copilot_binary_on_path, find_executable_in_path, fnm_root_candidates_from,
        literal_copilot_executable_names,
    };

    #[test]
    fn fnm_root_candidates_include_xdg_and_legacy_locations() {
        let home = PathBuf::from("/tmp/copilot-home");

        let roots = fnm_root_candidates_from(None, None, Some(home.clone()));

        assert_eq!(
            roots,
            vec![
                home.join(".local").join("share").join("fnm"),
                home.join(".fnm"),
            ]
        );
    }

    #[test]
    fn fnm_root_candidates_prefer_explicit_locations_first() {
        let home = PathBuf::from("/tmp/copilot-home");
        let explicit_fnm_dir = PathBuf::from("/tmp/custom-fnm");
        let xdg_data_home = PathBuf::from("/tmp/xdg-data");

        let roots = fnm_root_candidates_from(
            Some(explicit_fnm_dir.clone()),
            Some(xdg_data_home.clone()),
            Some(home.clone()),
        );

        assert_eq!(
            roots,
            vec![
                explicit_fnm_dir,
                xdg_data_home.join("fnm"),
                home.join(".local").join("share").join("fnm"),
                home.join(".fnm"),
            ]
        );
    }

    #[test]
    fn fnm_root_candidates_ignore_empty_xdg_data_home() {
        let home = PathBuf::from("/tmp/copilot-home");

        let roots = fnm_root_candidates_from(None, Some(PathBuf::new()), Some(home.clone()));

        assert_eq!(
            roots,
            vec![
                home.join(".local").join("share").join("fnm"),
                home.join(".fnm"),
            ]
        );
        assert!(!roots.iter().any(|path| path == &PathBuf::from("fnm")));
    }

    #[test]
    fn fnm_root_produces_expected_bin_paths() {
        let temp_dir = tempdir().expect("should create temp dir");
        let root = temp_dir.path().join("fnm-root");
        let alias_bin = root.join("aliases").join("default").join("bin");
        let version_bin = root
            .join("node-versions")
            .join("v22.18.0")
            .join("installation")
            .join("bin");

        fs::create_dir_all(&alias_bin).expect("should create fnm alias bin");
        fs::create_dir_all(&version_bin).expect("should create fnm version bin");

        let roots = fnm_root_candidates_from(Some(root.clone()), None, None);
        assert_eq!(roots, vec![root.clone()]);

        // Verify the expected bin paths exist under the root structure
        assert!(alias_bin.is_dir());
        assert!(version_bin.is_dir());
    }

    #[test]
    fn find_copilot_in_path_finds_binary_in_path_entries() {
        let temp_dir = tempdir().expect("should create temp dir");
        let bin_dir = temp_dir.path().join("bin");
        fs::create_dir_all(&bin_dir).expect("should create bin dir");

        let executable_name = literal_copilot_executable_names()
            .into_iter()
            .next()
            .expect("should provide a copilot executable name");
        let executable_path = bin_dir.join(&executable_name);
        fs::write(&executable_path, "#!/bin/sh\n").expect("should create fake binary");

        let path_env =
            env::join_paths([Path::new("/missing"), bin_dir.as_path()]).expect("should build PATH");

        assert_eq!(
            find_executable_in_path(
                Some(path_env.as_os_str()),
                &literal_copilot_executable_names()
            ),
            Some(executable_path)
        );
    }

    #[test]
    fn find_copilot_in_path_ignores_missing_entries() {
        let path_env = env::join_paths([Path::new("/missing-one"), Path::new("/missing-two")])
            .expect("should build PATH");

        assert_eq!(
            find_executable_in_path(
                Some(path_env.as_os_str()),
                &literal_copilot_executable_names()
            ),
            None
        );
    }

    #[test]
    #[serial]
    #[cfg(target_os = "macos")]
    fn platform_dirs_precede_version_manager_dirs() {
        let temp = tempdir().expect("should create temp dir");
        let fake_home = temp.path().join("home");

        // Create fake nvm version dirs so collect_nvm_paths() returns entries.
        let nvm_dir = fake_home.join(".nvm");
        let nvm_version_bin = nvm_dir
            .join("versions")
            .join("node")
            .join("v18.0.0")
            .join("bin");
        fs::create_dir_all(&nvm_version_bin).expect("should create nvm version bin");

        // Create fake nodenv version dirs.
        let nodenv_root = fake_home.join(".nodenv");
        let nodenv_version_bin = nodenv_root.join("versions").join("20.0.0").join("bin");
        fs::create_dir_all(&nodenv_version_bin).expect("should create nodenv version bin");

        // Create fake fnm version dirs.
        let fnm_root = fake_home.join(".local").join("share").join("fnm");
        let fnm_version_bin = fnm_root
            .join("node-versions")
            .join("v22.0.0")
            .join("installation")
            .join("bin");
        fs::create_dir_all(&fnm_version_bin).expect("should create fnm version bin");

        // Save env vars.
        let prev_path = env::var_os("PATH");
        let prev_home = env::var_os("HOME");
        let prev_nvm_dir = env::var_os("NVM_DIR");
        let prev_nodenv_root = env::var_os("NODENV_ROOT");
        let prev_fnm_dir = env::var_os("FNM_DIR");
        let prev_xdg_data_home = env::var_os("XDG_DATA_HOME");

        // Set env: empty PATH so only append_standard() dirs appear,
        // HOME to our fake home, and explicit version-manager roots.
        // Safety: test-only, single-threaded via #[serial].
        unsafe {
            env::set_var("PATH", "");
            env::set_var("HOME", &fake_home);
            env::set_var("NVM_DIR", &nvm_dir);
            env::set_var("NODENV_ROOT", &nodenv_root);
            env::remove_var("FNM_DIR");
            env::remove_var("XDG_DATA_HOME");
        }

        let paths: Vec<PathBuf> = super::standard_search_paths().into_iter().collect();

        // Restore env vars.
        // Safety: test-only, single-threaded via #[serial].
        unsafe {
            match prev_path {
                Some(v) => env::set_var("PATH", v),
                None => env::remove_var("PATH"),
            }
            match prev_home {
                Some(v) => env::set_var("HOME", v),
                None => env::remove_var("HOME"),
            }
            match prev_nvm_dir {
                Some(v) => env::set_var("NVM_DIR", v),
                None => env::remove_var("NVM_DIR"),
            }
            match prev_nodenv_root {
                Some(v) => env::set_var("NODENV_ROOT", v),
                None => env::remove_var("NODENV_ROOT"),
            }
            match prev_fnm_dir {
                Some(v) => env::set_var("FNM_DIR", v),
                None => env::remove_var("FNM_DIR"),
            }
            match prev_xdg_data_home {
                Some(v) => env::set_var("XDG_DATA_HOME", v),
                None => env::remove_var("XDG_DATA_HOME"),
            }
        }

        let platform_dirs: Vec<PathBuf> = vec![
            PathBuf::from("/opt/homebrew/bin"),
            PathBuf::from("/usr/local/bin"),
            PathBuf::from("/usr/bin"),
            PathBuf::from("/bin"),
            PathBuf::from("/usr/sbin"),
            PathBuf::from("/sbin"),
        ];

        // Find the last platform dir index and the first version-manager dir index.
        let last_platform_idx = platform_dirs
            .iter()
            .filter_map(|d| paths.iter().position(|p| p == d))
            .max()
            .expect("at least one platform dir should be present");

        let version_manager_prefixes = [
            nvm_version_bin.parent().unwrap().parent().unwrap(), // .nvm/versions/node
            nodenv_version_bin.parent().unwrap().parent().unwrap(), // .nodenv/versions
            fnm_version_bin
                .parent()
                .unwrap()
                .parent()
                .unwrap()
                .parent()
                .unwrap()
                .parent()
                .unwrap(), // .local/share/fnm
        ];

        let first_version_mgr_idx = paths
            .iter()
            .position(|p| {
                version_manager_prefixes
                    .iter()
                    .any(|prefix| p.starts_with(prefix))
            })
            .expect("at least one version-manager dir should be present");

        assert!(
            last_platform_idx < first_version_mgr_idx,
            "Platform dirs (last at index {last_platform_idx}) must precede \
             version-manager dirs (first at index {first_version_mgr_idx}).\n\
             Full path list: {paths:#?}"
        );
    }

    #[test]
    #[serial]
    fn find_executable_in_path_can_ignore_copilot_name_override() {
        let temp_dir = tempdir().expect("should create temp dir");
        let bin_dir = temp_dir.path().join("bin");
        fs::create_dir_all(&bin_dir).expect("should create bin dir");

        let path_executable_name = literal_copilot_executable_names()
            .into_iter()
            .next()
            .expect("should provide a literal copilot executable name");
        #[cfg(target_os = "windows")]
        let overridden_executable_name = "my-copilot.exe";

        #[cfg(not(target_os = "windows"))]
        let overridden_executable_name = "my-copilot";

        let path_executable_path = bin_dir.join(&path_executable_name);
        let overridden_executable_path = bin_dir.join(overridden_executable_name);

        fs::write(&path_executable_path, "#!/bin/sh\n").expect("should create literal fake binary");
        fs::write(&overridden_executable_path, "#!/bin/sh\n")
            .expect("should create overridden fake binary");

        let path_env =
            env::join_paths([Path::new("/missing"), bin_dir.as_path()]).expect("should build PATH");

        let previous_path = env::var_os("PATH");
        let previous_copilot_cli_name = env::var_os("COPILOT_CLI_NAME");
        // Safety: test-only, single-threaded via #[serial].
        unsafe {
            env::set_var("PATH", &path_env);
            env::set_var("COPILOT_CLI_NAME", "my-copilot");
        }

        let resolved_path = copilot_binary_on_path();

        // Safety: test-only, single-threaded via #[serial].
        unsafe {
            if let Some(previous_path) = previous_path {
                env::set_var("PATH", previous_path);
            } else {
                env::remove_var("PATH");
            }

            if let Some(previous_copilot_cli_name) = previous_copilot_cli_name {
                env::set_var("COPILOT_CLI_NAME", previous_copilot_cli_name);
            } else {
                env::remove_var("COPILOT_CLI_NAME");
            }
        }

        assert_eq!(
            resolved_path.expect("should find the literal copilot binary on PATH"),
            path_executable_path
        );
    }
}
