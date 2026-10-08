import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import { createRequire } from "node:module";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";

const directory = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const compilerDirectory = path.dirname(require.resolve("typescript/package.json"));
const loader = pathToFileURL(require.resolve("tsx")).href;

function validate(context, { source = "export const value: number = 42;", types = true, compiler = true, external = false, language = "typescript" } = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "sdk docs validation "));
  context.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const tooling = path.join(root, "scripts/docs-validation");
  const validation = path.join(root, "docs/.validation");
  const examples = path.join(validation, language);
  fs.mkdirSync(tooling, { recursive: true });
  fs.mkdirSync(examples, { recursive: true });
  fs.mkdirSync(path.join(root, "nodejs/node_modules"), { recursive: true });
  const linkType = process.platform === "win32" ? "junction" : "dir";
  fs.symlinkSync(path.join(directory, "node_modules"), path.join(tooling, "node_modules"), linkType);
  if (compiler) {
    fs.symlinkSync(compilerDirectory, path.join(root, "nodejs/node_modules/typescript"), linkType);
  }
  if (types) {
    const typeDirectory = path.join(root, "node_modules/@types/node");
    fs.mkdirSync(typeDirectory, { recursive: true });
    fs.writeFileSync(path.join(typeDirectory, "index.d.ts"), "export {};\n");
  }
  fs.copyFileSync(path.join(directory, "validate.ts"), path.join(tooling, "validate.ts"));
  fs.writeFileSync(path.join(root, "package.json"), JSON.stringify({ type: "module" }));
  if (source !== null) {
    fs.writeFileSync(path.join(examples, "sample.ts"), source);
  }
  if (external) {
    fs.writeFileSync(path.join(root, "broken.ts"), 'export const value: number = "invalid";');
  }
  fs.writeFileSync(path.join(validation, "manifest.json"), JSON.stringify({ blocks: source === null ? [] : [{
    sourceFile: "fixture.md", sourceLine: 7, outputFile: "typescript/sample.ts",
  }] }));
  const environment = { ...process.env };
  delete environment.GITHUB_STEP_SUMMARY;
  const result = spawnSync(process.execPath, [
    "--import", loader, path.join(tooling, "validate.ts"), `--lang=${language}`,
  ], { cwd: root, encoding: "utf8", env: environment, timeout: 60000 });
  assert.ifError(result.error);
  assert.equal(result.signal, null);
  return { status: result.status, output: result.stdout + result.stderr, root };
}

test("valid TypeScript succeeds in a path containing spaces", (context) => {
  const result = validate(context);
  assert.equal(result.status, 0, result.output);
  assert.match(result.output, /1 files passed/);
});

test("example type errors fail and retain their documentation location", (context) => {
  const result = validate(context, { source: 'export const value: number = "invalid";' });
  assert.equal(result.status, 1, result.output);
  assert.match(result.output, /TS2322/);
  assert.match(result.output, /fixture\.md:7/);
});

test("global compiler diagnostics fail validation", (context) => {
  const result = validate(context, { types: false });
  assert.equal(result.status, 1, result.output);
  assert.match(result.output, /TS2688/);
  assert.doesNotMatch(result.output, /All documentation code blocks are valid/);
});

test("a missing compiler fails validation", (context) => {
  const result = validate(context, { compiler: false });
  assert.equal(result.status, 1, result.output);
  assert.match(result.output, /Cannot find module|MODULE_NOT_FOUND/);
  assert.doesNotMatch(result.output, /All documentation code blocks are valid/);
});

test("errors outside extracted examples fail validation", (context) => {
  const result = validate(context, {
    source: 'export { value } from "../../../broken.js";', external: true,
  });
  assert.equal(result.status, 1, result.output);
  assert.match(result.output, /broken\.ts/);
  assert.match(result.output, /TS2322/);
});

test("generated Go module preserves a checkout path containing spaces", (context) => {
  const version = spawnSync("go", ["version"], { encoding: "utf8" });
  if (version.error?.code === "ENOENT") {
    context.skip("Go is not installed");
    return;
  }
  assert.ifError(version.error);
  assert.equal(version.status, 0, version.stderr);
  const result = validate(context, { language: "go", source: null });
  assert.equal(result.status, 0, result.output);
  const parsed = spawnSync("go", ["mod", "edit", "-json"], {
    cwd: path.join(result.root, "docs/.validation/go"), encoding: "utf8",
  });
  assert.ifError(parsed.error);
  assert.equal(parsed.status, 0, parsed.stderr);
  const replacement = JSON.parse(parsed.stdout).Replace.find(
    (entry) => entry.Old.Path === "github.com/github/copilot-sdk/go",
  );
  assert.equal(replacement.New.Path, path.join(result.root, "go"));
});
