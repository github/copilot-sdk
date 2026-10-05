/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";

export function installNpmDependencies(directory, runNpm) {
    const nodeModules = join(directory, "node_modules");
    const stampFile = join(nodeModules, ".copilot-sdk-install-stamp");
    const args = ["ci", "--ignore-scripts", "--include=dev"];
    const hash = createHash("sha256").update(JSON.stringify(args));
    for (const file of ["package-lock.json", "package.json"]) {
        hash.update(readFileSync(join(directory, file)));
    }
    const fingerprint = hash.digest("hex");
    let stamp;
    try {
        stamp = readFileSync(stampFile, "utf8").trim();
    } catch (error) {
        if (error.code !== "ENOENT") {
            throw error;
        }
    }
    if (existsSync(nodeModules) && stamp === fingerprint) {
        return false;
    }

    // Failed installs must not leave a stamp claiming the dependency tree is current.
    rmSync(stampFile, { force: true });
    runNpm(args, directory);
    mkdirSync(nodeModules, { recursive: true });
    writeFileSync(stampFile, `${fingerprint}\n`);
    return true;
}
