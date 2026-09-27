/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class ExtensionLaunchDiagnosticsTests
{
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
        }
        finally
        {
            Directory.Delete(homeDir, recursive: true);
        }
    }
}
