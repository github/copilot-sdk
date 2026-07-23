# Spike 3.6 — Platform detection (linux-x64)

This spike is a standalone Java program that detects:

- OS (`darwin`, `linux`, `win32`)
- arch (`x64`, `arm64`)
- Linux libc (`GLIBC`, `MUSL`, `UNKNOWN`) via ELF `PT_INTERP` parsing of `/proc/self/exe`
- final runtime classifier (for `native/<classifier>/runtime.node`)

On this platform (linux-x64 glibc), it exercises the ELF parsing path and verifies
that `/proc/self/exe` resolves to the JVM binary, whose `PT_INTERP` segment contains
the glibc dynamic linker path `/lib64/ld-linux-x86-64.so.2`.

## Build

```sh
mvn clean package
```

## Run

```sh
java -jar target/spike-3-6-platform-detection-linux-x64.jar
```

## Expected output (glibc linux-x64)

```
INFO: Detected os=linux
INFO: Detected arch=x64
INFO: Detected linuxLibc=GLIBC
INFO: Detected classifier=linux-x64
INFO: ELF PT_INTERP=/lib64/ld-linux-x86-64.so.2
INFO: PASS: PT_INTERP matches expected glibc x86_64 dynamic linker
INFO: --- Spike result ---
INFO: Classifier for native binary selection: linux-x64
INFO: Resource path would be: native/linux-x64/runtime.node
```
