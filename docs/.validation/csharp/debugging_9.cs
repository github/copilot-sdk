// Source: mcp/debugging.md:245
// Correct configuration for .NET exe
["my-dotnet-server"] = new McpLocalServerConfig
{
    Type = "local",
    Command = @"C:\Tools\MyServer\MyServer.exe",  // Full path with .exe
    Args = new List<string>(),
    Cwd = @"C:\Tools\MyServer",  // Set working directory
    Tools = new List<string> { "*" },
}

// For dotnet tool (DLL)
["my-dotnet-tool"] = new McpLocalServerConfig
{
    Type = "local", 
    Command = "dotnet",
    Args = new List<string> { @"C:\Tools\MyTool\MyTool.dll" },
    Cwd = @"C:\Tools\MyTool",
    Tools = new List<string> { "*" },
}