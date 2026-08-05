# Spike 3.8: JNA Callbacks in GraalVM Native Image

This experiment tests the callback-dependent runtime ABI with JNA 5.19.1 on
both a regular JVM and a GraalVM Native Image executable. The Rust fixture has
two callback paths:

1. `callback_once` invokes Java synchronously on the Java calling thread.
2. `connection_open` starts a Rust thread that invokes Java five times.

Both paths use the same named JNA callback implementation and bridge data into
a `QueueInputStream` backed by a `BlockingQueue`.

## Result

**JNA callbacks are not viable in the tested Native Image configuration.**

The regular JVM passes the entire fixture: the synchronous callback and all
five Rust-thread callbacks enter Java, all six messages traverse the queue,
cleanup succeeds, and the process exits 0.

Native Image builds a 21 MB executable and successfully:

- initializes JNA and extracts `jnidispatch.dll`;
- loads `callback_test.dll`;
- calls ordinary native functions such as `host_start`;
- converts the Java callback to a native function pointer.

It fails when native code invokes that function pointer. The synchronous
`callback_once` control fails before the first line of Java callback code with:

```text
java.lang.Error: Invalid memory access
    at ...JNIFunctions$NewObjectWithObjectArrayArgFunctionPointer.invoke
    at ...JNIFunctions.ThrowNew
    at com.sun.jna.Native.invokeVoid(Native Method)
```

Before the synchronous control was added, the Rust-thread callback produced a
Native Image segfault in
`JNIJavaCallTrampolineHolder.varargsJavaCallTrampoline`. Because the
synchronous control fails too, the root problem is the JNA callback upcall, not
attachment of a Rust-created thread.

This is a result for the exact tested stack, not a claim about every GraalVM,
JNA, operating system, or architecture combination:

- Windows x64
- Oracle GraalVM 25.0.4+7.1
- JNA 5.19.1 (`jna` only, no `jna-platform`)
- Native Build Tools Maven plugin 0.11.3
- Maven 3.9.14
- Visual Studio Build Tools 2022 17.14 and Windows SDK 10.0.26100.0

## Reachability Metadata

JNA's repository metadata is necessary but insufficient for application types.
The build selects the latest available JNA metadata, version 5.8.0, for JNA
5.19.1. Application metadata in `reachability-metadata.json` must additionally
register:

- a dynamic proxy for `CallbackTestLibrary`;
- the `OutboundCallback.invoke(Pointer, Pointer, int)` method for reflection;
- the callback method for JNI access;
- the named `OutboundCallbackImpl.invoke` method for reflection and JNI access.

Without the application proxy registration, loading the JNA library interface
fails. Without reflection registration for the callback interface, JNA reports
that the callback has no single public method. Adding all registrations permits
callback creation but does not make callback invocation work.

## Reproduce on Windows

Build the Rust DLL:

```powershell
Set-Location ..\rust-dll
cargo build
```

From the repository's `java` directory, build the jar and run the passing JVM
control:

```powershell
$spike = "..\1917-java-embed-rust-cli-runtime-remove-before-merge\spike-3-8-graal-research"
$javaSpike = "$spike\java-program-that-invokes-rust-dll-jdk17"
$log = "$(Get-Date -Format 'yyyyMMdd-HHmm')-job-logs.txt"
mvn -f "$javaSpike\pom.xml" package 2>&1 | Tee-Object -FilePath $log
java "-Djna.library.path=$spike\rust-dll\target\debug" `
    -jar "$javaSpike\target\jna-graal-callback-spike-0.1.0.jar" 5
```

With `JAVA_HOME` set to GraalVM, build and run the failing native executable:

```powershell
$log = "$(Get-Date -Format 'yyyyMMdd-HHmm')-job-logs.txt"
mvn -f "$javaSpike\pom.xml" -Pnative clean package 2>&1 | `
    Tee-Object -FilePath $log
& "$javaSpike\target\jna-graal-callback-spike.exe" `
    "-Djna.library.path=$spike\rust-dll\target\debug" 5
```

Native Build Tools 1.1.6 was also tried, but its Maven extension initialization
failed under Maven 3.9.14 before Native Image compilation. Version 0.11.3 was
therefore used for the executable test.
