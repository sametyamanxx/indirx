using System.IO;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using IndirX.Core;

namespace IndirX.Windows;

public partial class MainWindow
{
    private double downloadSurfaceLightRatio;

    // Offline UI regression: use the real renderer, busy-control state and
    // progress setter. No network request, file download or settings write.
    private bool DownloadThemeMatches(string? preview)
    {
        if (items.Count != 0 || history.Count != 0 || cancellation != null) return false;
        downloadSurfaceLightRatio = 0;
        var dark = true;
        try
        {
            foreach (var collection in new[] { false, true })
            {
                var media = Enumerable.Range(1, collection ? 12 : 1)
                    .Select(index => new MediaItem($"Örnek video {index} — indirme teması testi", "https://www.youtube.com/watch?v=BaW_jenozKc", "0:30")).ToArray();
                RenderFound(new MediaResult("Örnek liste — tema testi", media, collection));
                SetBusyControls(true);
                foreach (var progress in new[] { 37, -1 })
                {
                    SetStatus(progress < 0 ? "Dönüştürülüyor / kapak ekleniyor" : "İndiriliyor", media[0].Title, progress);
                    UpdateLayout();
                    if (ResultsList.IsEnabled || OperationPanel.Visibility != Visibility.Visible) return false;
                    downloadSurfaceLightRatio = Math.Max(downloadSurfaceLightRatio, LightPixelRatio(ResultsList));
                    dark &= DownloadProgress.Background is SolidColorBrush track && track.Color.R < 100 && track.Color.G < 100 && track.Color.B < 120;
                    // Text may be light, but an empty or disabled list background
                    // must not turn into the near-white Windows control surface.
                    dark &= downloadSurfaceLightRatio < 0.06;
                    if (!collection && preview != null)
                        SaveThemePreview(progress < 0 ? preview.Replace(".downloading.png", ".converting.png") : preview);
                }
            }
            return dark;
        }
        finally
        {
            ResetFound(); ClearStatus(); SetBusyControls(false); UpdateSelection(); UpdateLayout();
        }
    }

    private static double LightPixelRatio(FrameworkElement element)
    {
        var width = (int)Math.Ceiling(element.ActualWidth);
        var height = (int)Math.Ceiling(element.ActualHeight);
        if (width < 1 || height < 1) return 1;
        var bitmap = new RenderTargetBitmap(width, height, 96, 96, PixelFormats.Pbgra32);
        bitmap.Render(element);
        var pixels = new byte[width * height * 4];
        bitmap.CopyPixels(pixels, width * 4, 0);
        var light = 0; var samples = 0;
        for (var y = 0; y < height; y += 2)
            for (var x = 0; x < width; x += 2)
            {
                var offset = (y * width + x) * 4;
                if (pixels[offset + 3] < 200) continue;
                samples++;
                if (pixels[offset] > 215 && pixels[offset + 1] > 215 && pixels[offset + 2] > 215) light++;
            }
        return samples == 0 ? 0 : (double)light / samples;
    }

    private void SaveThemePreview(string file)
    {
        var bitmap = new RenderTargetBitmap((int)ActualWidth, (int)ActualHeight, 96, 96, PixelFormats.Pbgra32);
        bitmap.Render(this);
        var encoder = new PngBitmapEncoder();
        encoder.Frames.Add(BitmapFrame.Create(bitmap));
        using var stream = File.Create(file);
        encoder.Save(stream);
    }
}
