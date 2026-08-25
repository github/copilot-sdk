/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

import {
  validateNativeClassifierJar,
  validatePlaceholderJar,
} from "./validate-native-artifact.mjs";

export function validateLocalPublication({
  artifactId,
  repositoryPath,
  repoRoot,
  requireSignatures,
  version,
}) {
  const artifactDirectory = path.join(
    repositoryPath,
    "com",
    "github",
    artifactId,
    version,
  );
  const requiredArtifacts = [
    `${artifactId}-${version}.jar`,
    `${artifactId}-${version}.pom`,
    `${artifactId}-${version}-sources.jar`,
    `${artifactId}-${version}-javadoc.jar`,
    `${artifactId}-${version}-linux-x64.jar`,
    `${artifactId}-${version}-win32-x64.jar`,
  ];
  const files = new Set(fs.readdirSync(artifactDirectory));

  for (const artifact of requiredArtifacts) {
    if (!files.has(artifact)) {
      throw new Error(`Local publication is missing ${artifact}`);
    }
    if (requireSignatures && !files.has(`${artifact}.asc`)) {
      throw new Error(
        `Local release publication is missing signature ${artifact}.asc`,
      );
    }
  }

  const publishedJars = [...files]
    .filter((file) => file.endsWith(".jar"))
    .sort();
  const expectedJars = requiredArtifacts
    .filter((file) => file.endsWith(".jar"))
    .sort();
  if (publishedJars.join("\n") !== expectedJars.join("\n")) {
    throw new Error(
      `Local publication contains unexpected JARs:\n${publishedJars.join("\n")}`,
    );
  }

  validatePlaceholderJar(
    path.join(artifactDirectory, `${artifactId}-${version}.jar`),
  );
  for (const classifier of ["linux-x64", "win32-x64"]) {
    const filename = `${artifactId}-${version}-${classifier}.jar`;
    validateNativeClassifierJar({
      classifier,
      jarPath: path.join(artifactDirectory, filename),
      expectedFilename: filename,
      repoRoot,
    });
  }

  return artifactDirectory;
}

function main() {
  const [repositoryPath, artifactId, version, repoRoot, signatures] =
    process.argv.slice(2);
  if (!repositoryPath || !artifactId || !version || !repoRoot) {
    console.error(
      "Usage: node validate-local-publication.mjs <repositoryPath> <artifactId> <version> <repoRoot> [--signatures]",
    );
    process.exitCode = 1;
    return;
  }

  try {
    const artifactDirectory = validateLocalPublication({
      artifactId,
      repositoryPath,
      repoRoot,
      requireSignatures: signatures === "--signatures",
      version,
    });
    console.log(`Validated local Maven publication: ${artifactDirectory}`);
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
