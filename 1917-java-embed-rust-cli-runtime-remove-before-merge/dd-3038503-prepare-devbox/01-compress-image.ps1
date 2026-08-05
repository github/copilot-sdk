$Distro  = "Ubuntu-24.04"
$Location = "eastus2"

$TempDir = "C:\Users\edburns\Documents2\chaff\20260727-dd-3038503-prepare-devbox"
$TarFile = "$TempDir\ubuntu-24.04.tar"
$Archive = "$TarFile.7z"
$HashFile = "$Archive.sha256"
$SevenZip = "$env:ProgramFiles\7-Zip\7z.exe"

New-Item -ItemType Directory -Path $TempDir -Force | Out-Null


& $SevenZip a -t7z -mx=3 -mmt=on $Archive $TarFile
if ($LASTEXITCODE -ne 0) { throw "Compression failed" }

& $SevenZip t $Archive
if ($LASTEXITCODE -ne 0) { throw "Archive validation failed" }

(Get-FileHash $Archive -Algorithm SHA256).Hash |
    Set-Content $HashFile -Encoding ascii

Get-Item $TarFile, $Archive |
    Select-Object Name, @{Name="GiB"; Expression={[math]::Round($_.Length / 1GB, 2)}}