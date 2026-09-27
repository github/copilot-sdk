/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class TestHelperTests
{
    [Fact]
    public void ExtensionLaunchMarkers_Redacts_Stderr_While_Reporting_Error_Category()
    {
        var home = Path.Join(Path.GetTempPath(), $"extension-log-test-{Guid.NewGuid():N}");
        var logs = Path.Join(home, "logs");
        Directory.CreateDirectory(logs);
        try
        {
            File.WriteAllLines(Path.Join(logs, "process-1-42.log"),
            [
                "=== launch pid=42 module=/tmp/extensions/failed-extension/extension.mjs ===",
                "=== initializing ===",
                "=== ready ===",
                "ignored old stderr",
                .. Enumerable.Range(0, 7).Select(index => $"context {index}"),
                "Error: extension startup failed",
                "=== exit code=1 disposition=crash ===",
            ]);

            var summary = TestHelper.ExtensionLaunchMarkers(home, "user:failed-extension");
            Assert.Contains("=== ready ===", summary);
            Assert.Contains("=== exit code=1 disposition=crash ===", summary);
            Assert.Contains("error categories: Error", summary);
            Assert.DoesNotContain("Error: extension startup failed", summary);
            Assert.DoesNotContain("ignored old stderr", summary);
        }
        finally
        {
            Directory.Delete(home, recursive: true);
        }
    }
}
