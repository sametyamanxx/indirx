param([Parameter(Mandatory)][string]$PublishDir,[Parameter(Mandatory)][string]$OutputDir)
$ErrorActionPreference = 'Stop'
[xml]$properties = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'Directory.Build.props'))
$version = [string]$properties.Project.PropertyGroup.Version
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
    $expectedTitle = [string][char]0x0130 + 'ND' + [char]0x0130 + 'RX'
    if ($state.Title -cne $expectedTitle -or $state.Platforms -ne $expectedPlatforms -or $state.HistoryCount -ne 0 -or $state.ItemCount -ne 0 -or !$state.ToolsPresent -or $state.Version -ne $version -or $state.Background -ne '#FF0F1520' -or !$state.Format -or $state.DownloadEnabled) { throw 'UI startup diagnostics failed' }
    if (!$state.WindowIconPresent -or !$state.BrandLogoPresent -or !$state.LayoutCorrect -or !$state.CenteredCards -or !$state.UpdateControlsRemoved -or !$state.ResultsFlowCorrect -or !$state.ResultsInDownloads -or !$state.IdleStatusHidden -or !$state.DownloadThemeCorrect -or !$state.ContentActionFlowCorrect -or !$state.FolderOpenRemoved -or !$state.DarkTitleBar -or !$state.CaptionMatchesTheme) {
        Write-Host (Get-Content $result -Raw)
        throw 'Branding, layout or native dark title bar check failed'
    }
    $androidHash = (Get-FileHash (Join-Path (Split-Path $PSScriptRoot -Parent) 'app/src/main/res/drawable/logo.xml') -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($state.AndroidLogoHash -ne $androidHash) { throw 'App logo provenance mismatch' }
}
& (Join-Path $PSScriptRoot 'check-branding.ps1') -PublishDir $PublishDir
Write-Host 'PASS: tools start, MP3 conversion, TikTok rejection and two clean WPF launches.'
