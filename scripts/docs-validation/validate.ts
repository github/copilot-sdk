/**
 * Validates extracted documentation code blocks.
 * Runs language-specific type/compile checks.
 */

import * as fs from "fs";
import * as path from "path";
import { execFileSync } from "child_process";
import { glob } from "glob";

const ROOT_DIR = path.resolve(import.meta.dirname, "../..");
const VALIDATION_DIR = path.join(ROOT_DIR, "docs/.validation");

interface ValidationResult {
  file: string;
  sourceFile: string;
  sourceLine: number;
  success: boolean;
  errors: string[];
}

interface Manifest {
  blocks: {
    id: string;
    sourceFile: string;
    sourceLine: number;
    language: string;
    outputFile: string;
  }[];
}

function loadManifest(): Manifest {
  const manifestPath = path.join(VALIDATION_DIR, "manifest.json");
  if (!fs.existsSync(manifestPath)) {
    console.error(
      "❌ No manifest found. Run extraction first: npm run extract"
    );
    process.exit(1);
  }
  return JSON.parse(fs.readFileSync(manifestPath, "utf-8"));
}

async function validateTypeScript(): Promise<ValidationResult[]> {
  const results: ValidationResult[] = [];
  const tsDir = path.join(VALIDATION_DIR, "typescript");
  const manifest = loadManifest();

  if (!fs.existsSync(tsDir)) {
    console.log("  No TypeScript files to validate");
    return results;
  }

  // Create a temporary tsconfig for validation
  const tsconfig = {
    compilerOptions: {
      target: "ES2022",
      module: "NodeNext",
      moduleResolution: "NodeNext",
      strict: true,
      skipLibCheck: true,
      noEmit: true,
      esModuleInterop: true,
      allowSyntheticDefaultImports: true,
      resolveJsonModule: true,
      types: ["node"],
      paths: {
        "@github/copilot-sdk": [path.join(ROOT_DIR, "nodejs/src/index.ts")],
      },
    },
    include: ["./**/*.ts"],
  };

  const tsconfigPath = path.join(tsDir, "tsconfig.json");
  fs.writeFileSync(tsconfigPath, JSON.stringify(tsconfig, null, 2));

  try {
    // Run tsc
    const tscPath = path.join(ROOT_DIR, "nodejs/node_modules/.bin/tsc");
    execFileSync(tscPath, ["--project", tsconfigPath], {
      encoding: "utf-8",
      cwd: tsDir,
    });

    // All files passed
    const files = await glob("*.ts", { cwd: tsDir });
    for (const file of files) {
      if (file === "tsconfig.json") continue;
      const block = manifest.blocks.find(
        (b) => b.outputFile === `typescript/${file}`
      );
      results.push({
        file: `typescript/${file}`,
        sourceFile: block?.sourceFile || "unknown",
        sourceLine: block?.sourceLine || 0,
        success: true,
        errors: [],
      });
    }
  } catch (err: any) {
    // Parse tsc output for errors
    const output = err.stdout || err.stderr || err.message || "";
    const errorLines = output.split("\n");
    const fileErrors = new Map<string, string[]>();
    let currentFile = "";

    for (const line of errorLines) {
      const match = line.match(/^(.+\.ts)\((\d+),(\d+)\): error/);
      if (match) {
        currentFile = match[1];
        if (!fileErrors.has(currentFile)) {
          fileErrors.set(currentFile, []);
        }
        fileErrors.get(currentFile)!.push(line);
      } else if (currentFile && line.trim()) {
        fileErrors.get(currentFile)?.push(line);
      }
    }

    // Create results
    const files = await glob("*.ts", { cwd: tsDir });
    for (const file of files) {
      if (file === "tsconfig.json") continue;
      const fullPath = path.join(tsDir, file);
      const block = manifest.blocks.find(
        (b) => b.outputFile === `typescript/${file}`
      );
      const errors = fileErrors.get(fullPath) || fileErrors.get(file) || [];

      results.push({
        file: `typescript/${file}`,
        sourceFile: block?.sourceFile || "unknown",
        sourceLine: block?.sourceLine || 0,
        success: errors.length === 0,
        errors,
      });
    }
  }

  return results;
}

