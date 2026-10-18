param([Parameter(Mandatory)][string]$PublishDir,[Parameter(Mandatory)][string]$OutputDir)
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$tools = Join-Path $PublishDir 'tools'
foreach ($tool in @('yt-dlp','ffmpeg','ffprobe','deno')) {
    [string[]]$arguments = if ($tool -in 'ffmpeg','ffprobe') { @('-version') } else { @('--version') }
    & (Join-Path $tools "$tool.exe") @arguments 2>&1 | Out-File (Join-Path $OutputDir "$tool-version.txt")
    if ($LASTEXITCODE -ne 0) { throw "$tool failed to start" }
}
$source = Join-Path $OutputDir 'test.wav'
$mp3 = Join-Path $OutputDir 'test.mp3'
& (Join-Path $tools 'ffmpeg.exe') -hide_banner -loglevel error -f lavfi -i 'sine=frequency=440:duration=0.3' -y $source
if ($LASTEXITCODE -ne 0) { throw 'FFmpeg audio generation failed' }
& (Join-Path $tools 'ffmpeg.exe') -hide_banner -loglevel error -i $source -y $mp3
if ($LASTEXITCODE -ne 0 -or (Get-Item $mp3).Length -eq 0) { throw 'FFmpeg MP3 conversion failed' }
$previousPreference = $ErrorActionPreference
try {
    # A nonzero native exit is expected here, including on Windows PowerShell 5.1.
    $ErrorActionPreference = 'Continue'
    & (Join-Path $tools 'yt-dlp.exe') --ignore-config --no-plugin-dirs --use-extractors 'youtube.*,instagram.*' --simulate -- 'https://www.tiktok.com/@name/video/1234567890123456789' 2>&1 | Out-File (Join-Path $OutputDir 'rejected-platform.txt')
    $rejectedCode = $LASTEXITCODE
} finally { $ErrorActionPreference = $previousPreference }
if ($rejectedCode -eq 0 -or (Get-Content (Join-Path $OutputDir 'rejected-platform.txt') -Raw) -notmatch 'Unsupported URL|No suitable extractor') { throw 'TikTok was not rejected by packaged engine' }
# Do not leak this intentional nonzero native status into GitHub's pwsh wrapper.
$global:LASTEXITCODE = 0
foreach ($run in @(1,2)) {
    $result = Join-Path $OutputDir "ui-$run.json"
    $app = Start-Process -FilePath (Join-Path $PublishDir 'IndirX.exe') -ArgumentList @('--smoke-test',('"' + $result + '"')) -PassThru
    if (!$app.WaitForExit(45000)) { Stop-Process -Id $app.Id -Force; throw 'UI startup timed out' }
    if ($app.ExitCode -ne 0 -or !(Test-Path $result)) { throw "UI startup failed: $result" }
    $state = Get-Content $result -Raw | ConvertFrom-Json
    $expectedPlatforms = 'YouTube ' + [char]0x00B7 + ' Instagram'
    if ($state.Platforms -ne $expectedPlatforms -or $state.HistoryCount -ne 0 -or $state.ItemCount -ne 0 -or !$state.ToolsPresent -or $state.Version -ne '0.2.7' -or $state.Background -ne '#FF0F1520' -or !$state.Format -or $state.DownloadEnabled) { throw 'UI startup diagnostics failed' }
}
Write-Host 'PASS: tools start, MP3 conversion, TikTok rejection and two clean WPF launches.'
