/**
 * Dual ESM/CJS build compatibility tests
 *
 * Verifies that both the ESM and CJS builds exist and work correctly,
 * so consumers using either module system get a working package.
 *
 * See: https://github.com/github/copilot-sdk/issues/528
 */

import { describe, expect, it } from "vitest";
import { existsSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join } from "node:path";

const distDir = join(import.meta.dirname, "../dist");

describe("Dual ESM/CJS build (#528)", () => {
    it("ESM dist file should exist", () => {
        expect(existsSync(join(distDir, "index.js"))).toBe(true);
    });

    it("CJS dist file should exist", () => {
        expect(existsSync(join(distDir, "cjs/index.js"))).toBe(true);
    });

    it("CJS build is requireable and exports CopilotClient and Dynamic Workflows", () => {
        const script = `
            const sdk = require(${JSON.stringify(join(distDir, "cjs/index.js"))});
            if (typeof sdk.CopilotClient !== 'function') {
                console.error('CopilotClient is not a function');
                process.exit(1);
            }
            if (typeof sdk.defineWorkflow !== 'function') {
                console.error('defineWorkflow is not a function');
                process.exit(1);
            }
            console.log('CJS require: OK');
        `;
        const output = execFileSync(process.execPath, ["--eval", script], {
            encoding: "utf-8",
            timeout: 10000,
            cwd: join(import.meta.dirname, ".."),
        });
        expect(output).toContain("CJS require: OK");
    });

    it("extension builds export defineWorkflow", () => {
        for (const { extensionPath, moduleType } of [
            { extensionPath: join(distDir, "extension.js"), moduleType: "esm" },
            { extensionPath: join(distDir, "cjs/extension.js"), moduleType: "cjs" },
        ]) {
            const script =
                moduleType === "cjs"
                    ? `
                    const sdk = require(${JSON.stringify(extensionPath)});
                    if (typeof sdk.defineWorkflow !== 'function') process.exit(1);
                    console.log('defineWorkflow cjs: OK');
                `
                    : `
                    import { pathToFileURL } from 'node:url';
                    const sdk = await import(pathToFileURL(${JSON.stringify(extensionPath)}).href);
                    if (typeof sdk.defineWorkflow !== 'function') process.exit(1);
                    console.log('defineWorkflow esm: OK');
                `;
            const output = execFileSync(
                process.execPath,
                [...(moduleType === "cjs" ? [] : ["--input-type=module"]), "--eval", script],
                {
                    encoding: "utf-8",
                    timeout: 10000,
                    cwd: join(import.meta.dirname, ".."),
                }
            );
            expect(output).toContain(`defineWorkflow ${moduleType}: OK`);
        }
    });

    it("CJS build resolves bundled CLI path", () => {
        const script = `
            const sdk = require(${JSON.stringify(join(distDir, "cjs/index.js"))});
            const client = new sdk.CopilotClient({ });
            console.log('CJS CLI resolved: OK');
        `;
        const output = execFileSync(process.execPath, ["--eval", script], {
            encoding: "utf-8",
            timeout: 10000,
            cwd: join(import.meta.dirname, ".."),
        });
        expect(output).toContain("CJS CLI resolved: OK");
    });

    it("ESM build resolves bundled CLI path", () => {
        const esmPath = join(distDir, "index.js");
        const script = `
            import { pathToFileURL } from 'node:url';
            const sdk = await import(pathToFileURL(${JSON.stringify(esmPath)}).href);
            const client = new sdk.CopilotClient({ });
            console.log('ESM CLI resolved: OK');
        `;
        const output = execFileSync(process.execPath, ["--input-type=module", "--eval", script], {
            encoding: "utf-8",
            timeout: 10000,
            cwd: join(import.meta.dirname, ".."),
        });
        expect(output).toContain("ESM CLI resolved: OK");
    });
});
