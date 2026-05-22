---
description: |
  Agentic fix for Java codegen-related build/test failures. Invoked when
  mvn verify fails after code generation changes.

on:
  workflow_dispatch:
    inputs:
      branch:
        description: 'Branch to fix'
        required: true
        type: string
      pr_number:
        description: 'PR number to push fixes to'
        required: true
        type: string
      error_summary:
        description: 'Summary of mvn verify failures'
        required: true
        type: string

permissions:
  contents: read
  actions: read

timeout-minutes: 60

network:
  allowed:
    - defaults
    - github

tools:
  github:
    toolsets: [context, repos]

safe-outputs:
  push-to-pull-request-branch:
    target: "*"
    labels: [dependencies]
  add-comment:
    target: "*"
    max: 5
  noop:
    report-as-issue: false
---
# Java Codegen Agentic Fix

You are an automation agent that fixes Java compilation and test failures caused by code generation changes in the `copilot-sdk` monorepo.

## Context

A Dependabot PR bumped the `@github/copilot` npm dependency in `java/scripts/codegen/package.json`. The `java-codegen-check` workflow ran the code generator (`java/scripts/codegen/java.ts`) against the new schemas and `mvn verify` subsequently failed. Your job is to fix **both** the code generator script (if needed) and the handwritten SDK/test source code so the build passes.

**❌❌❌ YOU MUST NEVER EDIT any of the java source code in `java/src/generated/` directly.** ✅✅Rather, the way to affect changes in these files is to change the code generator script and re-generate the classes in `java/src/generated`.

The branch to fix is: `${{ inputs.branch }}`
The PR number is: `${{ inputs.pr_number }}`

The error summary from the failing build is:
```
${{ inputs.error_summary }}
```

## Architecture overview

The code generator (`java/scripts/codegen/java.ts`) reads JSON schemas from `node_modules/@github/copilot/schemas/` and produces Java source files under `java/src/generated/java/`. These generated types are consumed by handwritten code in `java/src/main/java/` (primarily `CopilotSession.java`) and tested by handwritten tests in `java/src/test/java/`.

When `@github/copilot` is bumped, the schemas may change in ways the code generator does not yet handle. Common schema changes include:

- **`$ref` references**: Inline nested type definitions replaced with `$ref` pointers to `#/definitions/` entries. The code generator must resolve these references and emit standalone Java types instead of nested records.
- **Field type changes**: Numeric fields changing between `double`, `Long`, `int`, etc.
- **Renamed fields/properties**: JSON property names changing (e.g. `input` → `inputTokens`).
- **New types or events**: Entirely new schemas or event types added.
- **Structural changes**: Properties moving between objects, new required fields, changed enum values.

## Instructions

Follow these steps exactly. You have a maximum of **3 attempts** to get `mvn verify` passing.

### Step 0: Setup

Check out the branch and ensure the environment is ready:

```bash
git checkout "${{ inputs.branch }}"
git pull origin "${{ inputs.branch }}"
```

Set up the Java 17 environment and verify Maven and Node.js are available:

```bash
java -version
mvn --version
node --version
```

Install codegen dependencies:

```bash
cd java/scripts/codegen && npm ci && cd ../../..
```

### Step 1: Reproduce the failure

Run `mvn verify` from the `java/` directory to see the current errors:

```bash
cd java && mvn verify 2>&1 | tee /tmp/mvn-verify.log
```

Review the full log at `/tmp/mvn-verify.log` if the tail output is insufficient. The earliest errors are often the root cause.

If `mvn verify` succeeds (exit code 0), there is nothing to fix. Call the `noop` safe-output with message "mvn verify already passes on branch ${{ inputs.branch }}. No fixes needed." and stop.

### Step 2: Diagnose the root cause

Before making fixes, determine whether the failure is caused by:

**(A) The code generator not handling new schema patterns.** Signs:
- Generated types are missing fields that the handwritten code references
- Generated types have wrong field types (e.g. `double` instead of `Long`)
- Types that used to be nested records are now missing (because `$ref` moved them to `#/definitions/`)
- New schemas exist but no corresponding Java types were generated

**(B) Handwritten code referencing old generated type names/shapes.** Signs:
- Compilation errors in `java/src/main/java/` or `java/src/test/java/` referencing types that no longer exist
- Test data using old JSON field names

Often **both** (A) and (B) apply: the codegen needs fixing first, then handwritten code needs updating.

To diagnose, compare the current schemas with the generated output:

