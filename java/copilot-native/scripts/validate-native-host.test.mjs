/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import test from "node:test";

import {
  readElfInterpreter,
  validateNativeHost,
} from "./validate-native-host.mjs";

function createElf64({ machine = 62, interpreter } = {}) {
  const interpreterBytes =
    interpreter === undefined
      ? undefined
      : Buffer.from(`${interpreter}\0`, "utf8");
  const programHeaderCount = interpreterBytes === undefined ? 0 : 1;
  const interpreterOffset = ELF64_HEADER_SIZE + ELF64_PROGRAM_HEADER_SIZE;
  const buffer = Buffer.alloc(
    interpreterOffset + (interpreterBytes?.length ?? 0),
  );

  buffer.set([0x7f, 0x45, 0x4c, 0x46], 0);
  buffer[4] = 2;
  buffer[5] = 1;
  buffer[6] = 1;
  buffer.writeUInt16LE(machine, 18);
  buffer.writeUInt32LE(1, 20);
  buffer.writeBigUInt64LE(
    BigInt(programHeaderCount === 0 ? 0 : ELF64_HEADER_SIZE),
    32,
  );
  buffer.writeUInt16LE(ELF64_HEADER_SIZE, 52);
  buffer.writeUInt16LE(ELF64_PROGRAM_HEADER_SIZE, 54);
  buffer.writeUInt16LE(programHeaderCount, 56);

  if (interpreterBytes !== undefined) {
    buffer.writeUInt32LE(3, ELF64_HEADER_SIZE);
    buffer.writeBigUInt64LE(BigInt(interpreterOffset), ELF64_HEADER_SIZE + 8);
    buffer.writeBigUInt64LE(
      BigInt(interpreterBytes.length),
      ELF64_HEADER_SIZE + 32,
    );
    interpreterBytes.copy(buffer, interpreterOffset);
  }

  return buffer;
}

const ELF64_HEADER_SIZE = 64;
const ELF64_PROGRAM_HEADER_SIZE = 56;

test("reads the x64 musl interpreter from an ELF executable", () => {
  assert.deepEqual(
    readElfInterpreter(
      createElf64({
        interpreter: "/lib/ld-musl-x86_64.so.1",
      }),
    ),
    {
      machine: 62,
      interpreter: "/lib/ld-musl-x86_64.so.1",
    },
  );
});

test("reads the ARM64 musl interpreter from an ELF executable", () => {
  assert.deepEqual(
    readElfInterpreter(
      createElf64({
        machine: 183,
        interpreter: "/lib/ld-musl-aarch64.so.1",
      }),
    ),
    {
      machine: 183,
      interpreter: "/lib/ld-musl-aarch64.so.1",
    },
  );
});

test("reports a static ELF executable without an interpreter", () => {
  assert.deepEqual(readElfInterpreter(createElf64()), {
    machine: 62,
    interpreter: undefined,
  });
});

test("rejects a truncated ELF executable", () => {
  assert.throws(
    () => readElfInterpreter(Buffer.from([0x7f, 0x45, 0x4c, 0x46])),
    /too small/,
  );
});

test("rejects an ELF interpreter outside the executable", () => {
  const buffer = createElf64({
    interpreter: "/lib/ld-musl-x86_64.so.1",
  });
  buffer.writeBigUInt64LE(BigInt(buffer.length + 1), ELF64_HEADER_SIZE + 8);

  assert.throws(() => readElfInterpreter(buffer), /interpreter extends beyond/);
});

test("accepts Linux x64 with glibc", () => {
  assert.equal(
    validateNativeHost("linux-x64", {
      platform: "linux",
      arch: "x64",
      glibcVersionRuntime: "2.39",
    }),
    "Validated native build host: linux-x64 (glibc 2.39)",
  );
});

test("accepts Linux ARM64 with glibc", () => {
  assert.equal(
    validateNativeHost("linux-arm64", {
      platform: "linux",
      arch: "arm64",
      glibcVersionRuntime: "2.39",
    }),
    "Validated native build host: linux-arm64 (glibc 2.39)",
  );
});

test("accepts Linux musl x64", () => {
  assert.equal(
    validateNativeHost("linuxmusl-x64", {
      platform: "linux",
      arch: "x64",
      glibcVersionRuntime: undefined,
      elfMachine: 62,
      elfInterpreter: "/lib/ld-musl-x86_64.so.1",
    }),
    "Validated native build host: linuxmusl-x64 (musl)",
  );
});

test("accepts Linux musl ARM64", () => {
  assert.equal(
    validateNativeHost("linuxmusl-arm64", {
      platform: "linux",
      arch: "arm64",
      glibcVersionRuntime: undefined,
      elfMachine: 183,
      elfInterpreter: "/lib/ld-musl-aarch64.so.1",
    }),
    "Validated native build host: linuxmusl-arm64 (musl)",
  );
});

test("accepts Windows x64 without a libc requirement", () => {
  assert.equal(
    validateNativeHost("win32-x64", {
      platform: "win32",
      arch: "x64",
      glibcVersionRuntime: undefined,
    }),
    "Validated native build host: win32-x64",
  );
});

