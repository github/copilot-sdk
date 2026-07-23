# Spike 3.6 — Platform detection (darwin-arm64 start)

This spike is a standalone Java program that detects:

- OS (`darwin`, `linux`, `win32`)
- arch (`x64`, `arm64`)
- Linux libc (`glibc`, `musl`, `unknown`)
- final runtime classifier (for `native/<classifier>/runtime.node`)

It includes ELF `PT_INTERP` parsing from `/proc/self/exe` for Linux musl/glibc detection.

## Build

```sh
mvn clean package
```

## Run

```sh
java -jar target/spike-3-6-platform-detection-darwin-arm64.jar
```
