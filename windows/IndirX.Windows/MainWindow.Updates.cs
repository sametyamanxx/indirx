using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Windows;
using System.Windows.Threading;
using IndirX.Core;

namespace IndirX.Windows;

public partial class MainWindow
{
    private readonly CancellationTokenSource updateLifetime = new();
    private DispatcherTimer? updateTimer;
    private DateTime nextUpdateCheck;
    private bool checkingUpdates;
    private AppUpdate? offeredApp;
    private string? offeredEngine;

    // Called only for normal startup, never by the offline smoke-test runner.
    public void StartAutomaticUpdates()
    {
        updateTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(30) };
        updateTimer.Tick += async (_, _) => await CheckAutomaticUpdatesAsync();
        updateTimer.Start();
        Activated += async (_, _) => await CheckAutomaticUpdatesAsync();
        Closed += (_, _) => { updateTimer.Stop(); updateLifetime.Cancel(); };
        Dispatcher.BeginInvoke(DispatcherPriority.ContextIdle, new Action(async () => await CheckAutomaticUpdatesAsync()));
    }

    private async Task CheckAutomaticUpdatesAsync()
    {
        if (checkingUpdates || cancellation != null || !IsActive || closing || updateLifetime.IsCancellationRequested) return;
        if (offeredApp == null && offeredEngine == null && DateTime.UtcNow < nextUpdateCheck) return;
        checkingUpdates = true;
        try
        {
            if (offeredApp == null && offeredEngine == null)
            {
                nextUpdateCheck = DateTime.UtcNow.AddHours(6);
                using var client = Updates.CreateClient();
                try { offeredApp = Updates.FindApp(await Updates.GetJsonAsync(client, Updates.ReleasesUrl, updateLifetime.Token),
                    Assembly.GetExecutingAssembly().GetName().Version!.ToString(3)); }
                catch (Exception error) { Debug.WriteLine("Application update check: " + error.Message); }
                try
                {
                    using var json = JsonDocument.Parse(await Updates.GetJsonAsync(client, Updates.EngineUrl, updateLifetime.Token));
                    var latest = json.RootElement.GetProperty("tag_name").GetString() ?? "";
                    var current = (await ProcessRunner.RunAsync(Path.Combine(tools, "yt-dlp.exe"), ["--ignore-config", "--no-plugin-dirs", "--version"], tools, updateLifetime.Token)).Trim();
                    if (Updates.Newer(latest, current)) offeredEngine = latest;
                }
                catch (Exception error) { Debug.WriteLine("Engine update check: " + error.Message); }
            }
            if (cancellation != null || !IsActive || updateLifetime.IsCancellationRequested || (offeredApp == null && offeredEngine == null)) return;
            var app = offeredApp; var engine = offeredEngine;
            offeredApp = null; offeredEngine = null;
            var message = (app != null ? $"İndirX {app.Version} mevcut.\n" : "") +
                (engine != null ? $"İndirme altyapısı {engine} mevcut.\n" : "") +
                "\nGüncellemeler yüklensin mi? Uygulama güncellemesi kurulum penceresini açar ve İndirX'i kapatır.";
            if (MessageBox.Show(this, message, "Güncelleme mevcut", MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
            operation = BusyAsync(async token =>
            {
                using var client = Updates.CreateClient();
                if (engine != null)
                {
                    try
                    {
                        SetStatus("İndirme altyapısı güncelleniyor…", engine);
                        await ProcessRunner.RunAsync(Path.Combine(tools, "yt-dlp.exe"), ["--ignore-config", "--no-plugin-dirs", "--update"], tools, token);
                        SetStatus("İndirme altyapısı güncellendi", engine, 100);
                    }
                    catch (Exception error) when (error is not OperationCanceledException) { ShowError(error); }
                }
                if (app == null) return;
                var directory = Path.Combine(data, "Updates"); Directory.CreateDirectory(directory);
                var installer = Path.Combine(directory, Guid.NewGuid().ToString("N") + "-" + app.Name);
                SetStatus("Uygulama güncellemesi indiriliyor…", $"İndirX {app.Version} • SHA-256 doğrulaması yapılacak");
                await Updates.DownloadAsync(client, app, installer, token);
                token.ThrowIfCancellationRequested();
                var installed = Directory.EnumerateFiles(AppContext.BaseDirectory, "unins*.exe").Any();
                var start = new ProcessStartInfo(installer) { UseShellExecute = true };
                if (installed) start.ArgumentList.Add("/DIR=" + AppContext.BaseDirectory.TrimEnd(Path.DirectorySeparatorChar));
                if (Process.Start(start) == null) throw new IOException("Kurulum başlatılamadı.");
                closing = true;
                Application.Current.Shutdown();
            });
            await operation;
        }
        catch (OperationCanceledException) { }
        finally { checkingUpdates = false; }
    }
}
