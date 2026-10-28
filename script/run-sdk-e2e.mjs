/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { spawn } from "node:child_process";
import { once } from "node:events";
import { readSync } from "node:fs";
import { mkdtemp, mkdir, open, readFile, readdir, rm, stat, writeFile } from "node:fs/promises";
import { constants, tmpdir } from "node:os";
import path from "node:path";
import { createInterface } from "node:readline";
import { randomUUID } from "node:crypto";
import { PassThrough } from "node:stream";
import { pathToFileURL } from "node:url";

import { DEFAULT_DATADOG_SITE, normalizeDatadogTagValue, submitDatadogMetric } from "./datadog-metrics.mjs";

const LANGUAGES = new Set(["nodejs", "python", "go", "dotnet", "rust", "java"]);
const STATUS_ORDER = { skipped: 0, passed: 1, failed: 2 };

export function addResult(results, suite, status) {
    if (!suite || !Object.hasOwn(STATUS_ORDER, status)) {
        throw new Error("Invalid SDK E2E suite result.");
    }
    if (!results.has(suite) || STATUS_ORDER[status] > STATUS_ORDER[results.get(suite)]) {
        results.set(suite, status);
    }
}

export function parseVitestResults(text, results, counts = { passed: 0, failed: 0 }) {
    const report = JSON.parse(text);
    if (!Array.isArray(report.testResults)) throw new Error("Invalid Vitest JSON report.");
    for (const file of report.testResults) {
        const name = file.name.replaceAll("\\", "/");
        const index = name.lastIndexOf("/test/e2e/");
        if (index < 0 && !name.startsWith("test/e2e/")) continue;
        addResult(
            results,
            index < 0 ? name : name.slice(index + 1),
            file.status === "pending" ? "skipped" : file.status,
        );
        for (const test of file.assertionResults ?? []) {
            if (Object.hasOwn(counts, test.status)) counts[test.status]++;
        }
    }
}

