#[cfg(feature = "bundled-in-process")]
#[path = "build/in_process.rs"]
mod implementation;

#[cfg(not(feature = "bundled-in-process"))]
#[path = "build/out_of_process.rs"]
mod implementation;

fn main() {
    implementation::main();
}
