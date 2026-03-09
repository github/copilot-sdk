/**
 * Extracts code blocks from markdown documentation files.
 * Outputs individual files for validation by language-specific tools.
 */

import * as fs from "fs";
import * as path from "path";
import { glob } from "glob";

const DOCS_DIR = path.resolve(import.meta.dirname, "../../docs");
const OUTPUT_DIR = path.resolve(import.meta.dirname, "../../docs/.validation");

// Map markdown language tags to our canonical names
const LANGUAGE_MAP: Record<string, string> = {
  typescript: "typescript",
  ts: "typescript",
  javascript: "typescript", // Treat JS as TS for validation
  js: "typescript",
  python: "python",
  py: "python",
  go: "go",
  golang: "go",
  csharp: "csharp",
  "c#": "csharp",
  cs: "csharp",
};

interface CodeBlock {
  language: string;
  code: string;
  file: string;
  line: number;
  skip: boolean;
  hidden: boolean;
  wrapAsync: boolean;
}

interface ExtractionManifest {
  extractedAt: string;
  blocks: {
    id: string;
    sourceFile: string;
    sourceLine: number;
    language: string;
    outputFile: string;
  }[];
}

function parseMarkdownCodeBlocks(
  content: string,
  filePath: string
): CodeBlock[] {
  const blocks: CodeBlock[] = [];
  const lines = content.split("\n");

  let inCodeBlock = false;
  let currentLang = "";
  let currentCode: string[] = [];
  let blockStartLine = 0;
  let skipNext = false;
  let wrapAsync = false;
  let inHiddenBlock = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Check for validation directives
    if (line.includes("<!-- docs-validate: skip -->")) {
      skipNext = true;
      continue;
    }
    if (line.includes("<!-- docs-validate: wrap-async -->")) {
      wrapAsync = true;
      continue;
    }
    if (line.includes("<!-- docs-validate: hidden -->")) {
      inHiddenBlock = true;
      continue;
    }
    if (line.includes("<!-- /docs-validate: hidden -->")) {
      inHiddenBlock = false;
      // Skip the next visible code block since the hidden one replaces it
      skipNext = true;
      continue;
    }

    // Start of code block
    if (!inCodeBlock && line.startsWith("```")) {
      const lang = line.slice(3).trim().toLowerCase();
      if (lang && LANGUAGE_MAP[lang]) {
        inCodeBlock = true;
        currentLang = LANGUAGE_MAP[lang];
        currentCode = [];
        blockStartLine = i + 1; // 1-indexed line number
      }
      continue;
    }

    // End of code block
    if (inCodeBlock && line.startsWith("```")) {
      blocks.push({
        language: currentLang,
        code: currentCode.join("\n"),
        file: filePath,
        line: blockStartLine,
        skip: skipNext,
        hidden: inHiddenBlock,
        wrapAsync: wrapAsync,
      });
      inCodeBlock = false;
      currentLang = "";
      currentCode = [];
      // Only reset skipNext when NOT in a hidden block — hidden blocks
      // can contain multiple code fences that all get validated.
      if (!inHiddenBlock) {
        skipNext = false;
      }
      wrapAsync = false;
      continue;
    }

    // Inside code block
    if (inCodeBlock) {
      currentCode.push(line);
    }
  }

  return blocks;
}

function generateFileName(
  block: CodeBlock,
  index: number,
  langCounts: Map<string, number>
): string {
  const count = langCounts.get(block.language) || 0;
  langCounts.set(block.language, count + 1);

  const sourceBasename = path.basename(block.file, ".md");
  const ext = getExtension(block.language);

  return `${sourceBasename}_${count}${ext}`;
}

function getExtension(language: string): string {
  switch (language) {
    case "typescript":
      return ".ts";
    case "python":
      return ".py";
    case "go":
      return ".go";
    case "csharp":
      return ".cs";
    default:
      return ".txt";
  }
}

/**
 * Detect code fragments that can't be validated as standalone files.
 * These are typically partial snippets showing configuration options
 * or code that's meant to be part of a larger context.
 */
