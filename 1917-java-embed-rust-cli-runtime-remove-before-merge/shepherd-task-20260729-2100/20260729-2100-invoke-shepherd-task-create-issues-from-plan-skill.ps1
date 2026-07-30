$timestamp = '20260729-2100'
$logDirFull = 'C:\Users\edburns\workareas\copilot-sdk\1917-java-embed-rust-cli-runtime-remove-before-merge\shepherd-task-20260729-2100'
New-Item -ItemType Directory -Path $logDirFull -Force | Out-Null
$sessionSharePath = Join-Path $logDirFull "create-issues-session-$timestamp.md"
$sessionJsonPath = Join-Path $logDirFull "create-issues-session-$timestamp.json"
$sessionOtelPath = Join-Path $logDirFull "create-issues-otel-$timestamp.jsonl"
$promptPath = 'C:\Users\edburns\workareas\copilot-sdk\1917-java-embed-rust-cli-runtime-remove-before-merge\shepherd-task-20260729-2100\20260729-2100-invoke-shepherd-task-create-issues-from-plan-skill.md'
$prompt = Get-Content $promptPath -Raw
Write-Output "[shepherd-task] Logging create-issues run to: $logDirFull"
$env:COPILOT_OTEL_FILE_EXPORTER_PATH = $sessionOtelPath
$copilotExit = 0
try {
    $prompt | copilot --yolo --output-format json --share $sessionSharePath > $sessionJsonPath
    $copilotExit = $LASTEXITCODE
}
finally {
    Remove-Item Env:\COPILOT_OTEL_FILE_EXPORTER_PATH -ErrorAction SilentlyContinue
}
if ($copilotExit -ne 0) {
    Write-Error "[shepherd-task] FAILED: copilot exited with code $copilotExit"
}
else {
    Write-Output "[shepherd-task] Create-issues session complete."
}
