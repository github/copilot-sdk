/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import test from "node:test";

import { validateNativeHost } from "./validate-native-host.mjs";

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

test("rejects an unimplemented classifier", () => {
  assert.throws(
    () =>
      validateNativeHost("linuxmusl-x64", {
        platform: "linux",
        arch: "x64",
        glibcVersionRuntime: undefined,
      }),
    /Unsupported native build classifier/,
  );
});
