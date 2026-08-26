/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import {
  createNativeClassifierTestFixture,
  writeStoredZip,
} from "./create-native-classifier-test-fixture.mjs";
import {
  validateNativeClassifierJar,
  validatePlaceholderJar,
  validateSha256Manifest,
} from "./validate-native-artifact.mjs";
import { validateLocalPublication } from "./validate-local-publication.mjs";

const classifier = "win32-x64";
const artifactName = "copilot-sdk-java-runtime-1.2.3-win32-x64.jar";
const moduleRoot = fileURLToPath(new URL("../", import.meta.url));

test("accepts a matching, complete Windows classifier", (t) => {
  const fixture = createFixture(t);
  createNativeClassifierTestFixture({
    classifier,
    outputPath: fixture.jarPath,
    repoRoot: fixture.repoRoot,
  });

  assert.deepEqual(
    validateNativeClassifierJar({
      classifier,
      jarPath: fixture.jarPath,
      expectedFilename: artifactName,
      repoRoot: fixture.repoRoot,
    }),
    { classifier, nativeVersion: "9.8.7", sha256: undefined },
  );
});

test("accepts a matching, complete Darwin classifier", (t) => {
  const fixture = createFixture(t);
  const darwinClassifier = "darwin-arm64";
  const darwinArtifactName =
    "copilot-sdk-java-runtime-1.2.3-darwin-arm64.jar";
  const darwinJarPath = path.join(fixture.root, darwinArtifactName);
  createNativeClassifierTestFixture({
    classifier: darwinClassifier,
    outputPath: darwinJarPath,
    repoRoot: fixture.repoRoot,
  });

  assert.deepEqual(
    validateNativeClassifierJar({
      classifier: darwinClassifier,
      jarPath: darwinJarPath,
      expectedFilename: darwinArtifactName,
      repoRoot: fixture.repoRoot,
    }),
    {
      classifier: darwinClassifier,
      nativeVersion: "9.8.8",
      sha256: undefined,
    },
  );
});

test("rejects a wrong external filename before attachment", (t) => {
  const fixture = createFixture(t);
  const wrongName = path.join(fixture.root, "arbitrary.jar");
  createNativeClassifierTestFixture({
    classifier,
    outputPath: wrongName,
    repoRoot: fixture.repoRoot,
  });

  assert.throws(
    () =>
      validateNativeClassifierJar({
        classifier,
        jarPath: wrongName,
        expectedFilename: artifactName,
        repoRoot: fixture.repoRoot,
      }),
    /must be named/,
  );
});

test("rejects a missing classifier JAR with the expected filename", (t) => {
  const fixture = createFixture(t);

  assert.throws(
    () =>
      validateNativeClassifierJar({
        classifier,
        jarPath: fixture.jarPath,
        expectedFilename: artifactName,
        repoRoot: fixture.repoRoot,
      }),
    /does not exist/,
  );
});

test("rejects missing native resources", (t) => {
  const fixture = createFixture(t);
  writeStoredZip(fixture.jarPath, [
    ["native/win32-x64/runtime.node", "runtime"],
    [
      "native/win32-x64/platform.properties",
      "classifier=win32-x64\nversion=9.8.7\n",
    ],
  ]);

  assert.throws(
    () =>
      validateNativeClassifierJar({
        classifier,
        jarPath: fixture.jarPath,
        expectedFilename: artifactName,
        repoRoot: fixture.repoRoot,
      }),
    /copilot\.exe/,
  );
});

test("rejects incorrect pinned package metadata", (t) => {
  const fixture = createFixture(t);
  writeStoredZip(fixture.jarPath, [
    ["native/win32-x64/runtime.node", "runtime"],
    ["native/win32-x64/copilot.exe", "cli"],
    [
      "native/win32-x64/platform.properties",
      "classifier=win32-x64\nversion=0.0.1\n",
    ],
  ]);

  assert.throws(
    () =>
      validateNativeClassifierJar({
        classifier,
        jarPath: fixture.jarPath,
        expectedFilename: artifactName,
        repoRoot: fixture.repoRoot,
      }),
    /version=9\.8\.7/,
  );
});

test("rejects Linux resources in a Windows classifier", (t) => {
  const fixture = createFixture(t);
  createNativeClassifierTestFixture({
    classifier,
    outputPath: fixture.jarPath,
    repoRoot: fixture.repoRoot,
  });
  writeStoredZip(fixture.jarPath, [
    ["native/win32-x64/runtime.node", "runtime"],
    ["native/win32-x64/copilot.exe", "cli"],
    [
      "native/win32-x64/platform.properties",
      "classifier=win32-x64\nversion=9.8.7\n",
    ],
    ["native/linux-x64/runtime.node", "wrong platform"],
  ]);

  assert.throws(
    () =>
      validateNativeClassifierJar({
        classifier,
        jarPath: fixture.jarPath,
        expectedFilename: artifactName,
        repoRoot: fixture.repoRoot,
      }),
    /must not contain/,
  );
});