async function validatePython(): Promise<ValidationResult[]> {
  const results: ValidationResult[] = [];
  const pyDir = path.join(VALIDATION_DIR, "python");
  const manifest = loadManifest();

  if (!fs.existsSync(pyDir)) {
    console.log("  No Python files to validate");
    return results;
  }

  const files = await glob("*.py", { cwd: pyDir });

  for (const file of files) {
    const fullPath = path.join(pyDir, file);
    const block = manifest.blocks.find(
      (b) => b.outputFile === `python/${file}`
    );
    const errors: string[] = [];

    // Syntax check with py_compile
    try {
      execFileSync("python3", ["-m", "py_compile", fullPath], {
        encoding: "utf-8",
      });
    } catch (err: any) {
      errors.push(err.stdout || err.stderr || err.message || "Syntax error");
    }

    // Type check with mypy (if available)
    if (errors.length === 0) {
      try {
        execFileSync(
          "python3",
          ["-m", "mypy", fullPath, "--ignore-missing-imports", "--no-error-summary"],
          { encoding: "utf-8" }
        );
      } catch (err: any) {
        const output = err.stdout || err.stderr || err.message || "";
        // Filter out "Success" messages and notes
        const typeErrors = output
          .split("\n")
          .filter(
            (l: string) =>
              l.includes(": error:") &&
              !l.includes("Cannot find implementation")
          );
        if (typeErrors.length > 0) {
          errors.push(...typeErrors);
        }
      }
    }

    results.push({
      file: `python/${file}`,
      sourceFile: block?.sourceFile || "unknown",
      sourceLine: block?.sourceLine || 0,
      success: errors.length === 0,
      errors,
    });
  }

  return results;
}

async function validateGo(): Promise<ValidationResult[]> {
  const results: ValidationResult[] = [];
  const goDir = path.join(VALIDATION_DIR, "go");
  const manifest = loadManifest();

  if (!fs.existsSync(goDir)) {
    console.log("  No Go files to validate");
    return results;
  }

  // Create a go.mod for the validation directory
  const goMod = `module docs-validation

go 1.21

require github.com/github/copilot-sdk/go v0.0.0

replace github.com/github/copilot-sdk/go => ${path.join(ROOT_DIR, "go")}
`;
  fs.writeFileSync(path.join(goDir, "go.mod"), goMod);

  // Run go mod tidy to fetch dependencies
  try {
    execFileSync("go", ["mod", "tidy"], {
      encoding: "utf-8",
      cwd: goDir,
      env: { ...process.env, GO111MODULE: "on" },
    });
  } catch (err: any) {
    // go mod tidy might fail if there are syntax errors, continue anyway
  }

  const files = await glob("*.go", { cwd: goDir });

  // Try to compile each file individually
  for (const file of files) {
    const fullPath = path.join(goDir, file);
    const block = manifest.blocks.find((b) => b.outputFile === `go/${file}`);
    const errors: string[] = [];

    try {
      // Use go vet for syntax and basic checks
      execFileSync("go", ["build", "-o", "/dev/null", fullPath], {
        encoding: "utf-8",
        cwd: goDir,
        env: { ...process.env, GO111MODULE: "on" },
      });
    } catch (err: any) {
      const output = err.stdout || err.stderr || err.message || "";
      errors.push(
        ...output.split("\n").filter((l: string) => l.trim() && !l.startsWith("#"))
      );
    }

    results.push({
      file: `go/${file}`,
      sourceFile: block?.sourceFile || "unknown",
      sourceLine: block?.sourceLine || 0,
      success: errors.length === 0,
      errors,
    });
  }

  return results;
}

async function validateCSharp(): Promise<ValidationResult[]> {
  const results: ValidationResult[] = [];
  const csDir = path.join(VALIDATION_DIR, "csharp");
  const manifest = loadManifest();

  if (!fs.existsSync(csDir)) {
    console.log("  No C# files to validate");
    return results;
  }

  // Create a minimal csproj for validation
  const csproj = `<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>Library</OutputType>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
    <NoWarn>CS8019;CS0168;CS0219</NoWarn>
  </PropertyGroup>
  <ItemGroup>
    <ProjectReference Include="${path.join(ROOT_DIR, "dotnet/src/GitHub.Copilot.SDK.csproj")}" />
  </ItemGroup>
</Project>`;

  fs.writeFileSync(path.join(csDir, "DocsValidation.csproj"), csproj);

  const files = await glob("*.cs", { cwd: csDir });

  // Compile all files together
  try {
    execFileSync("dotnet", ["build", path.join(csDir, "DocsValidation.csproj")], {
      encoding: "utf-8",
      cwd: csDir,
    });

    // All files passed
    for (const file of files) {
      const block = manifest.blocks.find(
        (b) => b.outputFile === `csharp/${file}`
      );
      results.push({
        file: `csharp/${file}`,
        sourceFile: block?.sourceFile || "unknown",
        sourceLine: block?.sourceLine || 0,
        success: true,
        errors: [],
      });
    }
  } catch (err: any) {
    const output = err.stdout || err.stderr || err.message || "";

    // Parse errors by file
    const fileErrors = new Map<string, string[]>();

    for (const line of output.split("\n")) {
      const match = line.match(/([^/\\]+\.cs)\((\d+),(\d+)\): error/);
      if (match) {
        const fileName = match[1];
        if (!fileErrors.has(fileName)) {
          fileErrors.set(fileName, []);
        }
        fileErrors.get(fileName)!.push(line);
      }
    }

    for (const file of files) {
      const block = manifest.blocks.find(
        (b) => b.outputFile === `csharp/${file}`
      );
      const errors = fileErrors.get(file) || [];

      results.push({
        file: `csharp/${file}`,
        sourceFile: block?.sourceFile || "unknown",
        sourceLine: block?.sourceLine || 0,
        success: errors.length === 0,
        errors,
      });
    }
  }

  return results;
}

