#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs, { globSync } from "node:fs";
import path from "node:path";
import process from "node:process";
import { spawnSync } from "node:child_process";

function usage() {
    console.log(
        "Usage: run-node-tests.mjs --producer <id> [--cwd <directory>] [--node-arg <arg>] -- <test files/globs>\n" +
            "       run-node-tests.mjs --producer <id> [--cwd <directory>] --npm-script <script>",
    );
}

function sanitize(value) {
    return value.replaceAll(/[^A-Za-z0-9._-]/g, "-");
}

const args = process.argv.slice(2);
if (args.length === 0 || args.includes("--help")) {
    usage();
    process.exit(0);
}

let producer = "";
let cwd = process.cwd();
let npmScript = "";
const nodeArgs = [];
let index = 0;
for (; index < args.length; index++) {
    if (args[index] === "--") {
        index++;
        break;
    }
    if (args[index] === "--producer") {
        producer = args[++index] ?? "";
    } else if (args[index] === "--cwd") {
        cwd = path.resolve(args[++index] ?? "");
    } else if (args[index] === "--node-arg") {
        nodeArgs.push(args[++index] ?? "");
    } else if (args[index] === "--npm-script") {
        npmScript = args[++index] ?? "";
    } else {
        usage();
        process.exit(2);
    }
}
if (!producer || (!npmScript && index >= args.length)) {
    usage();
    process.exit(2);
}

const files = npmScript
    ? []
    : args.slice(index).flatMap((value) => {
          if (!/[*?[\]{}]/.test(value)) {
              return [value];
          }
          return globSync(value, { cwd }).sort();
      });
if (!npmScript && files.length === 0) {
    console.error("No Node test files matched the supplied paths or globs.");
    process.exit(2);
}
const outputDirectory = process.env.CI_TEST_RESULTS_DIR;
if (outputDirectory) {
    fs.mkdirSync(outputDirectory, { recursive: true });
}
const reportPath = outputDirectory ? path.join(outputDirectory, `node-${sanitize(producer)}.xml`) : "";
const reporterArgs = outputDirectory
    ? [
          "--test-reporter=spec",
          "--test-reporter-destination=stdout",
          "--test-reporter=junit",
          `--test-reporter-destination=${reportPath}`,
      ]
    : [];
let command = process.execPath;
let commandArguments = [...nodeArgs, "--test", ...reporterArgs, ...files];
let shell = false;
if (npmScript) {
    const packageJson = JSON.parse(fs.readFileSync(path.join(cwd, "package.json"), "utf8"));
    const script = packageJson.scripts?.[npmScript];
    if (typeof script !== "string" || !/\bnode\s+--test\b/.test(script)) {
        console.error(`Package script ${npmScript} must invoke node --test.`);
        process.exit(2);
    }
    const injected = outputDirectory
        ? `node --test --test-reporter=spec --test-reporter-destination=stdout --test-reporter=junit --test-reporter-destination=${JSON.stringify(reportPath)}`
        : "node --test";
    command = script.replace(/\bnode\s+--test\b/, injected);
    commandArguments = [];
    shell = true;
}
const result = spawnSync(command, commandArguments, {
    cwd,
    env: {
        ...process.env,
        CI_TEST_PRODUCER_ID: producer,
    },
    shell,
    stdio: "inherit",
});
if (result.error) {
    throw result.error;
}
process.exitCode = result.status ?? 1;
