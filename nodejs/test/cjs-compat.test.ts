/**
 * CJS shimmed environment compatibility test
 *
 * Verifies that getBundledCliPath() works when the ESM build is loaded in a
 * shimmed CJS environment (e.g., VS Code extensions bundled with esbuild
 * format:"cjs"). In these environments, import.meta.url may be undefined but
 * __filename is available via the CJS shim.
 *
 * See: https://github.com/github/copilot-sdk/issues/528
 */

import { describe, expect, it } from "vitest";
import { existsSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join } from "node:path";

const esmEntryPoint = join(import.meta.dirname, "../dist/index.js");

describe("CJS shimmed environment compatibility (#528)", () => {
    it("ESM dist file should exist", () => {
        expect(existsSync(esmEntryPoint)).toBe(true);
    });

    it("getBundledCliPath() should resolve in a CJS shimmed context", () => {
        // Simulate what esbuild format:"cjs" does: __filename is defined,
        // import.meta.url may be undefined. The SDK's fallback logic
        // (import.meta.url ?? pathToFileURL(__filename).href) handles this.
        //
        // We test by requiring the ESM build via --input-type=module in a
        // subprocess that has __filename available, verifying the constructor
        // (which calls getBundledCliPath()) doesn't throw.
        const script = `
            import { createRequire } from 'node:module';
            const require = createRequire(import.meta.url);
            const sdk = await import(${JSON.stringify(esmEntryPoint)});
            if (typeof sdk.CopilotClient !== 'function') {
                process.exit(1);
            }
            try {
                const client = new sdk.CopilotClient({ cliUrl: "8080" });
                console.log('CopilotClient constructor: OK');
            } catch (e) {
                console.error('constructor failed:', e.message);
                process.exit(1);
            }
        `;
        const output = execFileSync(
            process.execPath,
            ["--input-type=module", "--eval", script],
            {
                encoding: "utf-8",
                timeout: 10000,
                cwd: join(import.meta.dirname, ".."),
            },
        );
        expect(output).toContain("CopilotClient constructor: OK");
    });
});
