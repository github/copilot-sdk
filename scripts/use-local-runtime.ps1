# Copyright (c) Microsoft Corporation. All rights reserved.

<#
.SYNOPSIS
Use the sibling copilot-agent-runtime checkout for SDK development.
.DESCRIPTION
Sets COPILOT_CLI_PATH in this PowerShell process and its future child processes.
Does not change the user/machine environment, SDK dependencies, or authentication.
Stop SDK processes before rebuilding native code on Windows, then restart them.
.EXAMPLE
.\scripts\use-local-runtime.ps1
cd nodejs
npm run build
npx tsx .\samples\chat.ts
.EXAMPLE
.\scripts\use-local-runtime.ps1 -Build
.EXAMPLE
Get-Help .\scripts\use-local-runtime.ps1 -Full
#>
[CmdletBinding()]
param(
    [switch]$Build
)

& {
    $ErrorActionPreference = 'Stop'
    $sdkRoot = Split-Path -Parent $PSScriptRoot
    $runtimeRoot = [IO.Path]::GetFullPath((Join-Path $sdkRoot '..\copilot-agent-runtime'))
    $platform = & node -p "process.platform + '-' + process.arch"
    if ($LASTEXITCODE -ne 0) {
        throw 'Node.js is required to resolve the local runtime platform.'
    }
    $runtimeDirectory = Join-Path $runtimeRoot "dist-cli\prebuilds\$platform"
    $wrapperName = if ($IsWindows -or $env:OS -eq 'Windows_NT') { 'copilot-runtime.exe' } else { 'copilot-runtime' }
    $runtimePath = Join-Path $runtimeDirectory $wrapperName

    if ($Build) {
        Push-Location $runtimeRoot
        try {
            & pnpm run build
            if ($LASTEXITCODE -ne 0) {
                throw 'Local runtime build failed.'
            }
        }
        finally {
            Pop-Location
        }
        Push-Location (Join-Path $sdkRoot 'nodejs')
        try {
            & npm run build
            if ($LASTEXITCODE -ne 0) {
                throw 'Local Node.js SDK build failed. Run npm ci in nodejs if dependencies are missing.'
            }
        }
        finally {
            Pop-Location
        }
    }

    foreach ($path in @($runtimePath, (Join-Path $runtimeDirectory 'runtime.node'))) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Local runtime artifact missing: $path. Run this script with -Build."
        }
    }

    $env:COPILOT_CLI_PATH = $runtimePath
    Write-Host "COPILOT_CLI_PATH=$env:COPILOT_CLI_PATH"
    Write-Host 'SDK commands launched from this terminal now use the local runtime.'
}
