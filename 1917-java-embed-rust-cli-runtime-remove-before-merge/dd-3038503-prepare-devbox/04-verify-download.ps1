$ErrorActionPreference = "Stop"
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
function Write-DebugMessage {
    param([Parameter(Mandatory)][string]$Message)

    Write-Output "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')] [DEBUG] $Message"
}

Write-DebugMessage "Starting archive verification and extraction script."
Write-DebugMessage "Hash file: $HashFile"
Write-DebugMessage "Archive: $Archive"
Write-DebugMessage "Extraction directory: $DownloadDir"
Write-DebugMessage "7-Zip executable: $SevenZip"

Write-DebugMessage "Reading the expected SHA-256 hash."
$ExpectedHash = (Get-Content $HashFile).Trim()
Write-DebugMessage "Computing the archive SHA-256 hash."
$ActualHash = (Get-FileHash $Archive -Algorithm SHA256).Hash
Write-DebugMessage "Expected SHA-256: $ExpectedHash"
Write-DebugMessage "Actual SHA-256:   $ActualHash"

if ($ActualHash -ne $ExpectedHash) {
    Write-DebugMessage "Archive verification failed."
    throw "SHA-256 mismatch: transfer is corrupt"
}
Write-DebugMessage "Archive verification succeeded."

Write-DebugMessage "Extracting $Archive to $DownloadDir."
& $SevenZip x $Archive "-o$DownloadDir" -y
if ($LASTEXITCODE -ne 0) { throw "Extraction failed (exit code $LASTEXITCODE)" }
Write-DebugMessage "Extraction completed successfully."
Write-DebugMessage "Extracted TAR size: $((Get-Item $TarFile).Length) bytes."
Write-DebugMessage "Archive verification and extraction script completed successfully."