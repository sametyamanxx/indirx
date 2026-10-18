param([string]$Destination = (Join-Path $PSScriptRoot 'IndirX.Windows/Assets'))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationCore,WindowsBase
$source = Join-Path (Split-Path $PSScriptRoot -Parent) 'app/src/main/res/drawable/logo.xml'
[xml]$vector = [IO.File]::ReadAllText($source)
$android = 'http://schemas.android.com/apk/res/android'
$viewport = [double]::Parse($vector.DocumentElement.GetAttribute('viewportWidth', $android), [Globalization.CultureInfo]::InvariantCulture)
New-Item -ItemType Directory -Path $Destination -Force | Out-Null

function Render-Logo([int]$width, [int]$height, [int]$size, [double]$x, [double]$y, [string]$background) {
    $visual = [Windows.Media.DrawingVisual]::new()
    $drawing = $visual.RenderOpen()
    try {
        if ($background) {
            $brush = [Windows.Media.SolidColorBrush]::new([Windows.Media.ColorConverter]::ConvertFromString($background))
            $drawing.DrawRectangle($brush, $null, [Windows.Rect]::new(0, 0, $width, $height))
        }
        $drawing.PushTransform([Windows.Media.TranslateTransform]::new($x, $y))
        $drawing.PushTransform([Windows.Media.ScaleTransform]::new($size / $viewport, $size / $viewport))
        foreach ($path in $vector.DocumentElement.SelectNodes('path')) {
            $geometry = [Windows.Media.Geometry]::Parse($path.GetAttribute('pathData', $android))
            $fill = $null; $pen = $null
            $color = $path.GetAttribute('fillColor', $android)
            if ($color) { $fill = [Windows.Media.SolidColorBrush]::new([Windows.Media.ColorConverter]::ConvertFromString($color)) }
            $stroke = $path.GetAttribute('strokeColor', $android)
            if ($stroke) {
                $widthValue = [double]::Parse($path.GetAttribute('strokeWidth', $android), [Globalization.CultureInfo]::InvariantCulture)
                $pen = [Windows.Media.Pen]::new([Windows.Media.SolidColorBrush]::new([Windows.Media.ColorConverter]::ConvertFromString($stroke)), $widthValue)
                if ($path.GetAttribute('strokeLineCap', $android) -eq 'round') { $pen.StartLineCap = $pen.EndLineCap = [Windows.Media.PenLineCap]::Round }
                if ($path.GetAttribute('strokeLineJoin', $android) -eq 'round') { $pen.LineJoin = [Windows.Media.PenLineJoin]::Round }
            }
            $drawing.DrawGeometry($fill, $pen, $geometry)
        }
        $drawing.Pop(); $drawing.Pop()
    } finally { $drawing.Close() }
    $bitmap = [Windows.Media.Imaging.RenderTargetBitmap]::new($width, $height, 96, 96, [Windows.Media.PixelFormats]::Pbgra32)
    $bitmap.Render($visual)
    return $bitmap
}

function Encode-Png($bitmap) {
    $encoder = [Windows.Media.Imaging.PngBitmapEncoder]::new()
    $encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
    $stream = [IO.MemoryStream]::new()
    try { $encoder.Save($stream); return ,$stream.ToArray() } finally { $stream.Dispose() }
}

[IO.File]::WriteAllBytes((Join-Path $Destination 'Logo.png'), (Encode-Png (Render-Logo 256 256 256 0 0 '')))
$sizes = @(16,20,24,32,40,48,64,128,256)
$images = @($sizes | ForEach-Object { ,(Encode-Png (Render-Logo $_ $_ $_ 0 0 '')) })
$stream = [IO.File]::Create((Join-Path $Destination 'IndirX.ico'))
$writer = [IO.BinaryWriter]::new($stream)
try {
    $writer.Write([uint16]0); $writer.Write([uint16]1); $writer.Write([uint16]$sizes.Count)
    $offset = 6 + 16 * $sizes.Count
    for ($index = 0; $index -lt $sizes.Count; $index++) {
        $dimension = if ($sizes[$index] -eq 256) { 0 } else { $sizes[$index] }
        $writer.Write([byte]$dimension); $writer.Write([byte]$dimension)
        $writer.Write([byte]0); $writer.Write([byte]0)
        $writer.Write([uint16]1); $writer.Write([uint16]32)
        $writer.Write([uint32]$images[$index].Length); $writer.Write([uint32]$offset)
        $offset += $images[$index].Length
    }
    foreach ($image in $images) { $writer.Write([byte[]]$image) }
} finally { $writer.Dispose() }

foreach ($name in @('WizardSmall','WizardLarge')) {
    $bitmap = if ($name -eq 'WizardSmall') { Render-Logo 64 64 52 6 6 '#182131' } else { Render-Logo 164 314 108 28 70 '#0F1520' }
    $converted = [Windows.Media.Imaging.FormatConvertedBitmap]::new($bitmap, [Windows.Media.PixelFormats]::Bgr24, $null, 0)
    $encoder = [Windows.Media.Imaging.BmpBitmapEncoder]::new()
    $encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($converted))
    $file = [IO.File]::Create((Join-Path $Destination ($name + '.bmp')))
    try { $encoder.Save($file) } finally { $file.Dispose() }
}
$algorithm = [Security.Cryptography.SHA256]::Create()
$sourceStream = [IO.File]::OpenRead($source)
try { $hash = [BitConverter]::ToString($algorithm.ComputeHash($sourceStream)).Replace('-', '').ToLowerInvariant() }
finally { $sourceStream.Dispose(); $algorithm.Dispose() }
[IO.File]::WriteAllText((Join-Path $Destination 'Branding.sha256'), $hash, [Text.Encoding]::ASCII)
Write-Host 'Android vector rendered unchanged into PNG, multi-size Windows ICO and Setup artwork.'
