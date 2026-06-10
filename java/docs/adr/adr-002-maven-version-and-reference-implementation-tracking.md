# Maven Version and tracking of versions released from the reference implementation

## Context and Problem Statement

Releases of this implementation track releases of the reference implementation. For each release of the reference implementation, there may follow a corresponding relase of this implementation with the same number as the reference implementation, allowing for additional metadata to version multiple releases of this implementation that track the same release of the reference implementation.

## Considered Options

- Simple number qualifier (0.1.32-0, 0.1.32-1, ...) fails on a subtle but important point: 0.1.32-0 is treated identically to 0.1.32 by Maven (trailing zeros are normalized away), and bare numeric qualifiers are pre-release semantics. Your "first release" would sort before the reference implementation bare version.

- Java and number in the qualifier (0.1.32-java.N)

   - java is an unknown qualifier that sorts correctly and accurately describes what it is — the Java-ecosystem release of this version.

- sp and number in the qualifier  (0.1.32-sp.N)

   - sp is a known Maven qualifier with a specific semantic ("service pack"), which is slightly misleading since your -java.0 isn't a service pack — it's the primary release.

My recommendation, with the test results backing it up: 0.1.32-java.0, 0.1.32-java.1, 0.1.32-java.2. It passes every constraint you stated, is accepted by Sonatype (arbitrary string, not ending in -SNAPSHOT), and is self-documenting. The only thing left to validate empirically is actually uploading one to the portal — but nothing in Sonatype's published requirements would reject it. See below for empirical validation.

## Decision Outcome

Choose option "Java and number in qualifier".

## Supporting information

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