test("rejects Windows resources in a Linux classifier", (t) => {
  const fixture = createFixture(t);
  const linuxClassifier = "linux-x64";
  const linuxArtifactName =
    "copilot-sdk-java-runtime-1.2.3-linux-x64.jar";
  const linuxJarPath = path.join(fixture.root, linuxArtifactName);
  writeStoredZip(linuxJarPath, [
    ["native/linux-x64/runtime.node", "runtime"],
    ["native/linux-x64/copilot", "cli"],
    [
      "native/linux-x64/platform.properties",
      "classifier=linux-x64\nversion=9.8.6\n",
    ],
    ["native/win32-x64/runtime.node", "wrong platform"],
  ]);

  assert.throws(
    () =>
      validateNativeClassifierJar({
        classifier: linuxClassifier,
        jarPath: linuxJarPath,
        expectedFilename: linuxArtifactName,
        repoRoot: fixture.repoRoot,
      }),
    /must not contain/,
  );
});

test("rejects native resources in the placeholder JAR", (t) => {
  const fixture = createFixture(t);
  writeStoredZip(fixture.jarPath, [
    ["native/win32-x64/runtime.node", "runtime"],
  ]);

  assert.throws(
    () => validatePlaceholderJar(fixture.jarPath),
    /must not contain/,
  );
});

test("rejects Darwin native resources in the placeholder JAR", (t) => {
  const fixture = createFixture(t);
  writeStoredZip(fixture.jarPath, [
    ["native/darwin-arm64/runtime.node", "runtime"],
  ]);

  assert.throws(
    () => validatePlaceholderJar(fixture.jarPath),
    /must not contain/,
  );
});

test("accepts a native-free placeholder JAR", (t) => {
  const fixture = createFixture(t);
  writeStoredZip(fixture.jarPath, [
    ["META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n"],
  ]);

  assert.doesNotThrow(() => validatePlaceholderJar(fixture.jarPath));
});

test("rejects an invalid SHA-256 manifest", (t) => {
  const fixture = createFixture(t);
  createNativeClassifierTestFixture({
    classifier,
    outputPath: fixture.jarPath,
    repoRoot: fixture.repoRoot,
  });
  const manifestPath = path.join(fixture.root, "classifier.sha256");
  fs.writeFileSync(manifestPath, `${"0".repeat(64)}  ${artifactName}\n`);

  assert.throws(
    () =>
      validateSha256Manifest({
        expectedFilename: artifactName,
        jarPath: fixture.jarPath,
        manifestPath,
      }),
    /SHA-256 mismatch/,
  );
});

test("accepts a matching SHA-256 manifest", (t) => {
  const fixture = createFixture(t);
  createNativeClassifierTestFixture({
    classifier,
    outputPath: fixture.jarPath,
    repoRoot: fixture.repoRoot,
  });
  const manifestPath = path.join(fixture.root, "classifier.sha256");
  const digest = createHash("sha256")
    .update(fs.readFileSync(fixture.jarPath))
    .digest("hex");
  fs.writeFileSync(manifestPath, `${digest}  ${artifactName}\n`);

  assert.doesNotThrow(() =>
    validateSha256Manifest({
      expectedFilename: artifactName,
      jarPath: fixture.jarPath,
      manifestPath,
    }),
  );
});