function shouldSkipFragment(block: CodeBlock): boolean {
  const code = block.code.trim();

  // TypeScript/JavaScript: Skip bare object literals (config snippets)
  if (block.language === "typescript") {
    // Starts with property: value pattern (e.g., "provider: {")
    if (/^[a-zA-Z_]+\s*:\s*[\{\[]/.test(code)) {
      return true;
    }
    // Starts with just an object/array that's not assigned
    if (/^\{[\s\S]*\}$/.test(code) && !code.includes("import ") && !code.includes("export ")) {
      return true;
    }
  }

  // Go: Skip fragments that are just type definitions without package
  if (block.language === "go") {
    // Function signatures without bodies (interface definitions shown in docs)
    if (/^func\s+\w+\([^)]*\)\s*\([^)]*\)\s*$/.test(code)) {
      return true;
    }
  }

  return false;
}

function wrapCodeForValidation(block: CodeBlock): string {
  let code = block.code;

  // Python: auto-detect async code and wrap if needed
  if (block.language === "python") {
    const hasAwait = /\bawait\b/.test(code);
    const hasAsyncDef = /\basync\s+def\b/.test(code);

    // Check if await is used outside of any async def
    // Simple heuristic: if await appears at column 0 or after assignment at column 0
    const lines = code.split("\n");
    let awaitOutsideFunction = false;
    let inAsyncFunction = false;
    let indentLevel = 0;

    for (const line of lines) {
      const trimmed = line.trimStart();
      const leadingSpaces = line.length - trimmed.length;

      // Track if we're in an async function
      if (trimmed.startsWith("async def ")) {
        inAsyncFunction = true;
        indentLevel = leadingSpaces;
      } else if (inAsyncFunction && leadingSpaces <= indentLevel && trimmed && !trimmed.startsWith("#")) {
        // Dedented back, we're out of the function
        inAsyncFunction = false;
      }

      // Check for await outside function
      if (trimmed.includes("await ") && !inAsyncFunction) {
        awaitOutsideFunction = true;
        break;
      }
    }

    const needsWrap = block.wrapAsync || awaitOutsideFunction || (hasAwait && !hasAsyncDef);

    if (needsWrap) {
      const indented = code
        .split("\n")
        .map((l) => "    " + l)
        .join("\n");
      code = `import asyncio\n\nasync def main():\n${indented}\n\nasyncio.run(main())`;
    }
  }

  // Go: ensure package declaration
  if (block.language === "go" && !code.includes("package ")) {
    code = `package main\n\n${code}`;
  }

  // Go: add main function if missing and has statements outside functions
  if (block.language === "go" && !code.includes("func main()")) {
    // Check if code has statements that need to be in main
    const hasStatements = /^[a-z]/.test(code.trim().split("\n").pop() || "");
    if (hasStatements) {
      // This is a snippet, wrap it
      const lines = code.split("\n");
      const packageLine = lines.find((l) => l.startsWith("package ")) || "";
      const imports = lines.filter(
        (l) => l.startsWith("import ") || l.startsWith('import (')
      );
      const rest = lines.filter(
        (l) =>
          !l.startsWith("package ") &&
          !l.startsWith("import ") &&
          !l.startsWith("import (") &&
          !l.startsWith(")") &&
          !l.startsWith("\t") // import block lines
      );

      // Only wrap if there are loose statements (not type/func definitions)
      const hasLooseStatements = rest.some(
        (l) =>
          l.trim() &&
          !l.startsWith("type ") &&
          !l.startsWith("func ") &&
          !l.startsWith("//") &&
          !l.startsWith("var ") &&
          !l.startsWith("const ")
      );

      if (!hasLooseStatements) {
        // Code has proper structure, just ensure it has a main
        code = code + "\n\nfunc main() {}";
      }
    }
  }

  // C#: wrap in a class to avoid top-level statements conflicts
  // (C# only allows one file with top-level statements per project)
  if (block.language === "csharp") {
    // Check if it's a complete file (has namespace or class)
    const hasStructure =
      code.includes("namespace ") ||
      code.includes("class ") ||
      code.includes("record ") ||
      code.includes("public delegate ");

    if (!hasStructure) {
      // Extract any existing using statements
      const lines = code.split("\n");
      const usings: string[] = [];
      const rest: string[] = [];

      for (const line of lines) {
        if (line.trim().startsWith("using ") && line.trim().endsWith(";")) {
          usings.push(line);
        } else {
          rest.push(line);
        }
      }

      // Always ensure SDK using is present
      if (!usings.some(u => u.includes("GitHub.Copilot.SDK"))) {
        usings.push("using GitHub.Copilot.SDK;");
      }

      // Generate a unique class name based on block location
      const className = `ValidationClass_${block.file.replace(/[^a-zA-Z0-9]/g, "_")}_${block.line}`;

      // Wrap in async method to support await
      const hasAwait = code.includes("await ");
      const indentedCode = rest.map(l => "        " + l).join("\n");

      if (hasAwait) {
        code = `${usings.join("\n")}

public static class ${className}
{
    public static async Task Main()
    {
${indentedCode}
    }
}`;
      } else {
        code = `${usings.join("\n")}

public static class ${className}
{
    public static void Main()
    {
${indentedCode}
    }
}`;
      }
    } else {
      // Has structure, but may still need using directive
      if (!code.includes("using GitHub.Copilot.SDK;")) {
        code = "using GitHub.Copilot.SDK;\n" + code;
      }
    }
  }

  return code;
}

