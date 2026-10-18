using System.IO;
using System.Text.Json;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;

namespace IndirX.Windows;

public partial class App : Application
{
    private Mutex? instance;
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        instance = new Mutex(true, "Local\\IndirX.Windows", out var first);
        if (!first) { MessageBox.Show("İndirX zaten açık.", "İndirX"); Shutdown(); return; }
        var window = new MainWindow();
        MainWindow = window;
        window.Show();
        if (e.Args.Length == 2 && e.Args[0] == "--smoke-test")
        {
            Dispatcher.BeginInvoke(DispatcherPriority.ContextIdle, new Action(() =>
            {
                try
                {
                    var result = Path.GetFullPath(e.Args[1]);
                    Directory.CreateDirectory(Path.GetDirectoryName(result)!);
                    window.UpdateLayout();
                    var bitmap = new RenderTargetBitmap((int)window.ActualWidth, (int)window.ActualHeight, 96, 96, PixelFormats.Pbgra32);
                    bitmap.Render(window);
                    var encoder = new PngBitmapEncoder();
                    encoder.Frames.Add(BitmapFrame.Create(bitmap));
                    using (var stream = File.Create(Path.ChangeExtension(result, ".png"))) encoder.Save(stream);
                    NativeChrome.SaveWindowImage(window, Path.ChangeExtension(result, ".window.png"));
                    File.WriteAllText(result, JsonSerializer.Serialize(window.Diagnostics()));
                    Shutdown(0);
                }
                catch (Exception error)
                {
                    File.WriteAllText(e.Args[1] + ".error.txt", error.ToString());
                    Shutdown(1);
                }
            }));
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        instance?.Dispose();
        base.OnExit(e);
    }
}
