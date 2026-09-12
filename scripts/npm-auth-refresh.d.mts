/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

export interface AuthCommand {
    command: string;
    args: string[];
    cwd?: string;
}

export type ConfigWriter = (npmrcPaths: string[]) => void;
export type CommandRunner = (
    command: string,
    args: string[],
    platform: string,
    cwd?: string
) => void;
export type AuthRefresher = () => void;

export const azureFeedLocalRegistry: string;
export const cfsRegistry: string;
export const credentialProviderRegistry: string;
export function getProjectNpmrcPaths(scriptUrl?: string): string[];
export function buildProjectNpmConfig(existingConfig?: string): string;
export function writeProjectNpmConfigs(npmrcPaths: string[]): void;
export function getAuthCommands(platform: string, npmrcPath: string): AuthCommand[];
export function getCommandInvocation(
    platform: string,
    command: string,
    args: string[]
): AuthCommand;
export function runCommand(command: string, args: string[], platform?: string, cwd?: string): void;
export function refreshNpmAuthentication(
    platform?: string,
    npmrcPaths?: string[],
    writer?: ConfigWriter,
    runner?: CommandRunner
): void;
export function main(args?: string[], refresh?: AuthRefresher): number;
