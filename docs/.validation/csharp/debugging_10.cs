// Source: mcp/debugging.md:269
// Windows needs cmd /c for npx
["filesystem"] = new McpLocalServerConfig
{
    Type = "local",
    Command = "cmd",
    Args = new List<string> { "/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "C:\\allowed\\path" },
    Tools = new List<string> { "*" },
}