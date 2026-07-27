$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Write-Diagnostic {
    param(
        [Parameter(Mandatory)]
        [string] $Message
    )

    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')] $Message"
}

function Invoke-Az {
    param(
        [Parameter(Mandatory)]
        [string[]] $Arguments,

        [switch] $SensitiveOutput
    )

    Write-Diagnostic "Running: az $($Arguments -join ' ')"
    $Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $Output = & az @Arguments 2>&1
    $ExitCode = $LASTEXITCODE
    $Stopwatch.Stop()

    if ($Output -and -not $SensitiveOutput) {
        $Output | ForEach-Object { Write-Diagnostic "az: $_" }
    }

    Write-Diagnostic "Azure CLI exited with code $ExitCode after $([math]::Round($Stopwatch.Elapsed.TotalSeconds, 2)) seconds."
    if ($ExitCode -ne 0) {
        if ($SensitiveOutput -and $Output) {
            $Output | ForEach-Object { Write-Diagnostic "az error: $_" }
        }
        throw "Azure CLI command failed: az $($Arguments -join ' ')"
    }

    return $Output
}

$Distro  = "Ubuntu-24.04"
$Location = "eastus2"

$TempDir = "C:\Users\edburns\Documents2\chaff\20260727-dd-3038503-prepare-devbox"
$TarFile = "$TempDir\ubuntu-24.04.tar"
$Archive = "$TarFile.7z"
$HashFile = "$Archive.sha256"

Write-Diagnostic "Starting storage upload for distro '$Distro'."
Write-Diagnostic "PowerShell: $($PSVersionTable.PSVersion); Azure CLI: $((az version --query '"azure-cli"' -o tsv 2>$null) -join '')"
Write-Diagnostic "Location: $Location; archive: $Archive; hash file: $HashFile"

foreach ($Path in $Archive, $HashFile) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Required input file does not exist: $Path"
    }

    $File = Get-Item -LiteralPath $Path
    Write-Diagnostic "Input '$($File.Name)': $($File.Length) bytes; modified $($File.LastWriteTime.ToString('o'))."
}

$SubscriptionId = (Invoke-Az -Arguments @("account", "show", "--query", "id", "--output", "tsv")) -join ""
if ([string]::IsNullOrWhiteSpace($SubscriptionId)) {
    throw "Azure CLI did not return a subscription ID."
}
Write-Diagnostic "Using Azure subscription $SubscriptionId."
Invoke-Az -Arguments @("account", "set", "--subscription", $SubscriptionId)

$Suffix = (New-Guid).Guid.Replace("-", "").Substring(0, 12).ToLower()
$ResourceGroup = "rg-wsl-transfer-$Suffix"
$StorageAccount = "wslxfer$Suffix"
$Container = "transfer"
$BlobName = Split-Path $Archive -Leaf
$HashBlobName = Split-Path $HashFile -Leaf

Write-Diagnostic "Generated resource group '$ResourceGroup' and storage account '$StorageAccount'."

Invoke-Az -Arguments @(
    "group", "create",
    "--name", $ResourceGroup,
    "--location", $Location,
    "--output", "json"
)

Invoke-Az -Arguments @(
    "storage", "account", "create",
    "--name", $StorageAccount,
    "--resource-group", $ResourceGroup,
    "--location", $Location,
    "--sku", "Standard_LRS",
    "--kind", "StorageV2",
    "--https-only", "true",
    "--min-tls-version", "TLS1_2",
    "--allow-blob-public-access", "false",
    "--output", "json"
)

$env:AZURE_STORAGE_ACCOUNT = $StorageAccount
$env:AZURE_STORAGE_KEY = (Invoke-Az -SensitiveOutput -Arguments @(
    "storage", "account", "keys", "list",
    "--resource-group", $ResourceGroup,
    "--account-name", $StorageAccount,
    "--query", "[0].value",
    "--output", "tsv"
)) -join ""
Write-Diagnostic "Retrieved storage account key (value redacted)."

Invoke-Az -Arguments @(
    "storage", "container", "create",
    "--name", $Container,
    "--output", "json"
)

Invoke-Az -Arguments @(
    "storage", "blob", "upload",
    "--container-name", $Container,
    "--name", $BlobName,
    "--file", $Archive,
    "--overwrite", "true",
    "--output", "json"
)

Invoke-Az -Arguments @(
    "storage", "blob", "upload",
    "--container-name", $Container,
    "--name", $HashBlobName,
    "--file", $HashFile,
    "--overwrite", "true",
    "--output", "json"
)

Write-Diagnostic "Upload completed successfully."
Write-Host "Resource group:  $ResourceGroup"
Write-Host "Storage account: $StorageAccount"
Write-Host "Container:       $Container"
Write-Host "Archive:         $BlobName"