test("accepts Windows ARM64 without a libc requirement", () => {
  assert.equal(
    validateNativeHost("win32-arm64", {
      platform: "win32",
      arch: "arm64",
      glibcVersionRuntime: undefined,
    }),
    "Validated native build host: win32-arm64",
  );
});

test("accepts macOS ARM64 without a libc requirement", () => {
  assert.equal(
    validateNativeHost("darwin-arm64", {
      platform: "darwin",
      arch: "arm64",
      glibcVersionRuntime: undefined,
    }),
    "Validated native build host: darwin-arm64",
  );
});

test("accepts macOS x64 without a libc requirement", () => {
  assert.equal(
    validateNativeHost("darwin-x64", {
      platform: "darwin",
      arch: "x64",
      glibcVersionRuntime: undefined,
    }),
    "Validated native build host: darwin-x64",
  );
});

test("rejects Linux x64 with musl or unknown libc", () => {
  assert.throws(
    () =>
      validateNativeHost("linux-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: undefined,
      }),
    /requires glibc/,
  );
});

test("rejects Linux ARM64 with musl or unknown libc", () => {
  assert.throws(
    () =>
      validateNativeHost("linux-arm64", {
        platform: "linux",
        arch: "arm64",
        glibcVersionRuntime: undefined,
      }),
    /requires glibc/,
  );
});

test("rejects Linux musl x64 with glibc", () => {
  assert.throws(
    () =>
      validateNativeHost("linuxmusl-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: "2.39",
        elfMachine: 62,
        elfInterpreter: "/lib64/ld-linux-x86-64.so.2",
      }),
    /requires musl/,
  );
});

test("rejects Linux musl x64 when the glibc report is unavailable", () => {
  assert.throws(
    () =>
      validateNativeHost("linuxmusl-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: undefined,
        elfMachine: 62,
        elfInterpreter: "/lib64/ld-linux-x86-64.so.2",
      }),
    /ld-linux-x86-64/,
  );
});

test("rejects Linux musl x64 with unknown libc", () => {
  assert.throws(
    () =>
      validateNativeHost("linuxmusl-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: undefined,
        elfMachine: 62,
        elfInterpreter: undefined,
      }),
    /no dynamic ELF interpreter/,
  );
});

test("rejects Linux musl x64 with the ARM64 musl interpreter", () => {
  assert.throws(
    () =>
      validateNativeHost("linuxmusl-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: undefined,
        elfMachine: 183,
        elfInterpreter: "/lib/ld-musl-aarch64.so.1",
      }),
    /ld-musl-aarch64/,
  );
});

test("rejects Linux musl x64 when ELF detection fails", () => {
  assert.throws(
    () =>
      validateNativeHost("linuxmusl-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: undefined,
        elfDetectionError: "permission denied",
      }),
    /unable to inspect the Node executable: permission denied/,
  );
});

test("rejects a non-x64 host for Linux musl x64", () => {
  assert.throws(
    () =>
      validateNativeHost("linuxmusl-x64", {
        platform: "linux",
        arch: "arm64",
        glibcVersionRuntime: undefined,
      }),
    /requires Linux x64/,
  );
});

test("rejects a non-Linux host", () => {
  assert.throws(
    () =>
      validateNativeHost("linux-x64", {
        platform: "darwin",
        arch: "x64",
        glibcVersionRuntime: undefined,
      }),
    /requires Linux x64/,
  );
});

test("rejects a non-x64 host", () => {
  assert.throws(
    () =>
      validateNativeHost("linux-x64", {
        platform: "linux",
        arch: "arm64",
        glibcVersionRuntime: "2.39",
      }),
    /requires Linux x64/,
  );
});

test("rejects Linux x64 for the Linux ARM64 classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("linux-arm64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: "2.39",
      }),
    /requires Linux ARM64/,
  );
});

test("rejects a non-Windows host for the Windows classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("win32-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: "2.39",
      }),
    /requires Windows x64/,
  );
});

test("rejects Windows ARM64 for the Windows x64 classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("win32-x64", {
        platform: "win32",
        arch: "arm64",
        glibcVersionRuntime: undefined,
      }),
    /requires Windows x64/,
  );
});

test("rejects Windows x64 for the Windows ARM64 classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("win32-arm64", {
        platform: "win32",
        arch: "x64",
        glibcVersionRuntime: undefined,
      }),
    /requires Windows ARM64/,
  );
});

test("rejects a non-macOS host for the macOS classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("darwin-arm64", {
        platform: "linux",
        arch: "arm64",
        glibcVersionRuntime: "2.39",
      }),
    /requires macOS ARM64/,
  );
});

test("rejects macOS x64 for the macOS ARM64 classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("darwin-arm64", {
        platform: "darwin",
        arch: "x64",
        glibcVersionRuntime: undefined,
      }),
    /requires macOS ARM64/,
  );
});

test("rejects macOS ARM64 for the macOS x64 classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("darwin-x64", {
        platform: "darwin",
        arch: "arm64",
        glibcVersionRuntime: undefined,
      }),
    /requires macOS x64/,
  );
});

test("rejects an unimplemented classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("linux-riscv64", {
        platform: "linux",
        arch: "riscv64",
        glibcVersionRuntime: undefined,
      }),
    /Unsupported native build classifier/,
  );
});