function printResults(results: ValidationResult[], language: string): { failed: number; passed: number; failures: ValidationResult[] } {
  const failed = results.filter((r) => !r.success);
  const passed = results.filter((r) => r.success);

  if (failed.length === 0) {
    console.log(`  ✅ ${passed.length} files passed`);
    return { failed: 0, passed: passed.length, failures: [] };
  }

  console.log(`  ❌ ${failed.length} failed, ${passed.length} passed\n`);

  for (const result of failed) {
    console.log(`  ┌─ ${result.sourceFile}:${result.sourceLine}`);
    console.log(`  │  Extracted to: ${result.file}`);
    for (const error of result.errors.slice(0, 5)) {
      console.log(`  │  ${error}`);
    }
    if (result.errors.length > 5) {
      console.log(`  │  ... and ${result.errors.length - 5} more errors`);
    }
    console.log(`  └─`);
  }

  return { failed: failed.length, passed: passed.length, failures: failed };
}

function writeGitHubSummary(summaryData: { language: string; passed: number; failed: number; failures: ValidationResult[] }[]) {
  const summaryFile = process.env.GITHUB_STEP_SUMMARY;
  if (!summaryFile) return;

  const totalPassed = summaryData.reduce((sum, d) => sum + d.passed, 0);
  const totalFailed = summaryData.reduce((sum, d) => sum + d.failed, 0);
  const allPassed = totalFailed === 0;

  let summary = `## 📖 Documentation Validation Results\n\n`;

  if (allPassed) {
    summary += `✅ **All ${totalPassed} code blocks passed validation**\n\n`;
  } else {
    summary += `❌ **${totalFailed} failures** out of ${totalPassed + totalFailed} code blocks\n\n`;
  }

  summary += `| Language | Status | Passed | Failed |\n`;
  summary += `|----------|--------|--------|--------|\n`;

  for (const { language, passed, failed } of summaryData) {
    const status = failed === 0 ? "✅" : "❌";
    summary += `| ${language} | ${status} | ${passed} | ${failed} |\n`;
  }

  if (totalFailed > 0) {
    summary += `\n### Failures\n\n`;
    for (const { language, failures } of summaryData) {
      if (failures.length === 0) continue;
      summary += `#### ${language}\n\n`;
      for (const f of failures) {
        summary += `- **${f.sourceFile}:${f.sourceLine}**\n`;
        summary += `  \`\`\`\n  ${f.errors.slice(0, 3).join("\n  ")}\n  \`\`\`\n`;
      }
    }
  }

  fs.appendFileSync(summaryFile, summary);
}

async function main() {
  const args = process.argv.slice(2);
  const langArg = args.find((a) => a.startsWith("--lang="));
  const targetLang = langArg?.split("=")[1];

  console.log("🔍 Validating documentation code blocks...\n");

  if (!fs.existsSync(VALIDATION_DIR)) {
    console.error("❌ No extracted code found. Run extraction first:");
    console.error("   npm run extract");
    process.exit(1);
  }

  let totalFailed = 0;
  const summaryData: { language: string; passed: number; failed: number; failures: ValidationResult[] }[] = [];

  const validators: [string, () => Promise<ValidationResult[]>][] = [
    ["TypeScript", validateTypeScript],
    ["Python", validatePython],
    ["Go", validateGo],
    ["C#", validateCSharp],
  ];

  for (const [name, validator] of validators) {
    const langKey = name.toLowerCase().replace("#", "sharp");
    if (targetLang && langKey !== targetLang) continue;

    console.log(`\n${name}:`);
    const results = await validator();
    const { failed, passed, failures } = printResults(results, name);
    totalFailed += failed;
    summaryData.push({ language: name, passed, failed, failures });
  }

  // Write GitHub Actions summary
  writeGitHubSummary(summaryData);

  console.log("\n" + "─".repeat(40));

  if (totalFailed > 0) {
    console.log(`\n❌ Validation failed: ${totalFailed} file(s) have errors`);
    console.log("\nTo fix:");
    console.log("  1. Check the error messages above");
    console.log("  2. Update the code blocks in the markdown files");
    console.log("  3. Re-run: npm run validate");
    console.log("\nTo skip a code block, add before it:");
    console.log("  <!-- docs-validate: skip -->");
    process.exit(1);
  }

  console.log("\n✅ All documentation code blocks are valid!");
}

main().catch((err) => {
  console.error("Validation failed:", err);
  process.exit(1);
});
