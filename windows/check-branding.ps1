param([Parameter(Mandatory)][string]$PublishDir,[string]$Setup)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
if (!('IndirXNativeIconReader' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class IndirXNativeIconReader {
    [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
    public static extern uint ExtractIconEx(string file, int index, IntPtr[] large, IntPtr[] small, uint count);
    [DllImport("user32.dll")]
    public static extern bool DestroyIcon(IntPtr icon);
}
'@
}
function Read-NativeIcon([string]$file) {
    $large = [IntPtr[]]::new(1); $small = [IntPtr[]]::new(1)
    if ([IndirXNativeIconReader]::ExtractIconEx($file, 0, $large, $small, 1) -lt 1 -or $large[0] -eq [IntPtr]::Zero) { throw "Native icon missing: $file" }
    try {
        $icon = [Drawing.Icon]::FromHandle($large[0])
        try { return $icon.ToBitmap() } finally { $icon.Dispose() }
    } finally {
        if ($large[0] -ne [IntPtr]::Zero) { [IndirXNativeIconReader]::DestroyIcon($large[0]) | Out-Null }
        if ($small[0] -ne [IntPtr]::Zero) { [IndirXNativeIconReader]::DestroyIcon($small[0]) | Out-Null }
    }
}
$iconFile = Join-Path $PublishDir 'Assets/IndirX.ico'
$expectedHash = (Get-FileHash (Join-Path (Split-Path $PSScriptRoot -Parent) 'app/src/main/res/drawable/logo.xml') -Algorithm SHA256).Hash.ToLowerInvariant()
if ((Get-Content (Join-Path $PublishDir 'Assets/Branding.sha256') -Raw).Trim() -ne $expectedHash) { throw 'Branding is not generated from the current Android logo' }
$executables = @((Join-Path $PublishDir 'IndirX.exe'))
if ($Setup) { $executables += $Setup }
foreach ($executable in $executables) {
    # Native extraction avoids both Explorer's icon cache and .NET Framework's
    # incorrect decoding of some PNG-compressed multi-resolution ICO frames.
    $actual = Read-NativeIcon $executable; $expected = Read-NativeIcon $iconFile
    try {
        if ($actual.Width -ne $expected.Width -or $actual.Height -ne $expected.Height) { throw 'Native icon size mismatch' }
        $difference = 0.0; $channels = 0
        for ($y = 0; $y -lt $expected.Height; $y++) {
            for ($x = 0; $x -lt $expected.Width; $x++) {
                $a = $actual.GetPixel($x,$y); $b = $expected.GetPixel($x,$y)
                if ($b.A -lt 32) { continue }
                $difference += [Math]::Abs([int]$a.R - [int]$b.R) + [Math]::Abs([int]$a.G - [int]$b.G) + [Math]::Abs([int]$a.B - [int]$b.B)
                $channels += 3
            }
        }
        if (!$channels -or $difference / $channels -gt 12) { throw "Native EXE icon differs from Android branding: $executable" }
    } finally { $actual.Dispose(); $expected.Dispose() }
}
Write-Host 'PASS: Android vector provenance and actual embedded Windows EXE icons.'
