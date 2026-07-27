$ErrorActionPreference = "Stop"

function Write-DebugMessage {
    param([Parameter(Mandatory)][string]$Message)

    Write-Output "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')] [DEBUG] $Message"
}

Write-DebugMessage "Starting Azure Blob download script."

$ResourceGroup = "rg-wsl-transfer-ee5e8a35e287"
$StorageAccount = "wslxferee5e8a35e287"
$Container = "transfer"
$BlobName = "ubuntu-24.04.tar.7z"
$HashBlobName = "$BlobName.sha256"

$DownloadDir = "C:\Users\edburns\Documents2\chaff\20260727-dd-3038503-prepare-devbox"
$Archive = Join-Path $DownloadDir $BlobName
$HashFile = Join-Path $DownloadDir $HashBlobName
$TarFile = Join-Path $DownloadDir "ubuntu-24.04.tar"
$SevenZip = "$env:ProgramFiles\7-Zip\7z.exe"

Write-DebugMessage "Resource group: $ResourceGroup"
Write-DebugMessage "Storage account: $StorageAccount"
Write-DebugMessage "Container: $Container"
Write-DebugMessage "Archive blob: $BlobName"
Write-DebugMessage "Hash blob: $HashBlobName"
Write-DebugMessage "Download directory: $DownloadDir"

Write-DebugMessage "Ensuring the download directory exists."
New-Item -ItemType Directory -Path $DownloadDir -Force | Out-Null
Write-DebugMessage "Download directory is ready."

$env:AZURE_STORAGE_ACCOUNT = $StorageAccount
Write-DebugMessage "Requesting a storage account access key (the key will not be logged)."
$env:AZURE_STORAGE_KEY = az storage account keys list `
    --resource-group $ResourceGroup `
    --account-name $StorageAccount `
    --query "[0].value" `
    --output tsv
if ($LASTEXITCODE -ne 0) { throw "Failed to retrieve the storage account key (exit code $LASTEXITCODE)" }
Write-DebugMessage "Storage account access key acquired."

Write-DebugMessage "Downloading archive to $Archive."
az storage blob download `
    --container-name $Container `
    --name $BlobName `
    --file $Archive `
    --overwrite true `
    --only-show-errors
if ($LASTEXITCODE -ne 0) { throw "Archive download failed (exit code $LASTEXITCODE)" }
Write-DebugMessage "Archive download completed; size: $((Get-Item $Archive).Length) bytes."

Write-DebugMessage "Downloading SHA-256 file to $HashFile."
az storage blob download `
    --container-name $Container `
    --name $HashBlobName `
    --file $HashFile `
    --overwrite true `
    --only-show-errors
if ($LASTEXITCODE -ne 0) { throw "Hash file download failed (exit code $LASTEXITCODE)" }
Write-DebugMessage "Hash file download completed; size: $((Get-Item $HashFile).Length) bytes."
Write-DebugMessage "Azure Blob download script completed successfully."
