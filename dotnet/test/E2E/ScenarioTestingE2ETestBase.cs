/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public abstract class ScenarioTestingE2ETestBase(
    E2ETestFixture fixture,
    string snapshotCategory,
    ITestOutputHelper output)
    : E2ETestBase(fixture, snapshotCategory, output, replayOnly: true);
