#!/usr/bin/env node

/**
 * Normalizes Copilot SDK docs for publishing on docs.github.com.
 *
 * For every .md file in the SDK docs directory, this script:
 *   - Adds YAML frontmatter (title, intro, shortTitle, versions, contentType)
 *   - Adds `children` arrays to index.md files
 *   - Rewrites internal relative .md links to [AUTOTITLE](/path) format
 *   - Rewrites absolute docs.github.com links to [AUTOTITLE](/path) format
 *   - Creates missing index.md files for subdirectories
 *   - Fixes code fence language aliases (go → golang, ts → typescript)
 *   - Normalizes ordered list prefixes to 1.
 *
 * Adapted from the spike normalization script in docs-internal#60525.
 *
 * Usage:
 *   node normalize-sdk-docs.mjs --content-dir <path> --sdk-docs-dir <path>
 */

import fs from "node:fs";
import path from "node:path";
import { parseArgs } from "node:util";
import matter from "gray-matter";

// Parse CLI arguments
const { values: args } = parseArgs({
  options: {
    "content-dir": { type: "string" },
    "sdk-docs-dir": { type: "string" },
  },
});

const CONTENT_DIR = path.resolve(args["content-dir"]);
const SDK_DOCS_DIR = path.resolve(args["sdk-docs-dir"]);

if (!fs.existsSync(CONTENT_DIR)) {
  console.error(`Content directory not found: ${CONTENT_DIR}`);
  process.exit(1);
}
if (!fs.existsSync(SDK_DOCS_DIR)) {
  console.error(`SDK docs directory not found: ${SDK_DOCS_DIR}`);
  process.exit(1);
}

// --- Helpers ---

/** Recursively collect all .md files in a directory. */
function getAllMarkdownFiles(dir) {
  const results = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...getAllMarkdownFiles(fullPath));
    } else if (entry.isFile() && entry.name.endsWith(".md")) {
      results.push(fullPath);
    }
  }
  return results;
}

