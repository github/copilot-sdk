/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { pathToFileURL } from "node:url";

export function validateNativeHost(classifier, host) {
  if (classifier === "linux-x64") {
    if (host.platform !== "linux" || host.arch !== "x64") {
      throw new Error(
        `Native ${classifier} packaging requires Linux x64; detected ${host.platform}-${host.arch}`,
      );
    }
    if (!host.glibcVersionRuntime) {
      throw new Error(
        `Native ${classifier} packaging requires glibc; musl and unknown libc hosts are unsupported`,
      );
    }
    return `Validated native build host: ${classifier} (glibc ${host.glibcVersionRuntime})`;
  }

  if (classifier === "win32-x64") {
    if (host.platform !== "win32" || host.arch !== "x64") {
      throw new Error(
        `Native ${classifier} packaging requires Windows x64; detected ${host.platform}-${host.arch}`,
      );
    }
    return `Validated native build host: ${classifier}`;
  }

  throw new Error(`Unsupported native build classifier: ${classifier}`);
}

export function detectNativeHost() {
  const report = process.report?.getReport();
  return {
    platform: process.platform,
    arch: process.arch,
    glibcVersionRuntime: report?.header?.glibcVersionRuntime,
  };
}

function main() {
  const [classifier] = process.argv.slice(2);
  if (!classifier) {
    console.error("Usage: node validate-native-host.mjs <classifier>");
    process.exitCode = 1;
    return;
  }

  try {
    console.log(validateNativeHost(classifier, detectNativeHost()));
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  main();
}
