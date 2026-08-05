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

## Verify shared-library permissions on Linux

Build the fixture as a shared library, deliberately remove every execute bit,
and pass its path to the spike:

```sh
cc -shared -fPIC -o target/libpermission_probe.so src/main/c/permission_probe.c
chmod 0644 target/libpermission_probe.so
java -jar target/spike-3-6-platform-detection-linux-x64.jar target/libpermission_probe.so
```

The spike fails if the file has an execute bit or if JNA cannot load and invoke
it. A successful run prints:

```
INFO: PASS: JNA loaded and invoked a shared library with permissions [OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ]
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
