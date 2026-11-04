# Maven versions and reference implementation tracking

## Decision

Java previews use Maven `-SNAPSHOT` versions and publish to Central Portal Maven snapshots, rather than becoming immutable Maven Central releases. Stable Java releases retain the shared SDK version and the existing Maven Central release publication path.

The Java Maven version is derived from the shared SDK release identity:

| Release channel | Java Maven version | Publication destination |
| --- | --- | --- |
| Public prerelease, scheduled or manual | `<major>.<minor>.<patch>-SNAPSHOT` | `https://central.sonatype.com/repository/maven-snapshots/` |
| Public latest, including stable promotion | `<SDK-version>` | Maven Central releases |
| Public unstable | Existing version, unchanged | Existing destination, unchanged |
| Internal-only unstable | Java remains unselected | No Java publication |

For example, SDK `1.0.15-preview.1` and SDK `1.0.15-preview.2` both produce Java `1.0.15-SNAPSHOT`. Advancing the shared base version to `1.0.16-preview.1` changes the Java coordinate to `1.0.16-SNAPSHOT`. Both `com.github:copilot-sdk-java` and `com.github:copilot-sdk-java-runtime`, including its native classifiers, use the same projected Java version.

This projection is Java-only. Shared SDK versions, runtime versions and pins, source tags, stable promotion rules, documentation directories, and every other SDK's publication behavior remain unchanged. Java documentation carries the shared SDK version for site identity and source links and the projected Java version for Maven, Gradle, and JBang dependencies. The original `-java.N` naming decision below is historical and is superseded by this policy; it must not be reintroduced into the matched release pipeline.

## Status

Corrected on November 3, 2026, following clarification from Ed Burns that previews of the same base SDK version update one rolling Maven snapshot coordinate. The November 2 amendment incorrectly preserved the preview qualifier and ordinal.

**Accepted and implemented in the runtime release tooling; rollout verification pending.** The matched pipeline projects Java preview versions to snapshots, and the independent publisher is retired. Successful remote publication and removal of the already-exported public workflow must be verified during rollout.

## Current context and consequences

The runtime release workflow schedules fresh prereleases three times per weekday, at 10:00, 13:00, and 15:00 in `America/Los_Angeles`. It separately schedules stable promotion at 09:00 on weekdays. Before this amendment, Java packaging and publishing overrode the committed development snapshot with the shared non-snapshot SDK version.

The original qualifier experiment established that Maven Central accepts a `-java.N` release version. It did not establish that frequent ephemeral previews should be permanent releases. Actual publication has since moved to shared SDK versions, making this distinction necessary.

Sonatype's [Central immutability policy](https://central.sonatype.org/faq/can-i-change-a-component/) prohibits changing or deleting published releases. Its [snapshot publishing policy](https://central.sonatype.org/publish/publish-portal-snapshots/) permits updating snapshots and currently specifies cleanup after 90 days. Rolling means one logical coordinate per base version, not one physical file or immediate storage reclamation: Maven deploys timestamped builds and updates `maven-metadata.xml`, while repository retention governs older snapshot storage.

Preview consumers must configure the snapshot repository explicitly. Resolving a mutable snapshot gets the repository's current build for that coordinate, not necessarily the build associated with an older preview's documentation directory. The release pipeline's existing publication order is unchanged and does not promise source-order publication. Snapshot artifacts do not receive Central release validation, so existing local artifact, checksum, source, and runtime-pin validation remains necessary.

## Implementation

