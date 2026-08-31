/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createHash } from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";
import zlib from "node:zlib";

const END_OF_CENTRAL_DIRECTORY = 0x06054b50;
const CENTRAL_DIRECTORY_FILE_HEADER = 0x02014b50;
const LOCAL_FILE_HEADER = 0x04034b50;
const PLATFORM_CLASSIFIER =
  /^(?:linux(?:musl)?-(?:x64|arm64)|darwin-(?:x64|arm64)|win32-(?:x64|arm64))$/;

export function validateNativeClassifierJar({
  classifier,
  jarPath,
  expectedFilename,
  repoRoot,
}) {
  if (path.basename(jarPath) !== expectedFilename) {
    throw new Error(
      `External ${classifier} classifier must be named ${expectedFilename}; received ${path.basename(jarPath)}`,
    );
  }

  const archive = readJar(jarPath);
  const cliFilename = classifier.startsWith("win32")
    ? "copilot.exe"
    : "copilot";
  const resourcePrefix = `native/${classifier}/`;
  const requiredEntries = [
    `${resourcePrefix}runtime.node`,
    `${resourcePrefix}${cliFilename}`,
    `${resourcePrefix}platform.properties`,
  ];

  for (const entryName of requiredEntries) {
    const entry = archive.entries.get(entryName);
    if (!entry || entry.uncompressedSize === 0) {
      throw new Error(
        `External ${classifier} classifier is missing nonempty ${entryName}`,
      );
    }
  }

  for (const entryName of archive.entries.keys()) {
    const nativeClassifier = /^native\/([^/]+)\//.exec(entryName)?.[1];
    if (nativeClassifier && nativeClassifier !== classifier) {
      throw new Error(
        `External ${classifier} classifier must not contain ${entryName}`,
      );
    }
  }

  const properties = parseProperties(
    readEntry(archive, `${resourcePrefix}platform.properties`),
  );
  const expectedVersion = readPinnedNativeVersion(repoRoot, classifier);
  if (properties.classifier !== classifier) {
    throw new Error(
      `External classifier metadata must contain classifier=${classifier}; received ${properties.classifier ?? "none"}`,
    );
  }
  if (properties.version !== expectedVersion) {
    throw new Error(
      `External ${classifier} classifier metadata must contain version=${expectedVersion}; received ${properties.version ?? "none"}`,
    );
  }

  return {
    classifier,
    nativeVersion: expectedVersion,
    sha256: undefined,
  };
}

export function validatePlaceholderJar(jarPath) {
  const archive = readJar(jarPath);
  for (const entryName of archive.entries.keys()) {
    const nativeClassifier = /^native\/([^/]+)\//.exec(entryName)?.[1];
    if (nativeClassifier && PLATFORM_CLASSIFIER.test(nativeClassifier)) {
      throw new Error(
        `Placeholder primary JAR must not contain platform-native resources; found ${entryName}`,
      );
    }
  }
}

export function validateSha256Manifest({
  expectedFilename,
  jarPath,
  manifestPath,
}) {
  let manifest;
  try {
    manifest = fs.readFileSync(manifestPath, "utf8").trim();
  } catch (error) {
    throw new Error(
      `Could not read SHA-256 manifest ${manifestPath}: ${error.message}`,
    );
  }
  const match = /^([a-fA-F0-9]{64}) {2}(.+)$/.exec(manifest);
  if (!match || match[2] !== expectedFilename) {
    throw new Error(
      `SHA-256 manifest must contain one checksum for ${expectedFilename}: ${manifestPath}`,
    );
  }
  const actual = createHash("sha256")
    .update(fs.readFileSync(jarPath))
    .digest("hex");
  if (actual !== match[1].toLowerCase()) {
    throw new Error(
      `SHA-256 mismatch for ${expectedFilename}; expected ${match[1].toLowerCase()}, received ${actual}`,
    );
  }
}

export function readPinnedNativeVersion(repoRoot, classifier) {
  const packageName = `@github/copilot-${classifier}`;
  const lockPath = path.join(repoRoot, "nodejs", "package-lock.json");
  let lock;
  try {
    lock = JSON.parse(fs.readFileSync(lockPath, "utf8"));
  } catch (error) {
    throw new Error(
      `Could not read pinned ${packageName} version from ${lockPath}: ${error.message}`,
    );
  }

  const version = lock.packages?.[`node_modules/${packageName}`]?.version;
  if (!version) {
    throw new Error(
      `Could not find pinned ${packageName} version in ${lockPath}`,
    );
  }
  return version;
}