```bash
# List available schemas
ls java/scripts/codegen/node_modules/@github/copilot/schemas/

# Check for $ref usage in schemas (indicates the codegen may need $ref resolution)
grep -r '"$ref"' java/scripts/codegen/node_modules/@github/copilot/schemas/ | head -20

# Look at a specific schema that relates to failing types
cat java/scripts/codegen/node_modules/@github/copilot/schemas/<relevant-schema>.json | head -80
```

### Step 3: Fix the code generator (if needed)

If the diagnosis shows the code generator does not handle the new schema format:

1. **Read `java/scripts/codegen/java.ts`** to understand the current generation logic.

2. **Fix `java/scripts/codegen/java.ts`** to handle the new schema patterns. Common fixes include:
   - Adding `$ref` resolution to dereference `#/definitions/` pointers
   - Generating standalone types for definitions instead of nested records
   - Fixing type mappings for changed field types

3. **Re-run code generation** to produce updated generated files:
   ```bash
   cd java/scripts/codegen && npx tsx java.ts && cd ../../..
   ```

4. **Verify the generated output** looks reasonable:
   ```bash
   git diff --stat java/src/generated/java/
   ```

**You may ONLY modify `java/scripts/codegen/java.ts`.** Do not modify `package.json`, `package-lock.json`, or any other file under `java/scripts/codegen/`.

### Step 4: Fix handwritten code (up to 3 attempts)

For each attempt:

1. **Read the errors carefully.** Look for:
   - Compilation errors (missing methods, type mismatches, import issues)
   - Test failures (assertion errors, runtime exceptions)
   - The specific files and line numbers mentioned in the errors

2. **Read the generated types** to understand what changed. Check the generated files that the handwritten code references:
   ```bash
   # Example: check what a generated type looks like now
   cat java/src/generated/java/com/github/copilot/sdk/generated/rpc/<TypeName>.java
   ```

3. **Fix the affected source files.** You may modify files under:
   - `java/src/main/java/` — handwritten SDK source code
   - `java/src/test/java/` — handwritten test code

   Common fixes:
   - Update type references from old nested types to new standalone types (e.g. `SessionMcpListResultServersItem` → `McpServer`)
   - Fix constructor arguments for changed field types (`double` → `Long`)
   - Update JSON keys in test data to match renamed schema properties
   - Add/remove imports for renamed/relocated types

4. **Run formatting after making changes:**
   ```bash
   cd java && mvn spotless:apply
   ```

5. **Verify the fix:**
   ```bash
   cd java && mvn verify 2>&1 | tee /tmp/mvn-verify.log
   ```

   If the output is long, check `/tmp/mvn-verify.log` for the full error details — root causes often appear early in the log.

6. If `mvn verify` passes, proceed to Step 5.
   If it fails and you have attempts remaining, go back to sub-step 1.

### Step 5: Push fixes

After `mvn verify` passes, commit all changes and use the `push-to-pull-request-branch` safe-output tool to push to PR #${{ inputs.pr_number }}:

```bash
git add -A
git commit -m "Fix Java codegen and build failures after @github/copilot update

Automated fix applied by java-codegen-fix workflow."
```

Then call the `push-to-pull-request-branch` tool to push your commits to the PR branch.

### Step 6: Failure handling

If all 3 attempts fail:

1. Call the `add-comment` tool on PR #${{ inputs.pr_number }} explaining:
   - What errors remain
   - What fixes were attempted
   - Whether the issue is in the code generator or handwritten code
   - That manual intervention is needed

2. Call the `noop` safe-output with a message summarizing the failure.

Do **NOT** push broken code.

## Important constraints

- **NEVER** hand-edit files under `java/src/generated/java/` — these are auto-generated. They are updated by running `cd java/scripts/codegen && npx tsx java.ts`.
- **NEVER** modify `java/pom.xml` — build config is not in scope
- **NEVER** modify `java/scripts/codegen/package.json` or `java/scripts/codegen/package-lock.json` — dependency versions are not in scope
- **NEVER** modify files under `.github/` — workflow files are not in scope
- You **MAY** modify `java/scripts/codegen/java.ts` to fix the code generator
- You **MAY** modify files under `java/src/main/java/` and `java/src/test/java/` to fix handwritten code
- Always run `cd java && mvn spotless:apply` before committing to ensure code formatting
- Maximum 3 fix attempts before reporting failure via `noop`
- Only push if `mvn verify` passes
