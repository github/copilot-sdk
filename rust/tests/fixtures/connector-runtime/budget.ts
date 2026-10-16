/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/** Wall-clock budget the harness allows a single fixture child process. */
export const CASE_TIMEOUT_MS = 210_000;

/** Slack for process spawn, MCP/API server startup, and report writing. */
const MATRIX_OVERHEAD_MS = 30_000;

export const transports = (process.env.CONNECTOR_LOCAL_TRANSPORTS ?? "stdio,inprocess").split(",");
export const cases = (process.env.CONNECTOR_LOCAL_CASES ?? "static,rotation,scope,targeted,disabled").split(",");

/**
 * The matrix runs sequentially, so the suite timeout must cover every case's own
 * budget rather than a single case's.
 */
export const matrixTimeoutMs = transports.length * cases.length * CASE_TIMEOUT_MS + MATRIX_OVERHEAD_MS;