function readJar(jarPath) {
  let stat;
  try {
    stat = fs.statSync(jarPath);
  } catch (error) {
    throw new Error(`External classifier JAR does not exist: ${jarPath}`);
  }
  if (!stat.isFile() || path.extname(jarPath).toLowerCase() !== ".jar") {
    throw new Error(
      `External classifier must be a regular .jar file: ${jarPath}`,
    );
  }

  const data = fs.readFileSync(jarPath);
  const eocdOffset = findEndOfCentralDirectory(data);
  const entryCount = data.readUInt16LE(eocdOffset + 10);
  const centralDirectorySize = data.readUInt32LE(eocdOffset + 12);
  const centralDirectoryOffset = data.readUInt32LE(eocdOffset + 16);
  if (entryCount === 0xffff || centralDirectoryOffset === 0xffffffff) {
    throw new Error(`ZIP64 classifier JARs are not supported: ${jarPath}`);
  }
  if (centralDirectoryOffset + centralDirectorySize > data.length) {
    throw new Error(
      `Classifier JAR has an invalid central directory: ${jarPath}`,
    );
  }

  const entries = new Map();
  let offset = centralDirectoryOffset;
  for (let index = 0; index < entryCount; index += 1) {
    if (readUInt32(data, offset, jarPath) !== CENTRAL_DIRECTORY_FILE_HEADER) {
      throw new Error(
        `Classifier JAR has an invalid central directory entry: ${jarPath}`,
      );
    }
    const compressionMethod = data.readUInt16LE(offset + 10);
    const compressedSize = data.readUInt32LE(offset + 20);
    const uncompressedSize = data.readUInt32LE(offset + 24);
    const filenameLength = data.readUInt16LE(offset + 28);
    const extraLength = data.readUInt16LE(offset + 30);
    const commentLength = data.readUInt16LE(offset + 32);
    const localHeaderOffset = data.readUInt32LE(offset + 42);
    const filenameOffset = offset + 46;
    const nextOffset =
      filenameOffset + filenameLength + extraLength + commentLength;
    if (nextOffset > data.length) {
      throw new Error(
        `Classifier JAR has a truncated central directory entry: ${jarPath}`,
      );
    }

    const filename = data.toString(
      "utf8",
      filenameOffset,
      filenameOffset + filenameLength,
    );
    if (entries.has(filename)) {
      throw new Error(
        `Classifier JAR contains duplicate entry ${filename}: ${jarPath}`,
      );
    }
    entries.set(filename, {
      compressionMethod,
      compressedSize,
      uncompressedSize,
      localHeaderOffset,
    });
    offset = nextOffset;
  }

  return { data, entries, jarPath };
}

function findEndOfCentralDirectory(data) {
  const minimumSize = 22;
  const minimumOffset = Math.max(0, data.length - minimumSize - 0xffff);
  for (
    let offset = data.length - minimumSize;
    offset >= minimumOffset;
    offset -= 1
  ) {
    if (
      data.readUInt32LE(offset) === END_OF_CENTRAL_DIRECTORY &&
      offset + minimumSize + data.readUInt16LE(offset + 20) === data.length
    ) {
      return offset;
    }
  }
  throw new Error("Classifier JAR is not a valid ZIP archive");
}

function readEntry(archive, entryName) {
  const entry = archive.entries.get(entryName);
  if (!entry) {
    throw new Error(`Classifier JAR is missing ${entryName}`);
  }
  if (entry.uncompressedSize > 1024 * 1024) {
    throw new Error(
      `Classifier JAR metadata entry is unexpectedly large: ${entryName}`,
    );
  }

  const { data, jarPath } = archive;
  const localOffset = entry.localHeaderOffset;
  if (readUInt32(data, localOffset, jarPath) !== LOCAL_FILE_HEADER) {
    throw new Error(
      `Classifier JAR has an invalid local entry for ${entryName}`,
    );
  }
  const filenameLength = data.readUInt16LE(localOffset + 26);
  const extraLength = data.readUInt16LE(localOffset + 28);
  const contentsOffset = localOffset + 30 + filenameLength + extraLength;
  const contentsEnd = contentsOffset + entry.compressedSize;
  if (contentsEnd > data.length) {
    throw new Error(
      `Classifier JAR has a truncated local entry for ${entryName}`,
    );
  }

  const compressed = data.subarray(contentsOffset, contentsEnd);
  let contents;
  if (entry.compressionMethod === 0) {
    contents = compressed;
  } else if (entry.compressionMethod === 8) {
    contents = zlib.inflateRawSync(compressed);
  } else {
    throw new Error(
      `Classifier JAR uses unsupported compression for ${entryName}: ${entry.compressionMethod}`,
    );
  }
  if (contents.length !== entry.uncompressedSize) {
    throw new Error(`Classifier JAR has an invalid size for ${entryName}`);
  }
  return contents.toString("utf8");
}

function parseProperties(contents) {
  const properties = {};
  for (const line of contents.split(/\r?\n/)) {
    const separator = line.indexOf("=");
    if (separator > 0) {
      properties[line.slice(0, separator).trim()] = line
        .slice(separator + 1)
        .trim();
    }
  }
  return properties;
}

function readUInt32(data, offset, jarPath) {
  if (offset + 4 > data.length) {
    throw new Error(`Classifier JAR is truncated: ${jarPath}`);
  }
  return data.readUInt32LE(offset);
}

function main() {
  const [mode, ...arguments_] = process.argv.slice(2);
  try {
    if (mode === "classifier" && arguments_.length === 4) {
      const [classifier, jarPath, expectedFilename, repoRoot] = arguments_;
      const result = validateNativeClassifierJar({
        classifier,
        jarPath,
        expectedFilename,
        repoRoot,
      });
      console.log(
        `Validated ${result.classifier} classifier JAR (${result.nativeVersion}): ${jarPath}`,
      );
      return;
    }
    if (mode === "placeholder" && arguments_.length === 1) {
      validatePlaceholderJar(arguments_[0]);
      console.log(`Validated native-free placeholder JAR: ${arguments_[0]}`);
      return;
    }
    if (mode === "checksum" && arguments_.length === 3) {
      const [jarPath, manifestPath, expectedFilename] = arguments_;
      validateSha256Manifest({ expectedFilename, jarPath, manifestPath });
      console.log(
        `Validated SHA-256 manifest for ${expectedFilename}: ${manifestPath}`,
      );
      return;
    }
    throw new Error(
      "Usage: node validate-native-artifact.mjs classifier <classifier> <jar> <expectedFilename> <repoRoot> | placeholder <jar> | checksum <jar> <manifest> <expectedFilename>",
    );
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
