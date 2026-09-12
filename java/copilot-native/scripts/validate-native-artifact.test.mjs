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

test("accepts a matching, complete Windows ARM64 classifier", (t) => {
  const fixture = createFixture(t);
  const windowsArm64Classifier = "win32-arm64";
  const windowsArm64ArtifactName =
    "copilot-sdk-java-runtime-1.2.3-win32-arm64.jar";
  const windowsArm64JarPath = path.join(
    fixture.root,
    windowsArm64ArtifactName,
  );
  createNativeClassifierTestFixture({
    classifier: windowsArm64Classifier,
    outputPath: windowsArm64JarPath,
    repoRoot: fixture.repoRoot,
  });

  assert.deepEqual(
    validateNativeClassifierJar({
      classifier: windowsArm64Classifier,
      jarPath: windowsArm64JarPath,
      expectedFilename: windowsArm64ArtifactName,
      repoRoot: fixture.repoRoot,
    }),
    {
      classifier: windowsArm64Classifier,
      nativeVersion: "9.8.7",
      sha256: undefined,
    },
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
      nativeVersion: "9.8.7",
      sha256: undefined,
    },
  );
});

test("accepts a matching, complete Linux ARM64 classifier", (t) => {
  const fixture = createFixture(t);
  const linuxArm64Classifier = "linux-arm64";
  const linuxArm64ArtifactName =
    "copilot-sdk-java-runtime-1.2.3-linux-arm64.jar";
  const linuxArm64JarPath = path.join(fixture.root, linuxArm64ArtifactName);
  createNativeClassifierTestFixture({
    classifier: linuxArm64Classifier,
    outputPath: linuxArm64JarPath,
    repoRoot: fixture.repoRoot,
  });

  assert.deepEqual(
    validateNativeClassifierJar({
      classifier: linuxArm64Classifier,
      jarPath: linuxArm64JarPath,
      expectedFilename: linuxArm64ArtifactName,
      repoRoot: fixture.repoRoot,
    }),
    {
      classifier: linuxArm64Classifier,
      nativeVersion: "9.8.7",
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
    /copilot-runtime\.exe/,
  );
});

test("rejects incorrect pinned package metadata", (t) => {
  const fixture = createFixture(t);
  writeStoredZip(fixture.jarPath, [
    ["native/win32-x64/runtime.node", "runtime"],
    ["native/win32-x64/copilot-runtime.exe", "runtime wrapper"],
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
    ["native/win32-x64/copilot-runtime.exe", "runtime wrapper"],
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
    ["native/linux-x64/copilot-runtime", "runtime wrapper"],
    [
      "native/linux-x64/platform.properties",
      "classifier=linux-x64\nversion=9.8.7\n",
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

for (const artifactId of ["copilot-sdk-java-runtime", "copilot-sdk-java"]) {
  for (const version of ["0.0.0-ci", "1.2.3-SNAPSHOT"]) {
    test(`validates signed ${artifactId} publication at ${version}`, (t) => {
      const fixture = createPublicationFixture(t, { artifactId, version });

      assert.equal(
        validateLocalPublication({ ...fixture, requireSignatures: true }),
        fixture.publicationDirectory,
      );
    });
  }

  for (const { name, from, to, error } of [
    {
      name: "unresolved revision",
      from: "<version>1.2.3</version>",
      to: "<version>${revision}</version>",
      error: /unresolved.*revision/,
    },
    {
      name: "the committed snapshot instead of the release version",
      from: "<version>1.2.3</version>",
      to: "<version>1.2.3-SNAPSHOT</version>",
      error: /Unexpected Maven coordinates/,
    },
    {
      name: "an incorrect group",
      from: "<groupId>com.github</groupId>",
      to: "<groupId>org.example</groupId>",
      error: /Unexpected Maven coordinates/,
    },
    {
      name: "an incorrect artifact",
      from: `<artifactId>${artifactId}</artifactId>`,
      to: "<artifactId>different-artifact</artifactId>",
      error: /Unexpected Maven coordinates/,
    },
    {
      name: "a dependency version in place of the project version",
      from: "<version>1.2.3</version>",
      to: "<dependencies><dependency><version>1.2.3</version></dependency></dependencies>",
      error: /missing flattened project coordinates/,
    },
    {
      name: "a commented-out project version",
      from: "<version>1.2.3</version>",
      to: "<!-- <version>1.2.3</version> -->",
      error: /missing flattened project coordinates/,
    },
    {
      name: "an unpublished parent reference",
      from: "<modelVersion>4.0.0</modelVersion>",
      to: `<modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java-parent</artifactId>
    <version>1.2.3</version>
  </parent>`,
      error: /must not depend on a parent/,
    },
    {
      name: "unresolved revision in a dependency",
      from: "</project>",
      to: "<dependencies><dependency><version>${revision}</version></dependency></dependencies></project>",
      error: /unresolved.*revision/,
    },
  ]) {
    test(`${artifactId} publication rejects ${name}`, (t) => {
      const fixture = createPublicationFixture(t, { artifactId });
      const pom = fs.readFileSync(fixture.pomPath, "utf8");
      assert.ok(pom.includes(from));
      fs.writeFileSync(fixture.pomPath, pom.replace(from, to));

      assert.throws(() => validateLocalPublication(fixture), error);
    });
  }
}

test("local release publication requires a POM signature", (t) => {
  const fixture = createPublicationFixture(t);
  fs.rmSync(`${fixture.pomPath}.asc`);

  assert.throws(
    () => validateLocalPublication({ ...fixture, requireSignatures: true }),
    /missing signature.*\.pom\.asc/,
  );
});

test("local publication validation rejects cross-classifier contamination", (t) => {
  const fixture = createPublicationFixture(t);
  writeStoredZip(
    path.join(
      fixture.publicationDirectory,
      `${fixture.artifactId}-${fixture.version}-linux-x64.jar`,
    ),
    [
      ["native/linux-x64/runtime.node", "runtime"],
      ["native/linux-x64/copilot-runtime", "runtime wrapper"],
      [
        "native/linux-x64/platform.properties",
        "classifier=linux-x64\nversion=9.8.7\n",
      ],
      ["native/win32-x64/runtime.node", "wrong platform"],
    ],
  );

  assert.throws(() => validateLocalPublication(fixture), /must not contain/);
});

function createPublicationFixture(
  t,
  { artifactId = "copilot-sdk-java-runtime", version = "1.2.3" } = {},
) {
  const fixture = createFixture(t);
  const repositoryPath = path.join(fixture.root, "repository");
  const publicationDirectory = path.join(
    repositoryPath,
    "com",
    "github",
    artifactId,
    version,
  );
  fs.mkdirSync(publicationDirectory, { recursive: true });
  const pomPath = path.join(
    publicationDirectory,
    `${artifactId}-${version}.pom`,
  );
  fs.writeFileSync(
    pomPath,
    `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.github</groupId>
  <artifactId>${artifactId}</artifactId>
  <version>${version}</version>
</project>`,
  );
  for (const suffix of ["", "-sources", "-javadoc"]) {
    writeStoredZip(
      path.join(publicationDirectory, `${artifactId}-${version}${suffix}.jar`),
      [["META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n"]],
    );
  }
  if (artifactId === "copilot-sdk-java-runtime") {
    for (const nativeClassifier of [
      "linux-x64",
      "linux-arm64",
      "win32-x64",
      "win32-arm64",
      "darwin-arm64",
    ]) {
      createNativeClassifierTestFixture({
        classifier: nativeClassifier,
        outputPath: path.join(
          publicationDirectory,
          `${artifactId}-${version}-${nativeClassifier}.jar`,
        ),
        repoRoot: fixture.repoRoot,
      });
    }
  }
  for (const artifact of fs.readdirSync(publicationDirectory)) {
    fs.writeFileSync(
      path.join(publicationDirectory, `${artifact}.asc`),
      "signature",
    );
  }

  return {
    ...fixture,
    artifactId,
    version,
    repositoryPath,
    publicationDirectory,
    pomPath,
  };
}

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
    path.join(repoRoot, "nodejs", "package.json"),
    JSON.stringify({ copilotCliVersion: "9.8.7" }),
  );

  return {
    root,
    repoRoot,
    jarPath: path.join(root, artifactName),
  };
}
