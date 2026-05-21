# Prompt: Generate and Run the copilot-sdk-java Smoke Test

## Objective

Create a Maven project that acts as a smoke test for `copilot-sdk-java`. The project must compile, build, and run to completion with exit code 0 as the definition of success.

## Step 1 — Read the source README

Read the file `README.md` at the top level of this repository. You will need two sections from it:

- **"Snapshot Builds"** — provides the Maven GAV (groupId, artifactId, version) and the Maven Central Snapshots repository configuration to use for the dependency under test.
- **"Quick Start"** — provides the exact Java source code for the smoke test program. Use this code verbatim. Do not modify it, fix it, or improve it. If it does not compile or run correctly against the artifact under test, that is itself a smoke test failure and must be reported as such rather than silently corrected.

## Step 2 — Create the Maven project

Create the following file layout in a subdirectory named `smoke-test/` at the top level of this repository:

```
smoke-test/
  pom.xml
  src/main/java/(Class name taken from the code in the "Quick Start" section in the README).java   ← verbatim from README "Quick Start"
```

### `pom.xml` requirements

- **groupId**: `com.github` (or any reasonable value)
- **artifactId**: `copilot-sdk-smoketest`
- **version**: `1.0-SNAPSHOT`
- **packaging**: `jar`
- **Java source/target**: (taken from the "Requirements" section in the README) (via `maven.compiler.source` and `maven.compiler.target` properties)
- **`mainClass` property**: (taken from the "Quick Start" section in the README) (the class is in the default package)

#### Snapshot repository

Configure the Maven Central Snapshots repository exactly as specified in the "Snapshot Builds" section of `README.md`, and add `<updatePolicy>always</updatePolicy>` inside the `<snapshots>` block so that every build fetches the latest snapshot without requiring `-U`:

```xml
<repository>
    <id>central-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
    </snapshots>
</repository>
```

#### Dependency

Use the GAV from the "Snapshot Builds" section of `README.md` verbatim — do not substitute the release version from the "Maven" section.

#### Plugins — REQUIRED configuration

**Do not use `maven-shade-plugin`.** Use the `Class-Path` manifest approach instead:

1. **`maven-jar-plugin`** (version **3.4.1** — pin explicitly to suppress Maven version warnings):

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-jar-plugin</artifactId>
       <version>3.4.1</version>
       <configuration>
           <archive>
               <manifest>
                   <mainClass>${mainClass}</mainClass>
                   <addClasspath>true</addClasspath>
                   <classpathPrefix>lib/</classpathPrefix>
                   <useUniqueVersions>false</useUniqueVersions>
               </manifest>
           </archive>
       </configuration>
   </plugin>
   ```

   **Critical**: `<useUniqueVersions>false</useUniqueVersions>` is mandatory. Without it, the manifest `Class-Path:` entry uses the timestamped SNAPSHOT filename (e.g. `copilot-sdk-java-0.1.33-20260312.125508-3.jar`) while `copy-dependencies` writes the base SNAPSHOT filename (`copilot-sdk-java-0.1.33-SNAPSHOT.jar`), causing `NoClassDefFoundError` at runtime.

2. **`maven-dependency-plugin`** (version **3.6.1**):

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-dependency-plugin</artifactId>
       <version>3.6.1</version>
       <executions>
           <execution>
               <id>copy-dependencies</id>
               <phase>package</phase>
               <goals><goal>copy-dependencies</goal></goals>
               <configuration>
                   <outputDirectory>${project.build.directory}/lib</outputDirectory>
               </configuration>
           </execution>
       </executions>
   </plugin>
   ```

   This copies all runtime dependency JARs into `target/lib/`, which is where the manifest `Class-Path:` points.

## Step 3 — Build

```bash
mvn -U clean package
```

The `-U` flag forces a fresh snapshot metadata check regardless of local cache. The `<updatePolicy>always</updatePolicy>` already handles this for normal invocations, but `-U` is the safest choice for CI.

Build must succeed with `BUILD SUCCESS` before proceeding.

## Step 4 — Run

```bash
java -jar ./target/copilot-sdk-smoketest-1.0-SNAPSHOT.jar
```

The JAR must be run from the `smoke-test/` directory so that the relative `lib/` path in the manifest resolves correctly. Do not use `-cp` or `-classpath` — the test specifically validates that `java -jar` works with the manifest `Class-Path:` approach.

## Step 5 — Verify success

The smoke test passes if and only if the process exits with code **0**.

The "Quick Start" code in `README.md` already contains the exit-code logic: it captures the last assistant message and calls `System.exit(0)` if it contains `"4"` (the expected answer to "What is 2+2?"), or `System.exit(-1)` otherwise.

Check the exit code:
```bash
echo "Exit code: $?"
```

Expected: `Exit code: 0`

## Important API notes (do not apply these as fixes — they are here for diagnostic context only)

If the build fails with compilation errors such as `cannot find symbol` on methods like `getContent()`, `getCurrentTokens()`, `getTokenLimit()`, or `getMessagesLength()`, this indicates a mismatch between the Quick Start code and the SDK implementation. **Do not silently fix the code.** Report the failure. The purpose of this smoke test is precisely to catch such regressions.

For reference: the data classes in `copilot-sdk-java` are Java **records**. Record accessor methods have no `get` prefix — they are named `content()`, `currentTokens()`, `tokenLimit()`, and `messagesLength()`. If the README Quick Start uses `getContent()` etc., that is a bug in the README that must be surfaced, not silently corrected.
