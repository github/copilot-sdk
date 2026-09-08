import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

interface UnsafeIndicator {
    description: string;
    pattern: RegExp;
}

const unsafeIndicators: UnsafeIndicator[] = [
    { description: "public publication job", pattern: /^\s{2}publish-public:/m },
    { description: "GitHub Packages registry", pattern: /npm\.pkg\.github\.com/i },
    { description: "package write permission", pattern: /packages:\s*write/i },
    { description: "public package access", pattern: /--access\s+public/i },
    { description: "npm package publication command", pattern: /npm\s+publish\b/i },
    {
        description: "npm trusted publication setup",
        pattern: /trusted[\s-]*publish|npm\s+install\s+--global\s+npm@/i,
    },
    {
        description: "public npm write command",
        pattern:
            /(?:npm\s+(?:publish|dist-tag)|publish-manifest)[^\n]*registry\.npmjs\.org|registry\.npmjs\.org[^\n]*(?:npm\s+(?:publish|dist-tag)|publish-manifest|_authToken)/i,
    },
];

export function assertSafeTestWorkflow(workflow: string): void {
    const normalizedWorkflow = workflow.replace(/\\\r?\n\s*/g, " ");
    for (const indicator of unsafeIndicators) {
        assert(
            !indicator.pattern.test(workflow) && !indicator.pattern.test(normalizedWorkflow),
            `Unsafe test workflow contains ${indicator.description}.`
        );
    }
}

function main(): void {
    const [workflowPath] = process.argv.slice(2);
    if (!workflowPath) {
        throw new Error("Usage: sdk-canary-safety.ts <workflow-path>");
    }
    assertSafeTestWorkflow(readFileSync(workflowPath, "utf8"));
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    try {
        main();
    } catch (error) {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    }
}
