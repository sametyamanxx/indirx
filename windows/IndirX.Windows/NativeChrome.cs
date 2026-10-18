using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Media.Imaging;

namespace IndirX.Windows;

internal static class NativeChrome
{
    private const int DarkMode = 20, OldDarkMode = 19, CaptionColor = 35, TextColor = 36, BorderColor = 34;
    private const int Caption = 0x0020150F, Text = 0x00F4E8E2, Border = 0x004E392D;

    [DllImport("dwmapi.dll")]
    private static extern int DwmSetWindowAttribute(nint window, int attribute, ref int value, int size);
    [DllImport("dwmapi.dll")]
    private static extern int DwmGetWindowAttribute(nint window, int attribute, out int value, int size);

    public static void Apply(Window window)
    {
        var handle = new WindowInteropHelper(window).Handle;
        var enabled = 1;
        if (DwmSetWindowAttribute(handle, DarkMode, ref enabled, sizeof(int)) != 0)
            DwmSetWindowAttribute(handle, OldDarkMode, ref enabled, sizeof(int));
        // Explicit caption/border colors are supported on Windows 11; Windows 10
        // keeps its native dark caption with the immersive-dark-mode setting above.
        var caption = Caption; var text = Text; var border = Border;
        DwmSetWindowAttribute(handle, CaptionColor, ref caption, sizeof(int));
        DwmSetWindowAttribute(handle, TextColor, ref text, sizeof(int));
        DwmSetWindowAttribute(handle, BorderColor, ref border, sizeof(int));
    }

    public static bool IsDark(Window window)
    {
        var handle = new WindowInteropHelper(window).Handle;
        return (DwmGetWindowAttribute(handle, DarkMode, out var value, sizeof(int)) == 0 && value == 1) ||
            (DwmGetWindowAttribute(handle, OldDarkMode, out value, sizeof(int)) == 0 && value == 1);
    }

    public static bool HasMatchingCaption(Window window) =>
        Environment.OSVersion.Version.Build < 22000 ||
        (DwmGetWindowAttribute(new WindowInteropHelper(window).Handle, CaptionColor, out var value, sizeof(int)) == 0 && value == Caption);

    [StructLayout(LayoutKind.Sequential)]
    private struct Rect { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] private static extern bool GetWindowRect(nint window, out Rect rectangle);
    [DllImport("user32.dll")] private static extern nint GetWindowDC(nint window);
    [DllImport("user32.dll")] private static extern int ReleaseDC(nint window, nint dc);
    [DllImport("user32.dll")] private static extern bool PrintWindow(nint window, nint dc, uint flags);
    [DllImport("gdi32.dll")] private static extern nint CreateCompatibleDC(nint dc);
    [DllImport("gdi32.dll")] private static extern nint CreateCompatibleBitmap(nint dc, int width, int height);
    [DllImport("gdi32.dll")] private static extern nint SelectObject(nint dc, nint value);
    [DllImport("gdi32.dll")] private static extern bool DeleteObject(nint value);
    [DllImport("gdi32.dll")] private static extern bool DeleteDC(nint dc);

    // Capture only our window, including its native title bar, for smoke-test review.
    public static void SaveWindowImage(Window window, string file)
    {
        var handle = new WindowInteropHelper(window).Handle;
        if (!GetWindowRect(handle, out var rect)) return;
        var source = GetWindowDC(handle);
        var memory = CreateCompatibleDC(source);
        var bitmap = CreateCompatibleBitmap(source, rect.Right - rect.Left, rect.Bottom - rect.Top);
        var previous = SelectObject(memory, bitmap);
        try
        {
            if (!PrintWindow(handle, memory, 2)) return;
            var image = Imaging.CreateBitmapSourceFromHBitmap(bitmap, nint.Zero, Int32Rect.Empty, BitmapSizeOptions.FromEmptyOptions());
            var encoder = new PngBitmapEncoder();
            encoder.Frames.Add(BitmapFrame.Create(image));
            using var stream = System.IO.File.Create(file);
            encoder.Save(stream);
        }
        finally
        {
            SelectObject(memory, previous); DeleteObject(bitmap); DeleteDC(memory); ReleaseDC(handle, source);
        }
    }
}
