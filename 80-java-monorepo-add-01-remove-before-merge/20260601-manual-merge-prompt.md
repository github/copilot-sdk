#### CONTEXT

This Copilot CLI session workspace has two roots.

- `copilot-sdk` The GitHub Copilot SDK monorepo. We are interested in only the `java` folder and things that support developing the code in that folder. The topic branch on which to operate is currently checked out. `edburns/update-java-to-CLI-1_0_57` Any commits must be pushed to upstream.

- `copilot-sdk-java` The former standalone repo for the GitHub Copilot SDK for Java. I have this open for reference. You only need to refer to this if you get stuck doing the upgrade operation described in this prompt.

The reason I have both roots is that the migration from `copilot-sdk-java` standalone to `copilot-sdk-java` monorepo is not complete. What remains is the agentic `reference-impl-sync` machinery. There may be some useful knowledge in the following files.

   - `copilot-sdk-java/.github/workflows/reference-impl-sync.md`
   
   - `copilot-sdk/.github/workflows/codegen-check.yml`
   
   - `copilot-sdk-java/.github/workflows/codegen-agentic-fix.md`

✅✅✅ I have a separate task to complete this migration by bringing over the agentic workflow mechanism, but for my session now, I need you to do it "by hand" as a "one-off". I'll describe the process at a high level.

#### JOB TO BE DONE

1. A new version of GitHub Copilot CLI has been released. `1.0.57`.

2. I need to make it so the Java SDK is updated to depend on that version, rather than whatever version it currently depends on, which happens to be `^1.0.55-5`.

3. There are two important ways in which the Java SDK is dependent on the version of Copilot CLI. This version dependency is expressed via the version of the npm module `@github/copilot`.
   
   1. Test harness used by Java

      The test harness uses `@github/copilot` at the exact same pinned version in `copilot-sdk/test/harness/package.json`. The system Copilot CLI is not used.

      The version of Copilot CLI supported by the java implementation is synced to the version in `copilot-sdk/test/harness/package.json` by stamping the known-good version as of the git hash for a commit that includes the desired version. This hash stamp is saved in the `copilot-sdk/java/.lastmerge` file. This file is the git hash of the commit of `copilot-sdk` repo the last time the sync operation was performed. The version It corresponds to the `package.json

      The test harness used by Java checks out `copilot-sdk` at the `.lastmerge` hash into the `target` directory. This mechanism is implemented in `copilot-sdk/java/pom.xml`.

   2. Code generation from Zod schemas in `@github/copilot`.

      ✅✅ It is vitally important that code generation uses the exact same version of `@gihthub/copilot` as in the test harness.✅✅ This is achieved by reading the version from the `copilot-sdk/test/harness/package.json` and saving it into the `copilot-sdk/java/pom.xml` property `readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync`.

      `@github/copilot` contains the Zod schemas for the API and RPC events. The code generation script `java/scripts/codegen/java.ts` is tightly bound to these schemas. This code gen script is invoked from logic in `copilot-sdk/java/pom.xml` and causes the code in `copilot-sdk/java/src/generated/java` to be generated. The hand written code depends on and uses this generated code.

      Generally speaking, when upgrading the `@github/copilot` dependency version, there may be some changes in the schemas that cause `java.ts` to need to be updated. In that case, the necessary updates to `java.ts` must be performed. If updates need to be performed, we must use the `copilot-sdk/java/pom.xml` to re-generate the code.

      Then, we must re-compile the `copilot-sdk-java` JAR artifact that is the sole artifact produced by `copilot-sdk/java/pom.xml`.

      It is possible the changes to the generated code will cause breakages to the hand-written code that interacts with the generated code. If that happens, those breakages must be fixed.

#### WHAT the HUMAN has done so far.

1. Updated `copilot-sdk/java/.lastmerge` to have the hash corresponding to `1.0.57`.

2. Updated `readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync` in `copilot-sdk/java/pom.xml` to have `^1.0.57`.

#### WHAT YOU NEED TO DO

Account for the rest. At a high level, this includes, but may not be limited to:

0. Understand how the test harness is configured and invoked in `copilot-sdk/java/pom.xml`.

1. 

1. See if the `java.ts` needs to be updated. If so, do the necessary updates.

2. If `java.ts` has been updated, regenerate the code using the `copilot-sdk/java/pom.xml`. ❌❌❌ You really should not need to edit the POM to do this. This is a solved problem and you should just be able to invoke the existing logic to run the `java.ts` script.

3. If new code is generated.

   - ensure the `copilot-sdk-java` jar artifact still compiles. If there are failures, you must fix them and get a clean compile.
   
     `mvn jar:jar`
     
     If you are tempted to generate `copilot-sdk/java/src/generated/java/`, ❌❌❌YOU MUST NEVER TO THAT!❌❌❌ 
     
     ✅✅ The only way to affect change to code in `copilot-sdk/java/src/generated/java/` is via `java.ts` and re-running the code gen.
     
     Therefore, the act of fixing code due to changes in the Zod schemas involves both editing `java.ts` and also editing the hand-generated code that interacts with the `java.ts` generated code.
   
   - create new tests for the generated code, if necessary, in `copilot-sdk/java/src/test/java/com/github/copilot/generated/` and its sub-packages, according to existing local and global Java testing best practices. We must keep the code coverage numbers at least at 80%.
   
4. If any hand generated code is changed, you must also write new tests, or update existing tests, to cover those changes.

5. You must get a clean run of the tests on Java 25. You can assume the system is configured with Java 25.

  Here is the process for invoking the tests correctly. In `copilot-sdk/java`:

  ```
  mvn clean
  ```
  
  Make sure the `target` directory is really gone. Sometimes it fails to be removed. You must remove it.
  
  ```
  mvn test-compile jar:jar
  mvn verify -Dskip.test.harness=true
  ```
