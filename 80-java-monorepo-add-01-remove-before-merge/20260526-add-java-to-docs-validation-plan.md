# Plan: Add Java to docs-validation

## Context

Read the master plan at `80-java-monorepo-add-01-remove-before-merge/dd-2989727-move-java-to-monorepo-plan.md` (Phase 05, item 5) for overall context.

This plan implements **docs-validation support for Java code snippets** in the documentation. The existing infrastructure validates TypeScript, Python, Go, and C# — Java is the 5th language to add.

## Prerequisites

- The Java SDK Maven coordinates are `com.github:copilot-sdk-java` (after the repackage in PR #1437).
- The main package is `com.github.copilot` (not `com.github.copilot.sdk` — the rename happened in the repackage).
- JDK 17+ and Maven 3.9+ are required.
- Node.js is required for the extraction/validation scripts.

## Files to Modify

| File | Action |
|------|--------|
| `scripts/docs-validation/extract.ts` | Add `java` to `LANGUAGE_MAP`, add Java wrapping logic, add `"java"` extension, add `"java"` to language subdirectory creation |
| `scripts/docs-validation/validate.ts` | Add `validateJava()` function, register it in the `validators` array |
| `scripts/docs-validation/package.json` | Add `"validate:java"` script |
| `.github/workflows/docs-validation.yml` | Add `validate-java` job, add `java/src/**` to path triggers |
| Various `docs/**/*.md` files | Add `<!-- docs-validate: skip -->` directives above incomplete Java snippets |

---

## Step 1: Update `scripts/docs-validation/extract.ts`

### 1a. Add Java to `LANGUAGE_MAP`

In the `LANGUAGE_MAP` object (around line 14), add:

```typescript
  java: "java",
```

### 1b. Add Java file extension

In the `getExtension()` function, add a case:

```typescript
    case "java":
      return ".java";
```

### 1c. Add `"java"` to subdirectory creation

In the `main()` function where language subdirectories are created (the loop over `["typescript", "python", "go", "csharp"]`), add `"java"` to the array.

### 1d. Add Java wrapping logic in `wrapCodeForValidation()`

Add a Java wrapping block after the C# section. The logic should:

1. Check if the snippet already has a `class` declaration.
2. If it does NOT have a class:
   - Extract any existing `import` statements.
   - Add default imports if no SDK imports are present:
     ```java
     import com.github.copilot.*;
     import com.github.copilot.events.*;
     import java.util.*;
     import java.util.concurrent.*;
     ```
   - Generate a unique class name from `block.file` and `block.line` (sanitized to be a valid Java identifier — replace non-alphanumeric with `_`, prepend `Snippet_` if it starts with a digit).
   - Wrap the remaining code in:
     ```java
     public class <ClassName> {
         public static void main(String[] args) throws Exception {
             <code indented 8 spaces>
         }
     }
     ```
3. If it DOES have a class already:
   - Only add SDK imports if no `com.github.copilot` import is already present.

**Important:** The generated filename MUST match the public class name. Override `generateFileName()` behavior for Java: when the language is `"java"`, the filename must be `<ClassName>.java`. Do this by:
- After calling `wrapCodeForValidation()`, if the language is `"java"`, extract the class name from the wrapped code (regex: `/public class (\w+)/`) and use that as the filename.

### 1e. Add Java source comment format

In `getSourceComment()`, add `"java"` to the case that returns `// ${location}` (it already falls through to the default which does this, so no change may be needed — verify).

### 1f. Add Java fragment detection in `shouldSkipFragment()`

Add a Java case:

```typescript
  // Java: Skip interface definitions, annotations-only, or method signatures without bodies
  if (block.language === "java") {
    // Just an annotation
    if (/^@\w+/.test(code) && !code.includes("{")) {
      return true;
    }
    // Method signature without body
    if (/^(public|private|protected)?\s*(static\s+)?[\w<>\[\]]+\s+\w+\([^)]*\)\s*(throws\s+[\w,\s]+)?;\s*$/.test(code)) {
      return true;
    }
  }
```

---

## Step 2: Update `scripts/docs-validation/validate.ts`

### 2a. Add `validateJava()` function

Add this function after `validateCSharp()`. Follow the same pattern as the C# validator (project-level compilation):

```typescript
async function validateJava(): Promise<ValidationResult[]> {
  const results: ValidationResult[] = [];
  const javaDir = path.join(VALIDATION_DIR, "java");
  const manifest = loadManifest();

  if (!fs.existsSync(javaDir)) {
    console.log("  No Java files to validate");
    return results;
  }

  // Create a minimal Maven project structure
  const srcDir = path.join(javaDir, "src", "main", "java");
  fs.mkdirSync(srcDir, { recursive: true });

  // Move all .java files into src/main/java/
  const files = await glob("*.java", { cwd: javaDir });
  for (const file of files) {
    fs.renameSync(path.join(javaDir, file), path.join(srcDir, file));
  }

  // Create pom.xml that references the local SDK
  const pomXml = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>docs</groupId>
    <artifactId>docs-validation-java</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.github</groupId>
            <artifactId>copilot-sdk-java</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>`;

  fs.writeFileSync(path.join(javaDir, "pom.xml"), pomXml);

  // First, install the local SDK into the local Maven repo
  try {
    execFileSync("mvn", [
      "install",
      "-f", path.join(ROOT_DIR, "java", "pom.xml"),
      "-DskipTests",
      "-q",
    ], {
      encoding: "utf-8",
      cwd: path.join(ROOT_DIR, "java"),
    });
  } catch (err: any) {
    // If SDK install fails, all Java snippets fail
    const errorMsg = `SDK install failed: ${(err.stderr || err.message || "").slice(0, 200)}`;
    for (const file of files) {
      const block = manifest.blocks.find((b) => b.outputFile === `java/${file}`);
      results.push({
        file: `java/${file}`,
        sourceFile: block?.sourceFile || "unknown",
        sourceLine: block?.sourceLine || 0,
        success: false,
        errors: [errorMsg],
      });
    }
    return results;
  }

  // Compile the validation project
  try {
    execFileSync("mvn", ["compile", "-f", path.join(javaDir, "pom.xml"), "-q"], {
      encoding: "utf-8",
      cwd: javaDir,
    });

    // All files passed
    for (const file of files) {
      const block = manifest.blocks.find((b) => b.outputFile === `java/${file}`);
      results.push({
        file: `java/${file}`,
        sourceFile: block?.sourceFile || "unknown",
        sourceLine: block?.sourceLine || 0,
        success: true,
        errors: [],
      });
    }
  } catch (err: any) {
    const output = err.stdout || err.stderr || err.message || "";

    // Parse javac errors from Maven output
    // Format: [ERROR] /path/to/File.java:[line,col] error: message
    const fileErrors = new Map<string, string[]>();

    for (const line of output.split("\n")) {
      const match = line.match(/\[ERROR\]\s+.*[/\\]([^/\\]+\.java):\[(\d+),(\d+)\]\s*(.*)/);
      if (match) {
        const fileName = match[1];
        if (!fileErrors.has(fileName)) {
          fileErrors.set(fileName, []);
        }
        fileErrors.get(fileName)!.push(`${fileName}:${match[2]}: ${match[4]}`);
      }
    }

    for (const file of files) {
      const block = manifest.blocks.find((b) => b.outputFile === `java/${file}`);
      const errors = fileErrors.get(file) || [];

      results.push({
        file: `java/${file}`,
        sourceFile: block?.sourceFile || "unknown",
        sourceLine: block?.sourceLine || 0,
        success: errors.length === 0,
        errors,
      });
    }
  }

  return results;
}
```

### 2b. Register the Java validator

In the `validators` array (around line 440), add:

```typescript
    ["Java", validateJava],
