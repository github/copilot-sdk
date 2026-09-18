/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFileSync } from "node:fs";
import { posix as path } from "node:path";
import { pathToFileURL } from "node:url";

const ELF64_HEADER_SIZE = 64;
const ELF64_PROGRAM_HEADER_SIZE = 56;
const ELFCLASS64 = 2;
const ELFDATA2LSB = 1;
const EV_CURRENT = 1;
const EM_X86_64 = 62;
const EM_AARCH64 = 183;
const PT_INTERP = 3;

const MUSL_ARCHITECTURES = {
  x64: {
    arch: "x64",
    displayArch: "x64",
    elfMachine: EM_X86_64,
    interpreter: "ld-musl-x86_64.so.1",
  },
  arm64: {
    arch: "arm64",
    displayArch: "ARM64",
    elfMachine: EM_AARCH64,
    interpreter: "ld-musl-aarch64.so.1",
  },
};

const MUSL_CLASSIFIERS = {
  "linuxmusl-x64": MUSL_ARCHITECTURES.x64,
  "linuxmusl-arm64": MUSL_ARCHITECTURES.arm64,
};

function readSafeInteger(buffer, offset, label) {
  const value = buffer.readBigUInt64LE(offset);
  if (value > BigInt(Number.MAX_SAFE_INTEGER)) {
    throw new Error(`${label} exceeds the supported size`);
  }
  return Number(value);
}

export function readElfInterpreter(buffer) {
  if (buffer.length < ELF64_HEADER_SIZE) {
    throw new Error("Node executable is too small to contain an ELF64 header");
  }
  if (
    buffer[0] !== 0x7f ||
    buffer[1] !== 0x45 ||
    buffer[2] !== 0x4c ||
    buffer[3] !== 0x46
  ) {
    throw new Error("Node executable is not an ELF file");
  }
  if (buffer[4] !== ELFCLASS64 || buffer[5] !== ELFDATA2LSB) {
    throw new Error("Node executable is not a little-endian ELF64 file");
  }
  if (buffer[6] !== EV_CURRENT || buffer.readUInt32LE(20) !== EV_CURRENT) {
    throw new Error("Node executable uses an unsupported ELF version");
  }

  const machine = buffer.readUInt16LE(18);
  const programHeaderOffset = readSafeInteger(
    buffer,
    32,
    "ELF program header offset",
  );
  const programHeaderEntrySize = buffer.readUInt16LE(54);
  const programHeaderCount = buffer.readUInt16LE(56);

  if (programHeaderCount === 0xffff) {
    throw new Error("Extended ELF program header counts are unsupported");
  }
  if (
    programHeaderCount > 0 &&
    programHeaderEntrySize < ELF64_PROGRAM_HEADER_SIZE
  ) {
    throw new Error("ELF program header entries are too small");
  }

  const programHeaderEnd =
    BigInt(programHeaderOffset) +
    BigInt(programHeaderEntrySize) * BigInt(programHeaderCount);
  if (programHeaderEnd > BigInt(buffer.length)) {
    throw new Error("ELF program header table extends beyond the executable");
  }

  let interpreter;
  for (let index = 0; index < programHeaderCount; index += 1) {
    const entryOffset = programHeaderOffset + index * programHeaderEntrySize;
    if (buffer.readUInt32LE(entryOffset) !== PT_INTERP) {
      continue;
    }
    if (interpreter !== undefined) {
      throw new Error("Node executable contains multiple ELF interpreters");
    }

    const interpreterOffset = readSafeInteger(
      buffer,
      entryOffset + 8,
      "ELF interpreter offset",
    );
    const interpreterSize = readSafeInteger(
      buffer,
      entryOffset + 32,
      "ELF interpreter size",
    );
    const interpreterEnd = BigInt(interpreterOffset) + BigInt(interpreterSize);
    if (interpreterSize < 2 || interpreterEnd > BigInt(buffer.length)) {
      throw new Error("ELF interpreter extends beyond the executable");
    }

    const bytes = buffer.subarray(
      interpreterOffset,
      interpreterOffset + interpreterSize,
    );
    if (
      bytes[bytes.length - 1] !== 0 ||
      bytes.subarray(0, bytes.length - 1).includes(0)
    ) {
      throw new Error("ELF interpreter is not a valid null-terminated path");
    }

    interpreter = bytes.subarray(0, bytes.length - 1).toString("utf8");
    if (!interpreter.startsWith("/")) {
      throw new Error("ELF interpreter path is not absolute");
    }
  }

  return { machine, interpreter };
}

