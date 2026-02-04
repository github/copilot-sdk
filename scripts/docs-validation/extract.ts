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
        wrapAsync: wrapAsync,
      });
      inCodeBlock = false;
      currentLang = "";
      currentCode = [];
      skipNext = false;
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

function wrapCodeForValidation(block: CodeBlock): string {
  let code = block.code;

  // Python: wrap in async main if needed
  if (block.language === "python" && block.wrapAsync) {
    const indented = code
      .split("\n")
      .map((l) => "    " + l)
      .join("\n");
    code = `import asyncio\n\nasync def main():\n${indented}\n\nasyncio.run(main())`;
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

  // C#: wrap in minimal structure if needed
  if (block.language === "csharp") {
    // Check if it's a complete file (has namespace or class)
    const hasStructure =
      code.includes("namespace ") ||
      code.includes("class ") ||
      code.includes("record ");
    if (!hasStructure) {
      // Top-level statements are fine in modern C#, just ensure usings are at top
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

  for (const mdFile of mdFiles) {
    const fullPath = path.join(DOCS_DIR, mdFile);
    const content = fs.readFileSync(fullPath, "utf-8");
    const blocks = parseMarkdownCodeBlocks(content, mdFile);

    for (const block of blocks) {
      if (block.skip) {
        skippedBlocks++;
        continue;
      }

      // Skip empty or trivial blocks
      if (block.code.trim().length < 10) {
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
