import * as esbuild from "esbuild";
import { globSync } from "glob";
import { execSync } from "child_process";

const entryPoints = globSync("src/**/*.ts");

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

// Generate .d.ts files
execSync("tsc", { stdio: "inherit" });
