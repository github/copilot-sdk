# Spike 3.6 — Platform detection (win32-x64)

This spike is a standalone Java program that detects:

- OS (`darwin`, `linux`, `win32`)
- arch (`x64`, `arm64`)
- Linux libc (`GLIBC`, `MUSL`, `UNKNOWN`, `NOT_APPLICABLE`)
- final runtime classifier (for `native/<classifier>/runtime.node`)

On this platform (win32-x64), the expected classifier is `win32-x64`.

## Build

```powershell
. "C:\Users\edburns\bin\env-java25.ps1"
mvn clean package
```

## Run

```powershell
java -jar target/spike-3-6-platform-detection-win32-x64.jar
```

## Expected output (win32-x64)

```
INFO: Detected os=win32
INFO: Detected arch=x64
INFO: Detected linuxLibc=NOT_APPLICABLE
INFO: Detected classifier=win32-x64
INFO: PASS: classifier matches expected win32-x64 target
INFO: --- Spike result ---
INFO: Classifier for native binary selection: win32-x64
INFO: Resource path would be: native/win32-x64/runtime.node
```
