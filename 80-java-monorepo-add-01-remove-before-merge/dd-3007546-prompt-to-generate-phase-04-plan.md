
Above, you used the phrase "two-lane". Going forward I see it as "one lane" and then "two lane".

- one lane: update the `update-copilot-dependency` to also include java.

- two lanes:

   - Manual by-hand updates to fix any breakages.
   
   - Manually trigged agentic workflow, similar to what we had in **standalone** to fix the breakages.
   
Write a plan to `80-java-monorepo-add-01-remove-before-merge/dd-3007546-agentic-fix-cli-upgrade-related-breakage-plan.md` that is a `copilot --yolo` ready plan to make these changes. It should have phases for the "one lane" and "two lane" sections. For each phase, it should loop until the goal for that phase is achieved. 

Let's break down the goal, loop and validation for each phase. 

✅✅ When I ask `copilot --yolo` to `execute 80-java-monorepo-add-01-remove-before-merge/dd-3007546-agentic-fix-cli-upgrade-related-breakage-plan.md as a prompt` it should start the process and complete it per the plan I'm asking you to now build, based on the following specification.

# Phase "one lane"

- Goal: When `update-copilot-dependency` is invoked, the existing stuff happens **and** the upgrade is also performed in Java.

   - The POM property `readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync` is updated.
   
   - The `.lastmerge` hash points to a commit that has the same version of Copilot CLI as in the POM, but for `copilot-sdk/test/harness/package.json`.
   
   - The POM is invoked to make it so `java.ts` is invoked **after** all the other generators are invoked. **and** if the generation fails, the job fails. Recall that you stated:
   
      > The workflow simply **fails**. There is no fallback or repair logic.
      > 
      > Specifically:
      > 
      > 1. The `Run codegen` step (`npm run generate` in codegen) errors out
      > 2. GitHub Actions halts the job (no `continue-on-error` is set on that step)
      > 3. The "Format generated code" and "Create pull request" steps never execute
      > 4. No PR is created — the version bump is dead in the water

- Loop

   - Make the changes you think you need to make locally.
   
   - Push them upstream to the topic branch of the monorepo `upstream/edburns/80-java-monorepo-iterating`.
   
   - **ON THE BRANCH, and ON THE BRANCH ONLY** invoke the `update-copilot-dependency` workflow.
   
      This will be a challenge, because we have already upgraded to the latest version. I don't have a good solution for this. You'll have to make something up. Maybe create a synthetic new version of `@github/copilot` that has a trivial, non-breaking change, and set up the necessary harness to stage and upgrade to that version?
      
   - Look at the result of the workflow invocation.
   
      - If it failed, discover why, push fixes, and run it again.
      
      - If it succeeded, move on to the next phase.
      
   - Do not go around this loop more than ten times. If you get ten times in, declare failure and stop the whole process.
      
- Validation

   Write a `80-java-monorepo-add-01-remove-before-merge/phase-one-lane-validation.md` file that is sufficent for Copilot to read and grade its success. Make it so this validation is executed after it the loop has exited.
      
# Phase "two lanes"

- lane 01: manual fix

   - Goal: modify `update-copilot-dependency` so that the created PR includes a creates a detailed agentic plan so the human agent can do the work to fix what needs fixing.
   
   - Loop: 
   
      - Run the workflow.
      
      - Look at the PR created and verify that the PR includes such a plan.
      
      - Do not go around this loop more than ten times. If you get ten times in, declare failure and stop the whole process.
      
   - Validation

      Write a `80-java-monorepo-add-01-remove-before-merge/phase-two-lanes-manual-fix-validation.md` file that is sufficent for Copilot to read and grade its success. Make it so this validation is executed after it the loop has exited.
      
   
- lane 02: agentic fix

   - Goal: an agentic workflow exists that is similar to what we had in standalone, but it assumes the `java.ts` and corresponding code updates succeeded. Therefore, this agentic workflow just needs to ensure the hand-written parts of the SDK are adapted to handle the changes in the generated code **and** add new tests if necessary.
   
      - Create new `gh aw` agentic workflow `copilot-sdk/.github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md` including your best understanding of what this workflow should contain.
      
         If `gh aw compile` hangs, create a new shell and run the command in that new shell.
      
         It should update the handwritten code to adapt to changes introduced in the new `@github/copilot` version. It should generate tests if necessary.
         
         ❌❌ Create no new tests in the `src/test/java` `com.github.copilot.generated` package. This is a known gap. ❌❌
      
         The workflow must invoke `copilot-sdk/.github/workflows/java-sdk-tests.yml` to ensure the old and new tests work.
   
   - Loop:
   
      - Assume Phase "one lane" succeeded. There is now a new Copilot CLI in POM and the test harness.
   
      - Make the next draft of `copilot-sdk/.github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md`, including your best understanding of what this workflow should contain.
      
      - Use `gh aw` to compile it and produce the lock file.
      
      - Run the agentic workflow ** ON THE TOPIC BRANCH **.
      
      - Look at the output of the run. 
      
      - Make changes, if necessary.

      - Do not go around this loop more than ten times. If you get ten times in, declare failure and stop the whole process.
      
   - Validation

      Write a `80-java-monorepo-add-01-remove-before-merge/phase-two-lanes-agentic-fix-validation.md` file that is sufficent for Copilot to read and grade its success. Make it so this validation is executed after it the loop has exited.
