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
  if (
    artifactId !== "copilot-sdk-java" &&
    artifactId !== "copilot-sdk-java-runtime"
  ) {
    throw new Error(`Unsupported Java SDK artifact: ${artifactId}`);
  }
  const classifiers =
    artifactId === "copilot-sdk-java-runtime"
      ? ["linux-x64", "linux-arm64", "win32-x64", "win32-arm64", "darwin-arm64"]
      : [];
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
    ...classifiers.map(
      (classifier) => `${artifactId}-${version}-${classifier}.jar`,
    ),
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

  validatePublishedPom({
    artifactId,
    pomPath: path.join(artifactDirectory, `${artifactId}-${version}.pom`),
    version,
  });
  validatePlaceholderJar(
    path.join(artifactDirectory, `${artifactId}-${version}.jar`),
  );
  for (const classifier of classifiers) {
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

function validatePublishedPom({ artifactId, pomPath, version }) {
  const pom = fs.readFileSync(pomPath, "utf8").replace(/<!--[\s\S]*?-->/g, "");
  if (pom.includes("${revision}")) {
    throw new Error(
      `Published POM contains unresolved \${revision}: ${pomPath}`,
    );
  }
  if (/<parent(?:\s|\/?>)/.test(pom)) {
    throw new Error(`Published POM must not depend on a parent: ${pomPath}`);
  }

  // Maven writes the flattened project's coordinates before nested elements.
  // Match that header so dependency coordinates cannot satisfy this check.
  const coordinates = pom.match(
    /<project\b[^>]*>\s*<modelVersion>[^<]+<\/modelVersion>\s*<groupId>([^<]+)<\/groupId>\s*<artifactId>([^<]+)<\/artifactId>\s*<version>([^<]+)<\/version>/,
  );
  if (!coordinates) {
    throw new Error(
      `Published POM is missing flattened project coordinates: ${pomPath}`,
    );
  }
  const [, groupId, publishedArtifactId, publishedVersion] = coordinates.map(
    (value) => value.trim(),
  );
  if (
    groupId !== "com.github" ||
    publishedArtifactId !== artifactId ||
    publishedVersion !== version
  ) {
    throw new Error(
      `Unexpected Maven coordinates in ${pomPath}: ${groupId}:${publishedArtifactId}:${publishedVersion} (expected com.github:${artifactId}:${version})`,
    );
  }
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
