/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;

// Each E2E test class fixture spins up its own Copilot CLI subprocess plus a CapiProxy
// (replaying HTTP proxy) Node.js subprocess. With ~25 test classes, running them in parallel
// would launch ~50 long-lived Node.js processes simultaneously and exhaust both file
// descriptors and memory on developer machines and CI runners (especially Windows). Tests
// within a class already run serially via xUnit's IClassFixture contract; this attribute
// extends that to cross-class execution. Re-enable parallelization only after either
// (a) sharing a single CLI subprocess across classes, or (b) gating concurrency with a
// semaphore that limits concurrent fixtures to a small number (e.g. 2-3).
[assembly: CollectionBehavior(DisableTestParallelization = true)]