async function main() {
  console.log("📖 Extracting code blocks from documentation...\n");

  // Clean output directory
  if (fs.existsSync(OUTPUT_DIR)) {
    fs.rmSync(OUTPUT_DIR, { recursive: true });
  }
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });

  // Create language subdirectories
  for (const lang of ["typescript", "python", "go", "csharp"]) {
    fs.mkdirSync(path.join(OUTPUT_DIR, lang), { recursive: true });
  }

  // Find all markdown files
  const mdFiles = await glob("**/*.md", {
    cwd: DOCS_DIR,
    ignore: [".validation/**", "node_modules/**", "IMPROVEMENT_PLAN.md"],
  });

  console.log(`Found ${mdFiles.length} markdown files\n`);

  const manifest: ExtractionManifest = {
    extractedAt: new Date().toISOString(),
    blocks: [],
  };

  const langCounts = new Map<string, number>();
  let totalBlocks = 0;
  let skippedBlocks = 0;
  let hiddenBlocks = 0;

  for (const mdFile of mdFiles) {
    const fullPath = path.join(DOCS_DIR, mdFile);
    const content = fs.readFileSync(fullPath, "utf-8");
    const blocks = parseMarkdownCodeBlocks(content, mdFile);

    for (const block of blocks) {
      if (block.skip) {
        skippedBlocks++;
        continue;
      }

      if (block.hidden) {
        hiddenBlocks++;
      }

      // Skip empty or trivial blocks
      if (block.code.trim().length < 10) {
        continue;
      }

      // Skip incomplete code fragments that can't be validated standalone
      if (shouldSkipFragment(block)) {
        skippedBlocks++;
        continue;
      }

      const fileName = generateFileName(block, totalBlocks, langCounts);
      const outputPath = path.join(OUTPUT_DIR, block.language, fileName);

      const wrappedCode = wrapCodeForValidation(block);

      // Add source location comment
      const sourceComment = getSourceComment(
        block.language,
        block.file,
        block.line
      );
      const finalCode = sourceComment + "\n" + wrappedCode;

      fs.writeFileSync(outputPath, finalCode);

      manifest.blocks.push({
        id: `${block.language}/${fileName}`,
        sourceFile: block.file,
        sourceLine: block.line,
        language: block.language,
        outputFile: `${block.language}/${fileName}`,
      });

      totalBlocks++;
    }
  }

  // Write manifest
  fs.writeFileSync(
    path.join(OUTPUT_DIR, "manifest.json"),
    JSON.stringify(manifest, null, 2)
  );

  // Summary
  console.log("Extraction complete!\n");
  console.log("  Language       Count");
  console.log("  ─────────────────────");
  for (const [lang, count] of langCounts) {
    console.log(`  ${lang.padEnd(14)} ${count}`);
  }
  console.log("  ─────────────────────");
  console.log(`  Total          ${totalBlocks}`);
  if (skippedBlocks > 0) {
    console.log(`  Skipped        ${skippedBlocks}`);
  }
  if (hiddenBlocks > 0) {
    console.log(`  Hidden         ${hiddenBlocks}`);
  }
  console.log(`\nOutput: ${OUTPUT_DIR}`);
}

function getSourceComment(
  language: string,
  file: string,
  line: number
): string {
  const location = `Source: ${file}:${line}`;
  switch (language) {
    case "typescript":
    case "go":
    case "csharp":
      return `// ${location}`;
    case "python":
      return `# ${location}`;
    default:
      return `// ${location}`;
  }
}

main().catch((err) => {
  console.error("Extraction failed:", err);
  process.exit(1);
});
