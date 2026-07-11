import * as esbuild from "esbuild";
import { globSync } from "glob";
import { execSync } from "child_process";

const entryPoints = globSync("src/**/*.ts");

// ESM build
await esbuild.build({
    entryPoints,
    outbase: "src",
    outdir: "dist",
    format: "esm",
    platform: "node",
    target: "es2022",
    sourcemap: false,
    outExtension: { ".js": ".js" },
});

// CJS build — uses .js extension with a "type":"commonjs" package.json marker
await esbuild.build({
    entryPoints,
    outbase: "src",
    outdir: "dist/cjs",
    format: "cjs",
    platform: "node",
    target: "es2022",
    sourcemap: false,
    outExtension: { ".js": ".js" },
    logOverride: { "empty-import-meta": "silent" },
});

// Mark the CJS directory so Node treats .js files as CommonJS
import { writeFileSync } from "fs";
writeFileSync("dist/cjs/package.json", JSON.stringify({ type: "commonjs" }) + "\n");

// Generate .d.ts files
execSync("tsc", { stdio: "inherit" });
