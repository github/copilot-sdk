/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[test]
fn millis_truncates_to_whole_milliseconds() {
    assert_eq!(StartupTimings::millis(Duration::from_micros(1_999)), 1);
    assert_eq!(StartupTimings::millis(Duration::from_millis(250)), 250);
    assert_eq!(StartupTimings::millis(Duration::ZERO), 0);
}

#[test]
fn default_leaves_every_phase_unset() {
    let timings = StartupTimings::default();
    assert_eq!(timings, StartupTimings::default());
    assert!(timings.program_resolve_ms.is_none());
    assert!(timings.process_spawn_ms.is_none());
    assert!(timings.port_wait_ms.is_none());
    assert_eq!(timings.transport_setup_ms, 0);
    assert_eq!(timings.handshake_ms, 0);
    assert!(timings.session_fs_ms.is_none());
    assert!(timings.llm_handler_ms.is_none());
    assert_eq!(timings.total_ms, 0);
}