export function validateNativeHost(classifier, host) {
  const muslHost = MUSL_CLASSIFIERS[classifier];
  if (muslHost) {
    if (host.platform !== "linux" || host.arch !== muslHost.arch) {
      throw new Error(
        `Native ${classifier} packaging requires Linux ${muslHost.displayArch}; detected ${host.platform}-${host.arch}`,
      );
    }
    if (host.glibcVersionRuntime) {
      throw new Error(
        `Native ${classifier} packaging requires musl; detected glibc ${host.glibcVersionRuntime}`,
      );
    }
    if (
      host.elfMachine !== muslHost.elfMachine ||
      path.basename(host.elfInterpreter ?? "") !== muslHost.interpreter
    ) {
      const detected = host.elfDetectionError
        ? `unable to inspect the Node executable: ${host.elfDetectionError}`
        : `${host.elfInterpreter ?? "no dynamic ELF interpreter"} (ELF machine ${host.elfMachine ?? "unknown"})`;
      throw new Error(
        `Native ${classifier} packaging requires musl; detected ${detected}`,
      );
    }
    return `Validated native build host: ${classifier} (musl)`;
  }

  if (classifier === "linux-x64" || classifier === "linux-arm64") {
    const expectedArch = classifier === "linux-x64" ? "x64" : "arm64";
    const displayArch = expectedArch === "x64" ? "x64" : "ARM64";
    if (host.platform !== "linux" || host.arch !== expectedArch) {
      throw new Error(
        `Native ${classifier} packaging requires Linux ${displayArch}; detected ${host.platform}-${host.arch}`,
      );
    }
    if (!host.glibcVersionRuntime) {
      throw new Error(
        `Native ${classifier} packaging requires glibc; musl and unknown libc hosts are unsupported`,
      );
    }
    return `Validated native build host: ${classifier} (glibc ${host.glibcVersionRuntime})`;
  }

  if (classifier === "win32-x64" || classifier === "win32-arm64") {
    const expectedArch = classifier === "win32-x64" ? "x64" : "arm64";
    const displayArch = expectedArch === "x64" ? "x64" : "ARM64";
    if (host.platform !== "win32" || host.arch !== expectedArch) {
      throw new Error(
        `Native ${classifier} packaging requires Windows ${displayArch}; detected ${host.platform}-${host.arch}`,
      );
    }
    return `Validated native build host: ${classifier}`;
  }

  if (classifier === "darwin-x64" || classifier === "darwin-arm64") {
    const expectedArch = classifier === "darwin-x64" ? "x64" : "arm64";
    const displayArch = expectedArch === "x64" ? "x64" : "ARM64";
    if (host.platform !== "darwin" || host.arch !== expectedArch) {
      throw new Error(
        `Native ${classifier} packaging requires macOS ${displayArch}; detected ${host.platform}-${host.arch}`,
      );
    }
    return `Validated native build host: ${classifier}`;
  }

  throw new Error(`Unsupported native build classifier: ${classifier}`);
}

export function detectNativeHost() {
  const report = process.report?.getReport();
  let elf;
  let elfDetectionError;
  if (process.platform === "linux") {
    try {
      elf = readElfInterpreter(readFileSync("/proc/self/exe"));
    } catch (error) {
      elfDetectionError =
        error instanceof Error ? error.message : String(error);
    }
  }
  return {
    platform: process.platform,
    arch: process.arch,
    glibcVersionRuntime: report?.header?.glibcVersionRuntime,
    elfMachine: elf?.machine,
    elfInterpreter: elf?.interpreter,
    elfDetectionError,
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
