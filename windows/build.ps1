param([switch]$Installer)
$ErrorActionPreference = 'Stop'
[xml]$properties = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'Directory.Build.props'))
$version = [string]$properties.Project.PropertyGroup.Version
$root = Split-Path $PSScriptRoot -Parent
$artifacts = Join-Path $root 'artifacts/windows'
$publish = Join-Path $artifacts 'publish'
$dist = Join-Path $artifacts 'dist'
New-Item -ItemType Directory -Force -Path $publish,$dist | Out-Null
dotnet run --project (Join-Path $PSScriptRoot 'IndirX.Tests/IndirX.Tests.csproj') -c Release
if ($LASTEXITCODE -ne 0) { throw 'Core tests failed' }
dotnet publish (Join-Path $PSScriptRoot 'IndirX.Windows/IndirX.Windows.csproj') -c Release -r win-x64 --self-contained true -o $publish
if ($LASTEXITCODE -ne 0) { throw 'Windows publish failed' }
& (Join-Path $PSScriptRoot 'download-tools.ps1') -Destination (Join-Path $publish 'tools')
Copy-Item (Join-Path $root 'LICENSE') (Join-Path $publish 'LICENSE.txt') -Force
Copy-Item (Join-Path $PSScriptRoot 'THIRD_PARTY_NOTICES.md') $publish -Force
Copy-Item (Join-Path $PSScriptRoot 'README.md') (Join-Path $publish 'WINDOWS-README.md') -Force
dotnet --info | Out-File (Join-Path $publish 'BUILD-INFO.txt') -Encoding utf8
$licenses = Join-Path $publish 'licenses'
New-Item -ItemType Directory -Force -Path $licenses | Out-Null
$deps = Get-Content (Join-Path $publish 'IndirX.deps.json') -Raw | ConvertFrom-Json
$runtime = ($deps.libraries.PSObject.Properties.Name | Where-Object { $_ -like 'runtimepack.Microsoft.NETCore.App.Runtime.win-x64/*' } | Select-Object -First 1) -replace '.*/',''
if (!$runtime) { throw 'Unable to identify packaged .NET runtime version' }
foreach ($entry in @(@('runtime','LICENSE.TXT'),@('runtime','THIRD-PARTY-NOTICES.TXT'),@('wpf','LICENSE.TXT'),@('wpf','THIRD-PARTY-NOTICES.TXT'))) {
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/dotnet/$($entry[0])/v$runtime/$($entry[1])" -OutFile (Join-Path $licenses "dotnet-$($entry[0])-$($entry[1])")
}
& (Join-Path $PSScriptRoot 'smoke-test.ps1') -PublishDir $publish -OutputDir (Join-Path $artifacts 'smoke')
if ($Installer) {
    $compiler = Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6/ISCC.exe'
    if (!(Test-Path $compiler)) { throw 'Install Inno Setup 6 before building the installer' }
    # Inno Setup needs an explicit UTF-8 BOM for Turkish strings on all host locales.
    $installerSource = Join-Path $artifacts 'IndirX.iss'
    [IO.File]::WriteAllText($installerSource, [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'installer/IndirX.iss')), [Text.UTF8Encoding]::new($true))
    & $compiler "/DAppVersion=$version" "/DPublishDir=$publish" "/DOutputDir=$dist" $installerSource
    if ($LASTEXITCODE -ne 0) { throw 'Installer compilation failed' }
    $setup = Join-Path $dist "IndirX-$version-Windows-x64-Setup.exe"
    & (Join-Path $PSScriptRoot 'check-branding.ps1') -PublishDir $publish -Setup $setup
    & (Join-Path $PSScriptRoot 'test-installer.ps1') -Setup $setup -OutputDir (Join-Path $artifacts 'installer-smoke')
}
Compress-Archive -Path (Join-Path $publish '*') -DestinationPath (Join-Path $dist "IndirX-$version-Windows-x64-Portable.zip") -Force
Get-ChildItem $dist -File | Where-Object { $_.Extension -in '.exe','.zip' -and $_.Name -like "IndirX-$version-*" } | ForEach-Object {
    $hash = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $($_.Name)" | Out-File (Join-Path $dist ($_.Name + '.sha256')) -Encoding ascii
}