/** Convert a filename slug to a title-case short title. */
function slugToTitle(slug) {
  const ACRONYMS = {
    cli: "CLI",
    oauth: "OAuth",
    github: "GitHub",
    mcp: "MCP",
    api: "API",
    sdk: "SDK",
    tcp: "TCP",
    byok: "BYOK",
  };

  return slug
    .split("-")
    .map((word) => ACRONYMS[word] || word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

/** Return the children entries for an index.md file. */
function getChildren(indexPath) {
  const dir = path.dirname(indexPath);
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const children = [];

  for (const entry of entries) {
    if (entry.name === "index.md") continue;
    if (entry.name.startsWith(".")) continue;

    if (entry.isDirectory()) {
      const subFiles = fs.readdirSync(path.join(dir, entry.name));
      if (subFiles.some((f) => f.endsWith(".md"))) {
        children.push(`/${entry.name}`);
      }
    } else if (entry.isFile() && entry.name.endsWith(".md")) {
      children.push(`/${entry.name.replace(/\.md$/, "")}`);
    }
  }

  return children.sort();
}

/**
 * Convert a resolved absolute file path to a docs URL path.
 * e.g. /…/content/copilot/sdk-docs/setup/local-cli.md → /copilot/sdk-docs/setup/local-cli
 */
function filePathToUrlPath(absPath) {
  let rel = path.relative(CONTENT_DIR, absPath);
  rel = rel.replace(/\.md$/, "");
  rel = rel.replace(/\/index$/, "");
  return `/${rel}`;
}

// --- Processing steps ---

/**
 * Step 1: Add frontmatter to a markdown file.
 * Extracts title from the first H1, intro from the first paragraph.
 */
function addFrontmatter(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");

  // Skip files that already have frontmatter
  if (raw.startsWith("---")) {
    console.log(
      `  SKIP (has frontmatter): ${path.relative(SDK_DOCS_DIR, filePath)}`
    );
    return;
  }

  const lines = raw.split("\n");

  // Extract title from first H1
  let title = "";
  let titleLineIndex = -1;
  for (let i = 0; i < lines.length; i++) {
    const match = lines[i].match(/^#\s+(.+)$/);
    if (match) {
      title = match[1].trim();
      titleLineIndex = i;
      break;
    }
  }

  if (!title) {
    console.log(
      `  WARN (no H1): ${path.relative(SDK_DOCS_DIR, filePath)}`
    );
    title = path.basename(filePath, ".md");
  }

  // Extract intro: first non-empty paragraph after the title
  let intro = "";
  let introEndIndex = titleLineIndex;
  if (titleLineIndex >= 0) {
    let i = titleLineIndex + 1;
    while (i < lines.length && lines[i].trim() === "") i++;

    const paraLines = [];
    while (
      i < lines.length &&
      lines[i].trim() !== "" &&
      !lines[i].startsWith("#")
    ) {
      paraLines.push(lines[i].trim());
      i++;
    }
    introEndIndex = i;
    intro = paraLines.join(" ");
  }

  // Compute shortTitle from filename for slugified-title test compatibility
  const basename = path.basename(filePath, ".md");
  const shortTitle = basename === "index" ? undefined : slugToTitle(basename);

  // Build frontmatter
  const frontmatterData = {
    title,
    ...(shortTitle && { shortTitle }),
    ...(intro && { intro }),
    versions: { fpt: "*", ghec: "*" },
    contentType: "other",
  };

  const isIndex = path.basename(filePath) === "index.md";
  if (isIndex) {
    frontmatterData.children = getChildren(filePath);
  }

  // Remove the title line and intro paragraph from the body
  const bodyLines = [...lines];
  if (titleLineIndex >= 0) {
    bodyLines.splice(titleLineIndex, introEndIndex - titleLineIndex);
    while (bodyLines.length > 0 && bodyLines[0].trim() === "") {
      bodyLines.shift();
    }
  }

  const body = bodyLines.join("\n");
  const output = matter.stringify(body, frontmatterData);

  fs.writeFileSync(filePath, output, "utf8");
  console.log(`  OK: ${path.relative(SDK_DOCS_DIR, filePath)}`);
}

/**
 * Step 2: Rewrite internal relative .md links to [AUTOTITLE](/url-path) format.
 */
function rewriteInternalLinks(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");
  const dir = path.dirname(filePath);

  const linkRegex = /\[([^\]]+)\]\((\.{1,2}\/[^)]*\.md(?:#[^)]*)?)\)/g;

  let changed = false;
  const updated = raw.replace(linkRegex, (_match, _text, href) => {
    const [rawPath, anchor] = href.split("#", 2);
    const resolved = path.resolve(dir, rawPath);

    if (!resolved.startsWith(CONTENT_DIR)) return _match;
    if (!fs.existsSync(resolved)) {
      console.log(
        `  WARN (target missing): ${href} in ${path.relative(SDK_DOCS_DIR, filePath)}`
      );
      return _match;
    }

    const urlPath = filePathToUrlPath(resolved);
    const anchorSuffix = anchor ? `#${anchor}` : "";
    changed = true;
    return `[AUTOTITLE](${urlPath}${anchorSuffix})`;
  });

  if (changed) {
    fs.writeFileSync(filePath, updated, "utf8");
    console.log(
      `  LINKS: ${path.relative(SDK_DOCS_DIR, filePath)}`
    );
  }
}

/**
 * Step 2b: Rewrite repo-relative links that point outside the docs tree.
 * These are links like ../nodejs/README.md that should point to the SDK repo on GitHub.
 * Catches any remaining relative .md links that Step 2 didn't convert to AUTOTITLE.
 */
function rewriteRepoRelativeLinks(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");
  const dir = path.dirname(filePath);
  const SDK_REPO_URL = "https://github.com/github/copilot-sdk/tree/main";

  // Match relative .md links that were NOT already rewritten to AUTOTITLE
  const linkRegex = /\[([^\]]+)\]\((\.{1,2}\/[^)]*\.md(?:#[^)]*)?)\)/g;

  let changed = false;
  const updated = raw.replace(linkRegex, (_match, text, href) => {
    const [rawPath, anchor] = href.split("#", 2);
    const resolved = path.resolve(dir, rawPath);

    // Skip if the file actually exists in the content tree (should have been handled by Step 2)
    if (fs.existsSync(resolved)) return _match;

    // Compute where this file would be in the SDK repo.
    // SDK docs are at content/copilot/sdk-docs/ which maps to copilot-sdk/docs/
    // So a link from content/copilot/sdk-docs/getting-started.md to ../nodejs/README.md
    // resolves to content/copilot/nodejs/README.md → which in the SDK repo is nodejs/README.md
    const relFromSdkDocs = path.relative(SDK_DOCS_DIR, resolved);

    // Links starting with ../ from SDK_DOCS_DIR go up to the repo root
    // e.g. ../nodejs/README.md from sdk-docs/ → ../../nodejs/README.md from content/copilot/sdk-docs/
    // relFromSdkDocs would be like "../nodejs/README.md"
    // We strip leading ../ segments to get the repo-root-relative path
    const parts = relFromSdkDocs.split(path.sep);
    let upCount = 0;
    for (const part of parts) {
      if (part === "..") upCount++;
      else break;
    }
    // The repo path is everything after the ".." segments
    const repoPath = parts.slice(upCount).join("/");

    const anchorSuffix = anchor ? `#${anchor}` : "";
    changed = true;
    return `[${text}](${SDK_REPO_URL}/${repoPath}${anchorSuffix})`;
  });

  if (changed) {
    fs.writeFileSync(filePath, updated, "utf8");
    console.log(
      `  REPO-LINKS: ${path.relative(SDK_DOCS_DIR, filePath)}`
    );
  }
}

/**
 * Step 3: Rewrite absolute docs.github.com links to [AUTOTITLE](/url-path).
 */
function rewriteDocsGitHubLinks(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");

  const docsLinkRegex =
    /\[([^\]]+)\]\((https:\/\/docs\.github\.com\/(?:en\/)?([^)]*))\)/g;

  let changed = false;
  const updated = raw.replace(
    docsLinkRegex,
    (_match, _text, _fullUrl, pathAndAnchor) => {
      const [rawPath, anchor] = pathAndAnchor.split("#", 2);
      const urlPath = `/${rawPath}`;
      const contentPath = path.join(CONTENT_DIR, `${rawPath}.md`);
      const contentIndexPath = path.join(CONTENT_DIR, rawPath, "index.md");

      if (!fs.existsSync(contentPath) && !fs.existsSync(contentIndexPath)) {
        console.log(
          `  WARN (docs.github.com target missing): ${urlPath} in ${path.relative(SDK_DOCS_DIR, filePath)}`
        );
        return _match;
      }

      const anchorSuffix = anchor ? `#${anchor}` : "";
      changed = true;
      return `[AUTOTITLE](${urlPath}${anchorSuffix})`;
    }
  );

  if (changed) {
    fs.writeFileSync(filePath, updated, "utf8");
    console.log(
      `  DOCS-LINKS: ${path.relative(SDK_DOCS_DIR, filePath)}`
    );
  }
}

/**
 * Step 4: Create missing index.md files for subdirectories.
 */
function createMissingIndexFiles() {
  const created = [];

  function walk(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      if (!entry.isDirectory() || entry.name.startsWith(".")) continue;
      const dirPath = path.join(dir, entry.name);
      const indexPath = path.join(dirPath, "index.md");

      // Recurse into subdirectories
      walk(dirPath);

      if (fs.existsSync(indexPath)) continue;

      // Check that the directory has at least one .md file
      const dirFiles = fs.readdirSync(dirPath);
      if (!dirFiles.some((f) => f.endsWith(".md"))) continue;

      const title = slugToTitle(entry.name);
      const children = getChildren(indexPath);

      const frontmatterData = {
        title,
        versions: { fpt: "*", ghec: "*" },
        contentType: "other",
        children,
      };

      const content = matter.stringify("", frontmatterData);
      fs.writeFileSync(indexPath, content, "utf8");
      created.push(indexPath);
      console.log(
        `  CREATED: ${path.relative(SDK_DOCS_DIR, indexPath)}`
      );
    }
  }

  walk(SDK_DOCS_DIR);
  return created;
}

/**
 * Step 5: Fix code fence language aliases.
 * Replaces ```go with ```golang and ```ts with ```typescript.
 */
function fixCodeFenceLanguages(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");

  const REPLACEMENTS = {
    go: "golang",
    ts: "typescript",
  };

  let changed = false;
  const updated = raw.replace(
    /^(```)(go|ts)\s*$/gm,
    (_match, backticks, lang) => {
      if (REPLACEMENTS[lang]) {
        changed = true;
        return `${backticks}${REPLACEMENTS[lang]}`;
      }
      return _match;
    }
  );

  if (changed) {
    fs.writeFileSync(filePath, updated, "utf8");
    console.log(
      `  LANGS: ${path.relative(SDK_DOCS_DIR, filePath)}`
    );
  }
}

/**
 * Step 6: Normalize ordered list prefixes to all use 1.
 * Changes "2. foo", "3. bar" etc. to "1. foo", "1. bar".
 */
function normalizeOrderedLists(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");
  const lines = raw.split("\n");

  let changed = false;
  let inCodeBlock = false;

  for (let i = 0; i < lines.length; i++) {
    // Track code blocks to avoid modifying code
    if (lines[i].trimStart().startsWith("```")) {
      inCodeBlock = !inCodeBlock;
      continue;
    }
    if (inCodeBlock) continue;

    const match = lines[i].match(/^(\s*)(\d+)\.\s/);
    if (match && match[2] !== "1") {
      lines[i] = lines[i].replace(/^(\s*)\d+\.\s/, "$11. ");
      changed = true;
    }
  }

  if (changed) {
    fs.writeFileSync(filePath, lines.join("\n"), "utf8");
    console.log(
      `  LISTS: ${path.relative(SDK_DOCS_DIR, filePath)}`
    );
  }
}

// --- Main ---

console.log(`Normalizing SDK docs in: ${SDK_DOCS_DIR}`);
console.log(`Content directory: ${CONTENT_DIR}\n`);

// Step 1: Add frontmatter
console.log("--- Adding frontmatter ---\n");
const files = getAllMarkdownFiles(SDK_DOCS_DIR);
console.log(`Found ${files.length} markdown files.\n`);
for (const file of files) {
  addFrontmatter(file);
}

// Step 2: Rewrite internal links
console.log("\n--- Rewriting internal links ---\n");
const allFiles = getAllMarkdownFiles(SDK_DOCS_DIR);
for (const file of allFiles) {
  rewriteInternalLinks(file);
}

// Step 2b: Rewrite repo-relative links (outside content tree)
console.log("\n--- Rewriting repo-relative links ---\n");
for (const file of allFiles) {
  rewriteRepoRelativeLinks(file);
}

// Step 3: Rewrite docs.github.com links
console.log("\n--- Rewriting docs.github.com links ---\n");
for (const file of allFiles) {
  rewriteDocsGitHubLinks(file);
}

// Step 4: Create missing index files
console.log("\n--- Creating missing index.md files ---\n");
createMissingIndexFiles();

// Step 5: Fix code fence languages
console.log("\n--- Fixing code fence languages ---\n");
const updatedFiles = getAllMarkdownFiles(SDK_DOCS_DIR);
for (const file of updatedFiles) {
  fixCodeFenceLanguages(file);
}

// Step 6: Normalize ordered lists
console.log("\n--- Normalizing ordered lists ---\n");
for (const file of updatedFiles) {
  normalizeOrderedLists(file);
}

console.log("\n✅ Normalization complete.");
