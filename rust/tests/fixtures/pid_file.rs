use std::fs::File;
use std::io;
use std::path::Path;

pub fn publish_pid_file(
    path: &Path,
    write_pid: impl FnOnce(&mut File) -> io::Result<()>,
) -> io::Result<()> {
    let staging_path = path.with_extension("pid.tmp");
    let mut file = File::create(&staging_path)?;
    write_pid(&mut file)?;
    drop(file);
    std::fs::rename(staging_path, path)
}
