### 1. npm cross-platform install behavior — unverified assumption

The entire package.json approach assumes `npm ci` will install all 8 `@github/copilot-*` packages regardless of host OS. These platform packages likely declare `os` and `cpu` fields in their own package.json, which causes npm to skip them when they don't match the host. Listing them as `dependencies` (not `optionalDependencies`) should make npm **error** rather than skip — but it might still refuse to install a `linux-x64` package on a Windows build host.

**Needs verification:** Run `npm install @github/copilot-linux-x64@1.0.69-2` on your Windows machine and see if it succeeds or errors. If it errors, the package.json approach needs `--force` or a different download mechanism (e.g., `npm pack` per-package, or direct tarball URL downloads like Rust does).

**Resolution:**

> 4. **Cleanest alternative:** Skip package.json entirely for the native module. Use `exec-maven-plugin` to run `npm pack @github/copilot-<platform>@<version>` for each platform, then extract with `tar`. Version is parameterized from the POM. Integrity is verified by checking SHA-512 from the monorepo's package-lock.json post-download.


### 2. SDK → runtime dependency relationship is undefined

The ADR says "The existing `copilot-sdk-java` coordination artifact depends on it." But depends on *what* exactly? It can't declare a dependency on all 8 classifiers — that defeats the purpose. Options:

- **No dependency at all** — consumer declares both `copilot-sdk-java` and `copilot-sdk-java-runtime:<classifier>` manually (what we showed earlier)
- **Optional dependency on the unclassified placeholder** — signals the relationship but doesn't pull binaries
- **Provided-scope dependency** — consumer must supply the runtime JAR

This affects consumer UX and should be decided explicitly. DJL leaves it to the consumer — `pytorch-engine` does not declare a dependency on `pytorch-native-cpu`.

**Resolution:**

> - **No dependency at all** — consumer declares both `copilot-sdk-java` and `copilot-sdk-java-runtime:<classifier>` manually (what we showed earlier)

### 3. Version coupling: SDK version ≠ runtime version

The SDK version is `1.0.9-preview.0`. The runtime version (from npm) is `1.0.69-2`. These are independently versioned. The `copilot-sdk-java-runtime` Maven artifact must use the **SDK** version (so consumers can align versions), but internally it packages a specific **runtime** version.

Where is the runtime version recorded? The plan's 3.7 mentions a `.properties` file in the JAR, and the ADR says "The version of the bundled `runtime.node` is recorded in the coordination JAR's manifest." But the `copilot-native` module's package.json is what actually pins the runtime version. When the runtime gets a new release, someone must update package.json + `package-lock.json` and cut a new SDK release.

This version mapping needs to be explicit — probably a `native/<classifier>/platform.properties` containing both the SDK version and the runtime version.

**Resolution:** 

The npm package version and the SDK version are the **same version**. `@github/copilot-linux-x64@1.0.9-preview.0` and `com.github:copilot-sdk-java:1.0.9-preview.0` — same string.

So there's no version mapping at all. The `copilot-native` module downloads `@github/copilot-<platform>@${project.version}` from npm. One version, everywhere. No extra property needed.

Gap #3 is not a gap.

### 4. Gradle Module Metadata for variant-aware resolution

Maven classifiers don't map cleanly to Gradle's variant model. Gradle consumers using the Maven repository will see 8 classifier JARs but have no way to automatically resolve the right one via Gradle's attribute matching without a [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html) file (`.module` JSON alongside the POM).

DJL solves this by publishing separate GAVs per platform (`pytorch-native-cpu`, `pytorch-native-cu128`). Without GMM, Gradle consumers must hardcode the classifier just like Maven consumers.

This is a "nice to have" — not a blocker — but worth noting as a future improvement or documenting as a known limitation.

**Resolution:** 

- Generate `copilot-sdk-java-runtime-${project.version}.module` (templated JSON, version/classifier substituted by Maven resource filtering or `maven-antrun-plugin`)
- Attach it via `build-helper-maven-plugin` as type `module`
- `central-publishing-maven-plugin` deploys it alongside the POM and JARs
