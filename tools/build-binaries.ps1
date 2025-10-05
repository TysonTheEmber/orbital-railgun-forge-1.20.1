$ErrorActionPreference = "Stop"

$Root     = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$MaskDir  = Join-Path $Root "src/main/resources/assets/orbital_railgun/textures/mask"
$MaskPng  = Join-Path $MaskDir "mask.png"
$PackSrc  = Join-Path $Root "shaderpacks/OrbitalRailgun-Addon"
$PackZip  = Join-Path $Root "shaderpacks/OrbitalRailgun-Addon.zip"

# 1) Decode mask.png
Write-Host ">> Ensuring mask directory: $MaskDir"
New-Item -ItemType Directory -Force -Path $MaskDir | Out-Null

$Base64 = @"
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4////fwAJ+wP9KobjigAAAABJRU5ErkJggg==
"@

Write-Host ">> Writing mask.png"
[IO.File]::WriteAllBytes($MaskPng, [Convert]::FromBase64String($Base64.Trim()))

# 2) Zip the shader-pack folder
if (!(Test-Path $PackSrc -PathType Container)) {
  Write-Host "!! Shader-pack source not found: $PackSrc"
  Write-Host "   Make sure the text files were added in the previous PR."
  exit 1
}

Write-Host ">> Creating shader pack zip: $PackZip"
if (Test-Path $PackZip) { Remove-Item $PackZip -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($PackSrc, $PackZip)

# 3) SHA256 checksums
Write-Host ">> SHA256 checksums:"
try {
  if (Get-Command "sha256sum" -ErrorAction SilentlyContinue) {
    & sha256sum $MaskPng $PackZip
  } else {
    Get-FileHash -Algorithm SHA256 $MaskPng, $PackZip | Format-Table Path, Hash
  }
} catch {
  Write-Host "!! Could not compute SHA256; printing file sizes instead."
  Get-Item $MaskPng, $PackZip | Select-Object FullName, Length | Format-Table
}

Write-Host ">> Done."
