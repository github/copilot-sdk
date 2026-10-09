/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFile } from "node:child_process";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { promisify } from "node:util";
import { test } from "vitest";

const execute = promisify(execFile);
const bootstrap = fileURLToPath(new URL("./startup.mjs", import.meta.url));

test.for(["tsx", "./server.ts"])(
  "records the last startup phase before import %s fails",
  async (failedImport, { expect, onTestFinished, signal }) => {
    const creatingDirectory = mkdtemp(
      path.join(os.tmpdir(), "sdk-proxy-startup-"),
    );
    let executing: ReturnType<typeof execute> | undefined;
    onTestFinished(async () => {
      const [created] = await Promise.allSettled([creatingDirectory]);
      try {
        if (executing) await Promise.allSettled([executing]);
      } finally {
        if (created.status === "fulfilled") {
          await rm(created.value, { recursive: true, force: true });
        }
      }
    });
    const directory = await creatingDirectory;
    signal.throwIfAborted();
    const loader = path.join(directory, "loader.mjs");
    const registration = path.join(directory, "register.mjs");
    await writeFile(
      loader,
      `export function resolve(specifier, context, nextResolve) {
      if (specifier === ${JSON.stringify(failedImport)}) throw new Error("CONTROLLED_IMPORT_FAILURE");
      if (specifier === "tsx") return { url: "data:text/javascript,export%20%7B%7D", shortCircuit: true };
      return nextResolve(specifier, context);
    }`,
    );
    await writeFile(
      registration,
      `import { register } from "node:module";
     register(${JSON.stringify(pathToFileURL(loader).href)}, import.meta.url);`,
    );

    executing = execute(
      process.execPath,
      ["--import", pathToFileURL(registration).href, bootstrap],
      {
        signal,
        env: { ...process.env, NODE_OPTIONS: "" },
      },
    );
    await expect(executing).rejects.toMatchObject({
      code: 1,
      stdout: "",
      stderr: expect.stringMatching(
        /\[SDK proxy startup\] Node entered.*CONTROLLED_IMPORT_FAILURE/su,
      ),
    });
    await expect(executing).rejects.toMatchObject({
      stderr:
        failedImport === "tsx"
          ? expect.not.stringContaining("TypeScript loader ready")
          : expect.stringContaining("TypeScript loader ready"),
    });
  },
);