test("validates one complete signed local publication", (t) => {
  const fixture = createFixture(t);
  const artifactId = "copilot-sdk-java-runtime";
  const version = "1.2.3";
  const publicationDirectory = path.join(
    fixture.root,
    "repository",
    "com",
    "github",
    artifactId,
    version,
  );
  fs.mkdirSync(publicationDirectory, { recursive: true });
  const primaryJar = `${artifactId}-${version}.jar`;
  const linuxJar = `${artifactId}-${version}-linux-x64.jar`;
  const windowsJar = `${artifactId}-${version}-win32-x64.jar`;
  const darwinJar = `${artifactId}-${version}-darwin-arm64.jar`;
  writeStoredZip(path.join(publicationDirectory, primaryJar), [
    ["META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n"],
  ]);
  writeStoredZip(
    path.join(publicationDirectory, `${artifactId}-${version}-sources.jar`),
    [],
  );
  writeStoredZip(
    path.join(publicationDirectory, `${artifactId}-${version}-javadoc.jar`),
    [],
  );
  createNativeClassifierTestFixture({
    classifier: "linux-x64",
    outputPath: path.join(publicationDirectory, linuxJar),
    repoRoot: fixture.repoRoot,
  });
  createNativeClassifierTestFixture({
    classifier,
    outputPath: path.join(publicationDirectory, windowsJar),
    repoRoot: fixture.repoRoot,
  });
  createNativeClassifierTestFixture({
    classifier: "darwin-arm64",
    outputPath: path.join(publicationDirectory, darwinJar),
    repoRoot: fixture.repoRoot,
  });
  fs.writeFileSync(
    path.join(publicationDirectory, `${artifactId}-${version}.pom`),
    "<project />",
  );
  for (const artifact of [
    primaryJar,
    `${artifactId}-${version}.pom`,
    `${artifactId}-${version}-sources.jar`,
    `${artifactId}-${version}-javadoc.jar`,
    linuxJar,
    windowsJar,
    darwinJar,
  ]) {
    fs.writeFileSync(
      path.join(publicationDirectory, `${artifact}.asc`),
      "signature",
    );
  }

  assert.equal(
    validateLocalPublication({
      artifactId,
      repositoryPath: path.join(fixture.root, "repository"),
      repoRoot: fixture.repoRoot,
      requireSignatures: true,
      version,
    }),
    publicationDirectory,
  );
});

test("local publication validation rejects cross-classifier contamination", (t) => {
  const fixture = createFixture(t);
  const artifactId = "copilot-sdk-java-runtime";
  const version = "1.2.3";
  const publicationDirectory = path.join(
    fixture.root,
    "repository",
    "com",
    "github",
    artifactId,
    version,
  );
  fs.mkdirSync(publicationDirectory, { recursive: true });

  writeStoredZip(
    path.join(publicationDirectory, `${artifactId}-${version}.jar`),
    [["META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n"]],
  );
  writeStoredZip(
    path.join(publicationDirectory, `${artifactId}-${version}-sources.jar`),
    [],
  );
  writeStoredZip(
    path.join(publicationDirectory, `${artifactId}-${version}-javadoc.jar`),
    [],
  );
  writeStoredZip(
    path.join(publicationDirectory, `${artifactId}-${version}-linux-x64.jar`),
    [
      ["native/linux-x64/runtime.node", "runtime"],
      ["native/linux-x64/copilot", "cli"],
      [
        "native/linux-x64/platform.properties",
        "classifier=linux-x64\nversion=9.8.6\n",
      ],
      ["native/win32-x64/runtime.node", "wrong platform"],
    ],
  );
  createNativeClassifierTestFixture({
    classifier: "win32-x64",
    outputPath: path.join(
      publicationDirectory,
      `${artifactId}-${version}-win32-x64.jar`,
    ),
    repoRoot: fixture.repoRoot,
  });
  createNativeClassifierTestFixture({
    classifier: "darwin-arm64",
    outputPath: path.join(
      publicationDirectory,
      `${artifactId}-${version}-darwin-arm64.jar`,
    ),
    repoRoot: fixture.repoRoot,
  });
  fs.writeFileSync(
    path.join(publicationDirectory, `${artifactId}-${version}.pom`),
    "<project />",
  );

  assert.throws(
    () =>
      validateLocalPublication({
        artifactId,
        repositoryPath: path.join(fixture.root, "repository"),
        repoRoot: fixture.repoRoot,
        requireSignatures: false,
        version,
      }),
    /must not contain/,
  );
});

function createFixture(t) {
  const fixtureParent = path.join(
    moduleRoot,
    "target",
    "validate-native-artifact-test-",
  );
  fs.mkdirSync(fixtureParent, { recursive: true });
  const root = fs.mkdtempSync(path.join(fixtureParent, `${process.pid}-`));
  t.after(() =>
    fs.rmSync(root, {
      recursive: true,
      force: true,
      maxRetries: 3,
      retryDelay: 100,
    }),
  );

  const repoRoot = path.join(root, "repo");
  fs.mkdirSync(path.join(repoRoot, "nodejs"), { recursive: true });
  fs.writeFileSync(
    path.join(repoRoot, "nodejs", "package-lock.json"),
    JSON.stringify({
      packages: {
        "node_modules/@github/copilot-win32-x64": { version: "9.8.7" },
        "node_modules/@github/copilot-linux-x64": { version: "9.8.6" },
        "node_modules/@github/copilot-darwin-arm64": { version: "9.8.8" },
      },
    }),
  );

  return {
    root,
    repoRoot,
    jarPath: path.join(root, artifactName),
  };
}
