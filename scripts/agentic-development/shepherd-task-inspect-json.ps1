<#
.SYNOPSIS
    Inspect a copilot JSON log file, showing the last N meaningful events.

.PARAMETER JsonFile
    Path to the JSON log file (relative or absolute).

.PARAMETER Count
    Number of messages to show (default: 20).
#>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$JsonFile,

    [Parameter(Mandatory = $false, Position = 1)]
    [int]$Count = 20
)

Get-Content $JsonFile |
  Where-Object { $_ -notmatch '"ephemeral":true' } |
  Select-Object -Last $Count |
  ForEach-Object { $_ | ConvertFrom-Json } |
  ForEach-Object {
    $ts = if ($_.timestamp) { ([datetime]$_.timestamp).ToString("HH:mm:ss") } else { "--------" }
    $summary = switch -Wildcard ($_.type) {
      "user.message"              { "USER: " + $_.data.content.Substring(0, [Math]::Min(80, $_.data.content.Length)) }
      "assistant.message"         { "ASST: " + $(if ($_.data.content) { $_.data.content.Substring(0, [Math]::Min(80, $_.data.content.Length)) } else { "[tool calls: " + ($_.data.toolRequests.name -join ", ") + "]" }) }
      "tool.execution_start"      { "TOOL> " + $_.data.toolName + " :: " + $_.data.arguments.description }
      "tool.execution_complete"   { "TOOL< " + $(if ($_.data.success) {"OK"} else {"FAIL"}) }
      "assistant.reasoning"       { "THINK: " + $_.data.content.Substring(0, [Math]::Min(80, $_.data.content.Length)) }
      default                     { $_.type }
    }
    "$ts | $summary"
  }