```

The `langKey` for Java will be `"java"` (since `"Java".toLowerCase()` is `"java"`), which matches the `--lang java` flag.

---

## Step 3: Update `scripts/docs-validation/package.json`

Add to the `"scripts"` object:

```json
"validate:java": "tsx validate.ts --lang java"
```

---

## Step 4: Update `.github/workflows/docs-validation.yml`

### 4a. Add `java/src/**` to the path triggers

In the `on.pull_request.paths` array, add:

```yaml
      - 'java/src/**'
```

### 4b. Add `validate-java` job

Add this job after the `validate-csharp` job, following the same pattern:

```yaml
  validate-java:
    name: "Validate Java"
    if: github.event.repository.fork == false
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6

      - uses: actions/setup-node@v6
        with:
          node-version: 22

      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Install SDK to local repo
        working-directory: java
        run: mvn install -DskipTests -q

      - name: Install validation dependencies
        working-directory: scripts/docs-validation
        run: npm ci

      - name: Extract and validate Java
        working-directory: scripts/docs-validation
        run: npm run extract && npm run validate:java
```

---

## Step 5: Add `<!-- docs-validate: skip -->` directives

Audit every Java code block in the docs (there are ~48 across ~21 files). For each block, determine if it can compile as a standalone snippet after wrapping. Mark incomplete fragments with `<!-- docs-validate: skip -->` on the line immediately before the opening ` ```java `.

### Criteria for skipping

A block needs `<!-- docs-validate: skip -->` if:
- It's a **partial snippet** showing only a method body fragment, not a complete statement sequence
- It references **variables or types defined elsewhere** that aren't in the SDK's public API
- It's a **configuration-only** block (e.g., Maven XML, properties)
- It's **pseudocode** or illustrative (uses `...` or `// ...` to indicate omitted code that would cause compilation failure)
- It uses `<!-- docs-validate: hidden -->` / `<!-- /docs-validate: hidden -->` pattern instead (the hidden block is the compilable version; the visible one is auto-skipped)

### Criteria for NOT skipping (should compile)

- Complete `main()` method body that uses SDK classes
- Complete class definition with all imports
- Code that only needs the standard wrapping (imports + class + main) to compile

### Files to audit

Run this to find all Java blocks:
```bash
grep -rn '```java' docs/ | cut -d: -f1 | sort -u
```

For each file, read the Java blocks and determine if they need skip directives. When in doubt, add the skip — it's better to have a passing CI that skips some blocks than a failing one.

---

## Step 6: Validate locally

After all changes, run:

```bash
cd scripts/docs-validation
npm run extract
npm run validate:java
```

Fix any compilation errors by either:
1. Fixing the doc snippet to be compilable
2. Adding `<!-- docs-validate: skip -->` above the block
3. Using the hidden block pattern for a compilable version

---

## Acceptance Criteria

- [ ] `npm run extract` recognizes and extracts Java blocks from docs
- [ ] `npm run validate:java` compiles all non-skipped Java blocks successfully
- [ ] The `validate-java` CI job passes on a PR
- [ ] No existing CI jobs are broken
- [ ] The Java SDK `mvn install -DskipTests` succeeds (prerequisite for validation)

## Notes for the implementing agent

- The Java package is `com.github.copilot` (NOT `com.github.copilot.sdk`) — use the new package names in imports.
- Generated types are under `com.github.copilot.generated` and `com.github.copilot.generated.rpc`.
- The SDK's main public classes include: `CopilotClient`, `CopilotClientOptions`, `CopilotTool`, `CopilotSession`, etc.
- Run `mvn install -DskipTests -q` in the `java/` directory before running validation locally.
- The `validate.ts` pattern for C# is the closest analogue — it does project-level compilation via `dotnet build`. Java does the same via `mvn compile`.
- Keep the error-parsing regex for Maven output flexible — Maven prefixes errors with `[ERROR]`.
