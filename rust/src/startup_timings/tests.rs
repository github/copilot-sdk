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
