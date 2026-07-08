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

$events = Get-Content $JsonFile |
  Where-Object { $_ -notmatch '"ephemeral":true' -and $_ -notmatch '"tool.execution_partial_result"' } |
  ForEach-Object {
    try { $_ | ConvertFrom-Json } catch { $null }
  } |
  Where-Object { $_ -ne $null } |
  Select-Object -Last $Count

$events | ForEach-Object {
    $ts = if ($_.timestamp) { ([datetime]$_.timestamp).ToString("HH:mm:ss") } else { "--------" }
    $summary = switch -Wildcard ($_.type) {
      "user.message"              { $txt = $_.data.content; "USER: " + $txt.Substring(0, [Math]::Min(120, $txt.Length)) }
      "assistant.message"         {
        if ($_.data.content) {
          $txt = $_.data.content; "ASST: " + $txt.Substring(0, [Math]::Min(120, $txt.Length))
        } else {
          $names = ($_.data.toolRequests | ForEach-Object { $_.name }) -join ", "
          "ASST: [tool calls: $names]"
        }
      }
      "tool.execution_start"      {
        $args_summary = ($_.data.arguments | ConvertTo-Json -Compress -Depth 1) -replace '[\r\n]',''
        if ($args_summary.Length -gt 80) { $args_summary = $args_summary.Substring(0,77) + "..." }
        "TOOL> " + $_.data.toolName + " :: " + $args_summary
      }
      "tool.execution_complete"   { "TOOL< " + $(if ($_.data.success) {"OK"} else {"FAIL"}) + " (" + $_.data.toolName + ")" }
      "assistant.reasoning"       { $txt = $_.data.content; "THINK: " + $txt.Substring(0, [Math]::Min(120, $txt.Length)) }
      default                     { $_.type }
    }
    "$ts | $summary"
  }
