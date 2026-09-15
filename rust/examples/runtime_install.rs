//! Installer-only probe. No CLI subprocess, authentication, or model requests.
//! Run with an isolated HOME to keep the installation cache separate.

use std::io::{self, Write};
use std::time::Instant;

use github_copilot_sdk::install_bundled_runtime;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("ready {}", std::process::id());
    io::stdout().flush()?;
    let mut line = String::new();
    io::stdin().read_line(&mut line)?;

    let start = Instant::now();
    let path = install_bundled_runtime().ok_or("bundled runtime installation failed")?;
    let elapsed = start.elapsed();
    assert_eq!(install_bundled_runtime().as_ref(), Some(&path));
    println!("installed {}", elapsed.as_secs_f64());
    io::stdout().flush()?;
    line.clear();
    io::stdin().read_line(&mut line)?;
    Ok(())
}
