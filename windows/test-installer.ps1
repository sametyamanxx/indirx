param([Parameter(Mandatory)][string]$Setup,[Parameter(Mandatory)][string]$OutputDir)
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$install = Join-Path $OutputDir 'installed'
$log = Join-Path $OutputDir 'setup.log'
$process = Start-Process -FilePath $Setup -ArgumentList @('/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART',('/DIR="' + $install + '"'),('/LOG="' + $log + '"')) -Wait -PassThru
if ($process.ExitCode -ne 0 -or !(Test-Path (Join-Path $install 'IndirX.exe'))) { throw 'Silent setup failed' }
& (Join-Path $PSScriptRoot 'smoke-test.ps1') -PublishDir $install -OutputDir (Join-Path $OutputDir 'after-install')
$uninstaller = Get-ChildItem $install -Filter 'unins*.exe' | Select-Object -First 1
if (!$uninstaller) { throw 'Uninstaller not found' }
$process = Start-Process -FilePath $uninstaller.FullName -ArgumentList @('/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART') -Wait -PassThru
if ($process.ExitCode -ne 0 -or (Test-Path (Join-Path $install 'IndirX.exe'))) { throw 'Uninstall failed' }
Write-Host 'PASS: Setup installs per-user, installed app starts, uninstaller removes the app.'
