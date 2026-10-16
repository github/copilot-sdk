/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class ExtensionLaunchDiagnosticsTests
{
    [Fact]
    public void Reports_Recent_Error_Categories_Without_Exposing_Tail_Lines()
    {
        var homeDir = Path.Join(Path.GetTempPath(), $"extension-diagnostics-{Guid.NewGuid():N}");
        var logsDir = Path.Join(homeDir, "logs");
        Directory.CreateDirectory(logsDir);
        try
        {
            File.WriteAllLines(Path.Join(logsDir, "process-1.log"),
            [
                "=== module=/private/project/sample-extension/extension.mjs ===",
                "TypeError: old failure",
                .. Enumerable.Range(0, 20).Select(index => $"context {index}"),
                "Error: connect ECONNREFUSED to secret-host.example using secret-user-input",
                "=== secret-user-input ===",
                "=== exit code=123456789 disposition=crash ===",
                "[sdk-extension-test] join failed code=123456789",
                "Error [ERR_SECRET_USER_INPUT]: secret-user-input",
            ]);

            var summary = TestHelper.ExtensionLaunchMarkers(homeDir, "user:sample-extension");

            Assert.Contains("recent error categories: ECONNREFUSED, Error", summary);
            Assert.DoesNotContain("TypeError", summary.Split(["recent error categories:"], StringSplitOptions.None)[1]);
            Assert.DoesNotContain("secret-user-input", summary);
            Assert.DoesNotContain("ERR_SECRET_USER_INPUT", summary);
            Assert.Contains("=== exit code=other disposition=crash ===", summary);
            Assert.Contains("[sdk-extension-test] join failed", summary);
            Assert.DoesNotContain("123456789", summary);
            Assert.DoesNotContain("secret-host.example", summary);
            Assert.DoesNotContain("/private/project", summary);
        }
        finally
        {
            Directory.Delete(homeDir, recursive: true);
        }
    }

    [Fact]
    public void Reports_Exception_Categories_Without_Exposing_Child_Stderr()
    {
        var homeDir = Path.Join(Path.GetTempPath(), $"extension-diagnostics-{Guid.NewGuid():N}");
        var logsDir = Path.Join(homeDir, "logs");
        Directory.CreateDirectory(logsDir);
        try
        {
            File.WriteAllText(Path.Join(logsDir, "process-1.log"), """
                === module=/private/project/sample-extension/extension.mjs ===
                === initializing ===
                [extension-bootstrap] Failed to load extension: TypeError [ERR_INVALID_ARG_TYPE]: secret-user-input
                [extension-bootstrap] Failed to load extension: Error: Request session.resume failed with message: session provider registration rejected for secret-user-input at private-host.example
                Error: connect ECONNRESET to private-host.example
                === exit code=1 disposition=crash ===
                """);
            File.WriteAllText(Path.Join(logsDir, "process-2.log"), """
                === module=/private/project/other-extension/extension.mjs ===
                ReferenceError: unrelated-child-error
                """);
            using var activeWriter = new FileStream(
                Path.Join(logsDir, "process-1.log"), FileMode.Open, FileAccess.Write, FileShare.ReadWrite | FileShare.Delete);

            var summary = TestHelper.ExtensionLaunchMarkers(homeDir, "user:sample-extension");

            Assert.Contains("=== initializing ===", summary);
            Assert.Contains("=== exit code=1 disposition=crash ===", summary);
            Assert.Contains("bootstrap import failed", summary);
            Assert.Contains("session.resume reason terms: session provider registration rejected", summary);
            Assert.Contains("TypeError", summary);
            Assert.Contains("ERR_INVALID_ARG_TYPE", summary);
            Assert.Contains("ECONNRESET", summary);
            Assert.DoesNotContain("secret-user-input", summary);
            Assert.DoesNotContain("private-host.example", summary);
            Assert.DoesNotContain("/private/project", summary);
            Assert.DoesNotContain("ReferenceError", summary);
        }
        finally
        {
            Directory.Delete(homeDir, recursive: true);
        }
    }
}
