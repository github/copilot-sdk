// Copyright (c) Microsoft Corporation. All rights reserved.
#![cfg(test)]
#![allow(clippy::unwrap_used)]

#[path = "../build/in_process.rs"]
mod implementation;

#[allow(dead_code)]
#[path = "../src/platform_dirs/mod.rs"]
mod platform_dirs;
