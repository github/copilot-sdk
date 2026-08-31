/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

import { readPinnedNativeVersion } from "./validate-native-artifact.mjs";

export function createNativeClassifierTestFixture({
  classifier,
  outputPath,
  repoRoot,
}) {
  const cliFilename = classifier.startsWith("win32")
    ? "copilot.exe"
    : "copilot";
  const nativeVersion = readPinnedNativeVersion(repoRoot, classifier);
  const prefix = `native/${classifier}`;
  writeStoredZip(outputPath, [
    [`${prefix}/runtime.node`, "test runtime"],
    [`${prefix}/${cliFilename}`, "test cli"],
    [
      `${prefix}/platform.properties`,
      `classifier=${classifier}\nversion=${nativeVersion}\n`,
    ],
  ]);
}

export function writeStoredZip(outputPath, entries) {
  const localRecords = [];
  const centralRecords = [];
  let offset = 0;

  for (const [filename, contents] of entries) {
    const filenameBuffer = Buffer.from(filename);
    const contentsBuffer = Buffer.isBuffer(contents)
      ? contents
      : Buffer.from(contents);
    const crc = crc32(contentsBuffer);
    const localHeader = Buffer.alloc(30);
    localHeader.writeUInt32LE(0x04034b50, 0);
    localHeader.writeUInt16LE(20, 4);
    localHeader.writeUInt32LE(crc, 14);
    localHeader.writeUInt32LE(contentsBuffer.length, 18);
    localHeader.writeUInt32LE(contentsBuffer.length, 22);
    localHeader.writeUInt16LE(filenameBuffer.length, 26);
    const localRecord = Buffer.concat([
      localHeader,
      filenameBuffer,
      contentsBuffer,
    ]);
    localRecords.push(localRecord);

    const centralHeader = Buffer.alloc(46);
    centralHeader.writeUInt32LE(0x02014b50, 0);
    centralHeader.writeUInt16LE(20, 4);
    centralHeader.writeUInt16LE(20, 6);
    centralHeader.writeUInt32LE(crc, 16);
    centralHeader.writeUInt32LE(contentsBuffer.length, 20);
    centralHeader.writeUInt32LE(contentsBuffer.length, 24);
    centralHeader.writeUInt16LE(filenameBuffer.length, 28);
    centralHeader.writeUInt32LE(offset, 42);
    centralRecords.push(Buffer.concat([centralHeader, filenameBuffer]));
    offset += localRecord.length;
  }

  const centralDirectory = Buffer.concat(centralRecords);
  const endOfCentralDirectory = Buffer.alloc(22);
  endOfCentralDirectory.writeUInt32LE(0x06054b50, 0);
  endOfCentralDirectory.writeUInt16LE(entries.length, 8);
  endOfCentralDirectory.writeUInt16LE(entries.length, 10);
  endOfCentralDirectory.writeUInt32LE(centralDirectory.length, 12);
  endOfCentralDirectory.writeUInt32LE(offset, 16);

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(
    outputPath,
    Buffer.concat([...localRecords, centralDirectory, endOfCentralDirectory]),
  );
}

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) {
      crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function main() {
  const [classifier, outputPath, repoRoot] = process.argv.slice(2);
  if (!classifier || !outputPath || !repoRoot) {
    console.error(
      "Usage: node create-native-classifier-test-fixture.mjs <classifier> <outputJar> <repoRoot>",
    );
    process.exitCode = 1;
    return;
  }

  try {
    createNativeClassifierTestFixture({ classifier, outputPath, repoRoot });
    console.log(`Created ${classifier} test fixture: ${outputPath}`);
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
