/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

export function iife<T>(fn: () => Promise<T>): Promise<T> {
  return fn();
}

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

type ShellConfigType = {
  shellToolName: string;
  readShellToolName: string;
  writeShellToolName: string;
};

/**
 * Shell configuration for platform-specific tool names.
 * Values duplicated from SDK since ShellConfig class isn't exported as a runtime value.
 */
export const ShellConfig: {
  powerShell: ShellConfigType;
  bash: ShellConfigType;
} = {
  powerShell: {
    shellToolName: "powershell",
    readShellToolName: "read_powershell",
    writeShellToolName: "write_powershell",
  } as ShellConfigType,
  bash: {
    shellToolName: "bash",
    readShellToolName: "read_bash",
    writeShellToolName: "write_bash",
  } as ShellConfigType,
};