// Only report identities and outcomes are needed; never extract names, output, or failure messages.
function xmlAttributes(text) {
    return Object.fromEntries(
        [...text.matchAll(/([\w:.-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')/g)].map((match) => [
            match[1],
            (match[2] ?? match[3]).replace(/&(?:amp|quot|apos|lt|gt);/g, (entity) => {
                return { "&amp;": "&", "&quot;": '"', "&apos;": "'", "&lt;": "<", "&gt;": ">" }[entity];
            }),
        ]),
    );
}

export function parseXmlResults(text, language, results, javaSuites = new Set(), counts = { passed: 0, failed: 0 }) {
    let xml = text;
    let previousXml = "";
    do {
        previousXml = xml;
        // Removing a comment can join surrounding text into a new comment opener.
        const withoutComments = xml.replace(/<!\[CDATA\[[\s\S]*?(?:\]\]>|$)|<!--[\s\S]*?(?:-->|$)/g, "");
        xml = withoutComments;
    } while (xml !== previousXml);
    if (language === "dotnet") {
        if (!xml.includes("<TestRun")) throw new Error("Invalid TRX report.");
        const classes = new Map();
        for (const match of xml.matchAll(/<UnitTest\b([^>]*)>([\s\S]*?)<\/UnitTest>/g)) {
            const method = match[2].match(/<TestMethod\b([^>]*)\/?>/);
            if (method) classes.set(xmlAttributes(match[1]).id, xmlAttributes(method[1]).className);
        }
        for (const match of xml.matchAll(/<UnitTestResult\b([^>]*)\/?>/g)) {
            const attributes = xmlAttributes(match[1]);
            const suite = classes.get(attributes.testId);
            if (!suite?.includes(".E2E.")) continue;
            const status =
                attributes.outcome === "Passed"
                    ? "passed"
                    : attributes.outcome === "NotExecuted"
                      ? "skipped"
                      : "failed";
            addResult(results, suite, status);
            if (Object.hasOwn(counts, status)) counts[status]++;
        }
        return;
    }
    if (!/<testsuites?\b/.test(xml)) throw new Error("Invalid JUnit report.");
    if (language === "java") {
        for (const match of xml.matchAll(/<testsuite\b([^>]*)>/g)) {
            const attributes = xmlAttributes(match[1]);
            if (!attributes.name?.includes(".e2e.") && !javaSuites.has(attributes.name?.split("$")[0])) continue;
            const failures = Number(attributes.failures ?? 0) + Number(attributes.errors ?? 0);
            const executed = Number(attributes.tests) - Number(attributes.skipped ?? 0);
            if (
                ![failures, executed].every((count) => Number.isSafeInteger(count) && count >= 0) ||
                failures > executed
            )
                throw new Error("Invalid JUnit counts.");
            addResult(results, attributes.name, failures > 0 ? "failed" : executed > 0 ? "passed" : "skipped");
            counts.failed += failures;
            counts.passed += executed - failures;
        }
        return;
    }
    for (const match of xml.matchAll(/<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g)) {
        const attributes = xmlAttributes(match[1]);
        const identity = attributes.classname || attributes.file || attributes.name;
        if (!/^e2e[./][\w./-]+$/.test(identity ?? "")) continue;
        const suite =
            identity.startsWith("e2e/") && identity.endsWith(".py")
                ? identity
                : identity
                      .split(".")
                      .filter((part) => !part.startsWith("Test"))
                      .join("/") + ".py";
        const body = match[2] ?? "";
        const status = /<(?:failure|error)\b/.test(body) ? "failed" : /<skipped\b/.test(body) ? "skipped" : "passed";
        addResult(results, suite, status);
        if (Object.hasOwn(counts, status)) counts[status]++;
    }
}

export function consoleResults(language, results, counts = { passed: 0, failed: 0 }) {
    const goParents = new Set();
    const goFailedParents = new Set();
    let rustSuite;
    let rustFailures = false;
    return (rawLine) => {
        const line = rawLine.replace(/\x1b\[[0-9;]*m/g, "");
        if (language === "go") {
            if (!line.startsWith("{")) return `${rawLine}\n`;
            const event = JSON.parse(line);
            if (
                event.Package?.endsWith("/internal/e2e") &&
                event.Test &&
                ["pass", "fail", "skip"].includes(event.Action)
            ) {
                const parts = event.Test.split("/");
                for (let depth = 1; depth < parts.length; depth++) {
                    const parent = parts.slice(0, depth).join("/");
                    goParents.add(parent);
                    if (event.Action === "fail") goFailedParents.add(parent);
                }
                addResult(
                    results,
                    event.Test.split("/")[0],
                    { pass: "passed", fail: "failed", skip: "skipped" }[event.Action],
                );
                if (event.Action === "pass" && !goParents.has(event.Test)) counts.passed++;
                if (event.Action === "fail" && !goFailedParents.has(event.Test)) counts.failed++;
            }
            return typeof event.Output === "string" ? event.Output : "";
        } else if (language === "rust") {
            const running = line.match(/Running (?:.*[/\\])?(e2e|runtime_host_e2e)\.rs\b/);
            if (line.includes("Running ")) {
                rustSuite = running?.[1];
                rustFailures = false;
            }
            if (!rustSuite) return;
            const result = line.match(/\btest ([\w:]+) \.\.\. (ok|FAILED|ignored)\b/);
            if (result) {
                addResult(
                    results,
                    result[1].includes("::") ? `${rustSuite}::${result[1].split("::")[0]}` : rustSuite,
                    { ok: "passed", FAILED: "failed", ignored: "skipped" }[result[2]],
                );
            }
            // --nocapture output can interrupt individual result lines. libtest's final
            // failure list still names every failed test.
            if (line === "failures:") rustFailures = true;
            else if (line.startsWith("test result:")) {
                rustFailures = false;
                const summary = line.match(/^test result: (?:ok|FAILED)\. (\d+) passed; (\d+) failed\b/);
                if (summary) {
                    counts.passed += Number(summary[1]);
                    counts.failed += Number(summary[2]);
                }
            } else if (rustFailures && /^\s+[\w:]+\s*$/.test(line)) {
                const name = line.trim();
                addResult(results, name.includes("::") ? `${rustSuite}::${name.split("::")[0]}` : rustSuite, "failed");
            }
        }
    };
}

async function reportFiles(language, env, cwd) {
    const directories =
        language === "java"
            ? ["sdk/target/surefire-reports", "sdk/target/surefire-reports-isolated", "sdk/target/failsafe-reports"]
            : language === "dotnet"
              ? [env.DOTNET_TEST_RESULTS_DIRECTORY || "TestResults"]
              : [];
    const files = [];
    async function walk(directory) {
        let entries;
        try {
            entries = await readdir(directory, { withFileTypes: true });
        } catch (error) {
            if (error.code === "ENOENT") return;
            throw error;
        }
        for (const entry of entries) {
            const file = path.join(directory, entry.name);
            if (entry.isDirectory()) await walk(file);
            else if (
                entry.isFile() &&
                (language === "dotnet"
                    ? file.endsWith(".trx")
                    : entry.name.startsWith("TEST-") && file.endsWith(".xml"))
            ) {
                files.push(file);
            }
        }
    }
    for (const directory of directories) await walk(path.resolve(cwd, directory));
    return files;
}

export function buildPayload(report, env, timestamp = Math.round(Date.now() / 1000)) {
    const branchType =
        env.GITHUB_EVENT_NAME === "merge_group" || env.GITHUB_REF?.startsWith("refs/heads/gh-readonly-queue/")
            ? "merge_queue"
            : env.GITHUB_REF === "refs/heads/main"
              ? "main"
              : "other";
    const tags = [
        `repository:${normalizeDatadogTagValue(env.GITHUB_REPOSITORY || "github/copilot-agent-runtime")}`,
        `language:${report.language}`,
        `platform:${normalizeDatadogTagValue(report.platform)}`,
        `os:${normalizeDatadogTagValue(report.os)}`,
        `backend:${normalizeDatadogTagValue(report.backend)}`,
        `connection:${report.connection}`,
        `branch_type:${branchType}`,
        `success:${report.success}`,
    ];
    return {
        series: [
            ...["passed", "failed"]
                .filter((status) => report.counts[status] > 0)
                .map((status) => ({
                    host: "github.com",
                    metric: "github.copilot_sdk.e2e.test.total",
                    type: "count",
                    points: [[timestamp, report.counts[status]]],
                    tags: [...tags, `status:${status}`],
                })),
            {
                host: "github.com",
                metric: "github.copilot_sdk.e2e.file.total",
                type: "count",
                points: [[timestamp, report.results.filter(([suite]) => suite !== "test-command").length]],
                tags,
            },
        ],
    };
}

export async function runTests(language, connection, command, env = process.env, cwd = process.cwd()) {
    if (!LANGUAGES.has(language) || !["subprocess", "inprocess"].includes(connection) || command.length === 0) {
        throw new Error("Expected an SDK language, connection (subprocess/inprocess), and a test command.");
    }
    const results = new Map();
    const counts = { passed: 0, failed: 0 };
    const temporaryDirectory = await mkdtemp(path.join(tmpdir(), "sdk-e2e-"));
    const childEnv = { ...env, SDK_E2E_REPORT: env.SDK_E2E_REPORT || path.join(temporaryDirectory, "results") };
    for (const name of Object.keys(childEnv)) {
        if (/^(?:DATADOG_|DD_)/.test(name)) delete childEnv[name];
    }
    if (language === "go") childEnv.GOFLAGS = `${childEnv.GOFLAGS || ""} -json`.trim();
    const before = new Map();
    try {
        for (const file of await reportFiles(language, env, cwd)) before.set(file, (await stat(file)).mtimeMs);
    } catch {
        console.warn("::warning::Could not snapshot existing SDK test reports.");
    }
    let parsingFailed = false;
    const observe = consoleResults(language, results, counts);
    let exitCode;
    let capture;
    try {
        if (language === "go" || language === "rust") {
            // Regular files cannot hold the command open when descendants inherit its output handles.
            const log = await open(path.join(temporaryDirectory, "console.log"), "w+");
            const output = new PassThrough();
            if (language !== "go") output.on("data", (chunk) => process.stdout.write(chunk));
            const lines = createInterface({ input: output });
            lines.on("line", (line) => {
                try {
                    const text = observe(line);
                    if (language === "go") process.stdout.write(text);
                } catch {
                    parsingFailed = true;
                    if (language === "go") process.stdout.write(`${line}\n`);
                }
            });
            const buffer = Buffer.alloc(64 * 1024);
            let position = 0;
            const flush = () => {
                let bytes;
                while ((bytes = readSync(log.fd, buffer, 0, buffer.length, position)) > 0) {
                    position += bytes;
                    output.write(Buffer.from(buffer.subarray(0, bytes)));
                }
            };
            capture = { log, output, closed: once(lines, "close"), flush, timer: setInterval(flush, 100) };
        }
        exitCode = await new Promise((resolve, reject) => {
            const child = spawn(command[0], command.slice(1), {
                cwd,
                env: childEnv,
                stdio: capture ? ["inherit", capture.log.fd, capture.log.fd] : "inherit",
            });
            child.on("error", reject);
            child.on("close", (code, signal) => resolve(code ?? (signal ? 128 + (constants.signals[signal] ?? 0) : 1)));
        });
    } catch (error) {
        console.error(`Could not start SDK test command: ${error.message}`);
        exitCode = 1;
    } finally {
        if (capture) {
            clearInterval(capture.timer);
            capture.flush();
            capture.output.end();
            await capture.closed;
            await capture.log.close();
        }
    }
    try {
        if (language === "nodejs") parseVitestResults(await readFile(childEnv.SDK_E2E_REPORT, "utf8"), results, counts);
        else if (language === "python")
            parseXmlResults(await readFile(childEnv.SDK_E2E_REPORT, "utf8"), language, results, new Set(), counts);
        else if (language === "go" && results.size > 0) {
            const groups = [...results];
            results.clear();
            const testFiles = new Map();
            const directory = path.join(cwd, "internal/e2e");
            for (const file of (await readdir(directory)).filter((name) => name.endsWith("_test.go"))) {
                const source = await readFile(path.join(directory, file), "utf8");
                for (const match of source.matchAll(/^func\s+(Test[^\s(]*)\s*\(/gm)) {
                    testFiles.set(match[1], `internal/e2e/${file}`);
                }
            }
            for (const [test, status] of groups) {
                const file = testFiles.get(test);
                if (file) addResult(results, file, status);
                else parsingFailed = true;
            }
        } else {
            for (const file of await reportFiles(language, env, cwd)) {
                try {
                    if (before.get(file) === (await stat(file)).mtimeMs) continue;
                    const text = await readFile(file, "utf8");
                    const javaSuites = new Set();
                    if (language === "java") {
                        for (const match of text.matchAll(/<testsuite\b([^>]*)>/g)) {
                            const name = xmlAttributes(match[1]).name?.split("$")[0];
                            if (!name || !/^com\.github\.copilot(?:\.[A-Za-z_]\w*)+$/.test(name)) continue;
                            if (name.includes(".e2e.")) continue;
                            const source = path.join(cwd, "sdk/src/test/java", ...name.split(".")) + ".java";
                            const contents = await readFile(source, "utf8");
                            if (contents.includes("E2ETestContext")) javaSuites.add(name);
                        }
                    }
                    parseXmlResults(text, language, results, javaSuites, counts);
                } catch {
                    parsingFailed = true;
                }
            }
        }
    } catch {
        parsingFailed = true;
    }
    if (parsingFailed)
        console.warn("::warning::Some SDK E2E results could not be read; retaining available suite results.");
    if (exitCode !== 0 && ![...results.values()].includes("failed")) addResult(results, "test-command", "failed");
    try {
        const directory = path.join(env.GITHUB_WORKSPACE || cwd, ".sdk-e2e-metrics");
        await mkdir(directory, { recursive: true });
        await writeFile(
            path.join(directory, `${randomUUID()}.json`),
            JSON.stringify({
                language,
                connection,
                platform: env.COPILOT_RUNTIME_TARGET || `${process.platform}-${process.arch}`,
                os: env.RUNNER_OS || process.platform,
                backend: env.COPILOT_SDK_E2E_BACKEND || "capi",
                success: exitCode === 0,
                counts,
                results: [...results],
            }),
        );
    } catch {
        console.warn("::warning::Could not save SDK E2E measurements; the test result is unchanged.");
    } finally {
        try {
            await rm(temporaryDirectory, { recursive: true });
        } catch {
            console.warn("::warning::Could not remove temporary SDK E2E report directory.");
        }
    }
    return exitCode;
}

export async function publish(directory, env = process.env, submit = submitDatadogMetric) {
    let files;
    try {
        files = await readdir(directory);
    } catch (error) {
        if (error.code === "ENOENT") {
            console.log("No SDK E2E measurements were produced.");
            return;
        }
        throw error;
    }
    const apiKey = env.DATADOG_API_KEY?.trim();
    if (!apiKey) console.warn("::warning::DATADOG_API_KEY is unavailable; skipping SDK E2E publication.");
    for (const file of files.filter((name) => name.endsWith(".json"))) {
        const reportPath = path.join(directory, file);
        try {
            const report = JSON.parse(await readFile(reportPath, "utf8"));
            if (
                !LANGUAGES.has(report.language) ||
                !["subprocess", "inprocess"].includes(report.connection) ||
                !["platform", "os", "backend"].every((name) => typeof report[name] === "string") ||
                typeof report.success !== "boolean" ||
                !report.counts ||
                !["passed", "failed"].every(
                    (status) => Number.isSafeInteger(report.counts[status]) && report.counts[status] >= 0,
                ) ||
                !Array.isArray(report.results) ||
                !report.results.every(
                    (result) =>
                        Array.isArray(result) &&
                        result.length === 2 &&
                        typeof result[0] === "string" &&
                        Object.hasOwn(STATUS_ORDER, result[1]),
                )
            ) {
                throw new Error("Invalid SDK E2E measurements.");
            }
            const payload = buildPayload(report, env);
            if (apiKey && payload.series.length > 0) {
                await submit(env.DATADOG_SITE?.trim() || DEFAULT_DATADOG_SITE, apiKey, payload);
                console.log(`Published ${payload.series.length} ${report.language} SDK E2E count series.`);
            }
        } catch {
            // Remote response bodies can echo credentials; do not include them in workflow logs.
            console.warn("::warning::SDK E2E publication failed; the test result is unchanged.");
        } finally {
            await rm(reportPath);
        }
    }
}

export async function main(args = process.argv.slice(2)) {
    if (args[0] === "--run" && args[3] === "--") {
        return runTests(args[1], args[2], args.slice(4));
    }
    if (args[0] === "--publish" && args.length === 1) {
        try {
            await publish(path.join(process.env.GITHUB_WORKSPACE || process.cwd(), ".sdk-e2e-metrics"));
        } catch {
            console.warn("::warning::SDK E2E publication failed; the test result is unchanged.");
        }
        return 0;
    }
    console.log(
        "Usage: node script/run-sdk-e2e.mjs --run <language> <subprocess|inprocess> -- <command> [args...]\n" +
            "       node script/run-sdk-e2e.mjs --publish\n" +
            "SDK_E2E_REPORT selects the Vitest JSON/pytest JUnit report path; the child receives an automatic path otherwise.\n" +
            "Publication reads .sdk-e2e-metrics in GITHUB_WORKSPACE and uses DATADOG_API_KEY and optional DATADOG_SITE.",
    );
    return args.length === 0 || args[0] === "--help" ? 0 : 2;
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
    process.exitCode = await main();
}