* Use one Java-only version projection consistently in packaging and publishing: Maven `revision`, local repository coordinates, retained JAR names, and native classifier paths must agree.
* Pass the shared SDK version and projected Java Maven version separately to Java documentation. Keep site directories and source links on the shared identity while dependency examples use the projected artifact version.
* Reuse native Maven deployment, the existing `central` server credentials, and the existing Central publishing plugin. Its [documented snapshot support](https://central.sonatype.org/publish/publish-portal-maven/) routes `-SNAPSHOT` versions to snapshot publication. Keep stable signing and publication unchanged.
* Use the matched release pipeline's existing three weekday preview slots; do not add another schedule or change other SDKs.
* Retire the independent Java snapshot workflow and remove its explicit preservation from the runtime's SDK exporter. Coordinate removal of the already-exported public workflow so it cannot race the replacement.
* Preserve the committed development snapshot version. A stable promotion rebuilds from the selected source with a non-snapshot Java version; it does not relabel preview JARs.
* Leave previously published releases and shared source history intact.

The implementation is the runtime repository's Java branches in `src/sdk/nodejs/scripts/package-release.ts` and `src/sdk/nodejs/scripts/publish-release.ts`, called by `.github/workflows/publish.yml` and `.github/workflows/sdk-publish-release.yml`. The retired publisher was `src/sdk/.github/workflows/java-publish-snapshot.yml`, exported as `.github/workflows/java-publish-snapshot.yml` in `github/copilot-sdk`. The exporter, `script/sdk-release-snapshot.ts`, no longer restores it. The matched pipeline is the sole Java preview publisher after the updated export reaches the public repository.

## Historical context and problem statement

The original decision sought to track reference implementation releases with the same base number while allowing multiple Java releases for each reference release.

## Historical considered options

- Simple number qualifier (0.1.32-0, 0.1.32-1, ...) fails on a subtle but important point: 0.1.32-0 is treated identically to 0.1.32 by Maven (trailing zeros are normalized away), and bare numeric qualifiers are pre-release semantics. Your "first release" would sort before the reference implementation bare version.

- Java and number in the qualifier (0.1.32-java.N)

   - java is an unknown qualifier that sorts correctly and accurately describes what it is — the Java-ecosystem release of this version.

- sp and number in the qualifier  (0.1.32-sp.N)

   - sp is a known Maven qualifier with a specific semantic ("service pack"), which is slightly misleading since your -java.0 isn't a service pack — it's the primary release.

My recommendation, with the test results backing it up: 0.1.32-java.0, 0.1.32-java.1, 0.1.32-java.2. It passes every constraint you stated, is accepted by Sonatype (arbitrary string, not ending in -SNAPSHOT), and is self-documenting. The only thing left to validate empirically is actually uploading one to the portal — but nothing in Sonatype's published requirements would reject it. See below for empirical validation.

## Historical decision outcome

The original decision chose option "Java and number in qualifier". The amended decision above replaces this naming policy.

## Historical supporting information

Output from experiment to publish an artifact with GAV `io.github.edburns:helloworld:0.1.31-java.0`:

Everything looks healthy. Here's the status:

   - ✅ GPG signing succeeded (no passphrase errors this time)
   - ✅ Upload to Central succeeded — deployment ID 
  e8192af5-769d-4cd3-a6d9-5fc38b4dbf10 was accepted
   - ✅ Authentication is working — all status checks return successfully
   - ⏳ State: publishing — Central accepted the bundle and is propagating it.
   The plugin is polling repeatedly (27+ checks so far), waiting for state to 
  change to published.

  This is normal. Maven Central publishing can take 5–30 minutes to transition
  from publishing → published. The waitUntil=published setting in the POM
  means Maven will keep polling until it completes. Just let it run.

> Given that the purpose of this exercise was to test your assertion that a GAV with a Version such as this one would be valid to publish to Maven central , can we already conclude this is a true assertion?

- Yes. The version 0.1.31-java.0 has already passed Maven Central's
  validation. The publishing state means Central accepted the bundle —
  including its GAV — and is now replicating it to mirrors. If the version
  were invalid, it would have been rejected during validation before entering
  the publishing state.

  So yes, the assertion holds: Maven Central accepts versions with dots inside
  the qualifier segment (e.g., 0.1.31-java.0).

## Related work items

- https://devdiv.visualstudio.com/DevDiv/_workitems/edit/2766089
