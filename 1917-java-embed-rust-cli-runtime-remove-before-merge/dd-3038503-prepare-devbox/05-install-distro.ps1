$ErrorActionPreference = "Stop"

function Write-DebugMessage {
	param([Parameter(Mandatory)][string]$Message)

	Write-Output "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')] [DEBUG] $Message"
}
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

$DistroName = "Ubuntu-24.04"
$InstallDirectory = "C:\WSL\Ubuntu-24.04"

Write-DebugMessage "Starting WSL distribution installation script."
Write-DebugMessage "Distribution name: $DistroName"
Write-DebugMessage "Installation directory: $InstallDirectory"
Write-DebugMessage "Source TAR: $TarFile"
if (Test-Path -LiteralPath $TarFile -PathType Leaf) {
    Write-DebugMessage "Source TAR size: $((Get-Item -LiteralPath $TarFile).Length) bytes."
} else {
    Write-DebugMessage "Source TAR does not currently exist. The WSL import is expected to fail."
}

Write-DebugMessage "Ensuring the installation directory exists."
New-Item -ItemType Directory -Path $InstallDirectory -Force | Out-Null
Write-DebugMessage "Installation directory is ready."

Write-DebugMessage "Importing $DistroName as a WSL 2 distribution. This may take several minutes."
wsl --import $DistroName $InstallDirectory $TarFile --version 2
if ($LASTEXITCODE -ne 0) { throw "WSL import failed (exit code $LASTEXITCODE)" }
Write-DebugMessage "WSL import completed successfully."

Write-DebugMessage "Listing installed WSL distributions."
wsl --list --verbose
if ($LASTEXITCODE -ne 0) { throw "Unable to list WSL distributions (exit code $LASTEXITCODE)" }

Write-DebugMessage "Reading registered WSL distributions from the current user's Lxss registry key."
$RegisteredDistributions = @(
    Get-ChildItem HKCU:\Software\Microsoft\Windows\CurrentVersion\Lxss |
        ForEach-Object {
            Write-DebugMessage "Inspecting registry key $($_.PSChildName)."
            $properties = Get-ItemProperty $_.PSPath
            Write-DebugMessage "Registry key $($_.PSChildName) maps to distribution '$($properties.DistributionName)'."
            [pscustomobject]@{
                Name = $properties.DistributionName
                Id   = $_.PSChildName
            }
        }
)
Write-DebugMessage "Found $($RegisteredDistributions.Count) registered WSL distribution(s)."
$RegisteredDistributions
Write-DebugMessage "WSL registry inspection completed."

# Write-DebugMessage "Launching $DistroName. The script resumes after the distribution exits."
# wsl --distribution $DistroName
# if ($LASTEXITCODE -ne 0) { throw "WSL distribution exited with code $LASTEXITCODE" }
# Write-DebugMessage "WSL distribution installation script completed successfully."

Write-DebugMessage "WSL distribution installation script completed successfully."