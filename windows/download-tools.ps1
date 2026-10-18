param([string]$Destination = (Join-Path $PSScriptRoot '.tools'))
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$lock = Get-Content (Join-Path $PSScriptRoot 'tools.lock.json') -Raw | ConvertFrom-Json
$cache = Join-Path $PSScriptRoot '.cache'
New-Item -ItemType Directory -Force -Path $Destination,$cache,(Join-Path $Destination 'licenses') | Out-Null

function Get-Verified($item, [string]$name) {
    $file = Join-Path $cache $name
    if (!(Test-Path $file) -or (Get-FileHash $file -Algorithm SHA256).Hash -ne $item.sha256) {
        Invoke-WebRequest -Uri $item.url -OutFile $file
    }
    if ((Get-FileHash $file -Algorithm SHA256).Hash -ne $item.sha256) { throw "SHA-256 mismatch: $name" }
    return $file
}

$yt = Get-Verified $lock.ytDlp 'yt-dlp.exe'
Copy-Item $yt (Join-Path $Destination 'yt-dlp.exe') -Force
$deno = Get-Verified $lock.deno 'deno.zip'
$ffmpeg = Get-Verified $lock.ffmpeg 'ffmpeg.zip'
$staging = Join-Path $cache ('extract-' + [Guid]::NewGuid().ToString('N'))
try {
    Expand-Archive -LiteralPath $deno -DestinationPath (Join-Path $staging 'deno')
    Copy-Item (Join-Path $staging 'deno/deno.exe') $Destination -Force
    Expand-Archive -LiteralPath $ffmpeg -DestinationPath (Join-Path $staging 'ffmpeg')
    $ffRoot = Get-ChildItem (Join-Path $staging 'ffmpeg') -Directory | Select-Object -First 1
    $bin = Join-Path $ffRoot.FullName 'bin'
    foreach ($pattern in @('ffmpeg.exe','ffprobe.exe','*.dll')) {
        Copy-Item (Join-Path $bin $pattern) $Destination -Force
    }
    Get-ChildItem $ffRoot.FullName -File | Where-Object { $_.Name -match 'LICENSE|COPYING|README' } |
        ForEach-Object { Copy-Item $_.FullName (Join-Path $Destination ('licenses/ffmpeg-' + $_.Name)) -Force }
} finally {
    if (Test-Path $staging) { Remove-Item -LiteralPath $staging -Recurse -Force }
}
Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/yt-dlp/yt-dlp/2026.08.19/LICENSE' -OutFile (Join-Path $Destination 'licenses/yt-dlp-LICENSE.txt')
Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/yt-dlp/yt-dlp/2026.08.19/THIRD_PARTY_LICENSES.txt' -OutFile (Join-Path $Destination 'licenses/yt-dlp-THIRD_PARTY_LICENSES.txt')
Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/denoland/deno/v2.9.7/LICENSE.md' -OutFile (Join-Path $Destination 'licenses/deno-LICENSE.txt')
Copy-Item (Join-Path $PSScriptRoot 'tools.lock.json') $Destination -Force
Write-Host 'Pinned yt-dlp, Deno and FFmpeg downloaded and SHA-256 verified.'
