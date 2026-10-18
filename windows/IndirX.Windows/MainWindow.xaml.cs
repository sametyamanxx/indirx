using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using IndirX.Core;
using Microsoft.Win32;

namespace IndirX.Windows;

public sealed class SelectableItem(MediaItem item, bool playlist) : INotifyPropertyChanged
{
    public MediaItem Item { get; } = item;
    public Visibility SelectionVisibility => playlist ? Visibility.Visible : Visibility.Collapsed;
    private bool selected = !playlist;
    public bool Selected { get => selected; set { selected = value; PropertyChanged?.Invoke(this, new(nameof(Selected))); } }
    public event PropertyChangedEventHandler? PropertyChanged;
}

public partial class MainWindow : Window
{
    private readonly string data = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "IndirX");
    private readonly string tools = Path.Combine(AppContext.BaseDirectory, "tools");
    private readonly ObservableCollection<SelectableItem> items = [];
    private readonly List<string> history = [];
    private readonly SettingsStore store;
    private UserSettings settings;
    private CancellationTokenSource? cancellation;
    private Task? operation;
    private bool initialized, playlist, closing;

    public MainWindow()
    {
        store = new SettingsStore(Path.Combine(data, "settings.json"));
        settings = store.Load();
        InitializeComponent();
        ResultsList.ItemsSource = items;
        PlatformLabel.Text = PlatformPolicy.DisplayName;
        VersionText.Text = $"İndirX • {Assembly.GetExecutingAssembly().GetName().Version?.ToString(3)} • Windows x64\nYalnızca indirme hakkın olan içerikleri kaydet. Geçmiş yalnızca bu oturumda gösterilir.";
        AudioMode.IsChecked = settings.AudioMode;
        VideoMode.IsChecked = !settings.AudioMode;
        RefreshFormat();
        FolderText.Text = "Kayıt yeri: " + settings.Folder;
        initialized = true;
    }

    public object Diagnostics() => new
    {
        Title, Platforms = PlatformLabel.Text, ItemCount = items.Count, HistoryCount = history.Count,
        ToolsPresent = new[] { "yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe", "deno.exe" }.All(file => File.Exists(Path.Combine(tools, file))),
        Background = Background.ToString(), Format = (FormatBox.SelectedItem as MediaFormat)?.Name,
        DownloadEnabled = DownloadButton.IsEnabled,
        Version = Assembly.GetExecutingAssembly().GetName().Version?.ToString(3)
    };

    private void Paste_Click(object sender, RoutedEventArgs e)
    {
        try { LinkBox.Text = PlatformPolicy.FromText(Clipboard.GetText()); }
        catch (Exception error) { ShowError(error); }
    }

    private async void Find_Click(object sender, RoutedEventArgs e)
    {
        if (cancellation != null) return;
        string url;
        try { url = PlatformPolicy.FromText(LinkBox.Text); }
        catch (Exception error) { ShowError(error); return; }
        LinkBox.Text = url;
        items.Clear(); ResultsCard.Visibility = Visibility.Collapsed; UpdateSelection();
        operation = BusyAsync(async token =>
        {
            EnsureTools();
            SetStatus("İçerikler aranıyor…", "Bağlantı çözümleniyor. Çalma listelerinde en fazla 500 içerik gösterilir.");
            var json = await ProcessRunner.RunAsync(Path.Combine(tools, "yt-dlp.exe"), Commands.Inspect(url, tools), tools, token);
            var found = MediaParser.Parse(json, url);
            playlist = found.IsPlaylist;
            CollectionTitle.Text = found.Title;
            foreach (var item in found.Items)
            {
                var choice = new SelectableItem(item, playlist);
                choice.PropertyChanged += (_, _) => UpdateSelection();
                items.Add(choice);
            }
            ResultsCard.Visibility = Visibility.Visible;
            SelectAllButton.Visibility = playlist ? Visibility.Visible : Visibility.Collapsed;
            UpdateSelection();
            SetStatus("İçerikler bulundu", $"{items.Count} içerik • indirmeye hazır", 0);
        });
        await operation;
    }

    private void SelectAll_Click(object sender, RoutedEventArgs e)
    {
        var select = items.Any(item => !item.Selected);
        foreach (var item in items) item.Selected = select;
        UpdateSelection();
    }

    private void UpdateSelection()
    {
        SelectionText.Text = playlist ? $"{items.Count} içerik • {items.Count(item => item.Selected)} seçili" : "İndirmeye hazır";
        SelectAllButton.Content = items.Count > 0 && items.All(item => item.Selected) ? "Tümünü Kaldır" : "Tümünü Seç";
        DownloadButton.Content = playlist ? "Seçilenleri indir ↓" : "İndir ↓";
        DownloadButton.IsEnabled = cancellation == null && items.Any(item => item.Selected);
    }

    private void Mode_Changed(object sender, RoutedEventArgs e)
    {
        if (!initialized) return;
        settings = settings with { AudioMode = AudioMode.IsChecked == true };
        RefreshFormat(); SaveSettings();
    }

    private void RefreshFormat()
    {
        var audio = AudioMode.IsChecked == true;
        FormatBox.ItemsSource = Commands.Formats.Where(format => format.IsAudio == audio).ToArray();
        FormatBox.SelectedItem = Commands.Formats.FirstOrDefault(format => format.IsAudio == audio &&
            format.Value == (audio ? settings.AudioFormat : settings.VideoFormat)) ?? Commands.Formats.First(format => format.IsAudio == audio);
    }

    private void Format_Changed(object sender, SelectionChangedEventArgs e)
    {
        if (!initialized || FormatBox.SelectedItem is not MediaFormat format) return;
        settings = format.IsAudio ? settings with { AudioFormat = format.Value } : settings with { VideoFormat = format.Value };
        SaveSettings();
    }

    private void ChooseFolder_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFolderDialog { Title = "İndirilen dosyaların kaydedileceği klasörü seç", Multiselect = false };
        if (Directory.Exists(settings.Folder)) dialog.InitialDirectory = settings.Folder;
        if (dialog.ShowDialog(this) != true) return;
        settings = settings with { Folder = dialog.FolderName };
        FolderText.Text = "Kayıt yeri: " + settings.Folder;
        SaveSettings();
    }

    private void SaveSettings()
    {
        try { store.Save(settings); }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException)
        { MessageBox.Show(this, "Tercihler kaydedilemedi: " + error.Message, "İndirX", MessageBoxButton.OK, MessageBoxImage.Warning); }
    }

    private async void Download_Click(object sender, RoutedEventArgs e)
    {
        if (cancellation != null || FormatBox.SelectedItem is not MediaFormat format) return;
        var selected = items.Where(item => item.Selected).Select(item => item.Item).ToArray();
        if (selected.Length == 0) return;
        var folder = settings.Folder;
        operation = BusyAsync(async token =>
        {
            EnsureTools();
            Directory.CreateDirectory(folder);
            var succeeded = 0; var failed = 0;
            foreach (var item in selected)
            {
                token.ThrowIfCancellationRequested();
                var temporary = Path.Combine(data, "Temp", "download-" + Guid.NewGuid().ToString("N"));
                Directory.CreateDirectory(temporary);
                try
                {
                    var index = succeeded + failed + 1;
                    SetStatus($"{index} / {selected.Length} • İndiriliyor", item.Title);
                    var progress = new Progress<string>(line =>
                    {
                        if (!line.StartsWith(Commands.ProgressPrefix, StringComparison.Ordinal)) return;
                        var value = line[Commands.ProgressPrefix.Length..].Trim().TrimEnd('%');
                        if (double.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out var percent))
                            SetStatus(percent >= 100 ? "Dönüştürülüyor / kapak ekleniyor" : $"{index} / {selected.Length} • İndiriliyor", item.Title,
                                percent >= 100 ? -1 : Math.Clamp(percent, 0, 99));
                    });
                    var output = await ProcessRunner.RunAsync(Path.Combine(tools, "yt-dlp.exe"), Commands.Download(item.Url, format, tools), temporary, token,
                        line => ((IProgress<string>)progress).Report(line));
                    var paths = output.Split('\n').Select(line => line.Trim()).Where(line => line.StartsWith(Commands.FilePrefix, StringComparison.Ordinal))
                        .Select(line => JsonSerializer.Deserialize<string>(line[Commands.FilePrefix.Length..]))
                        .Select(file => file == null ? null : Path.GetFullPath(file, temporary)).ToArray();
                    if (paths.Length == 0 || paths.Any(file => file == null || !OutputFiles.IsInside(file, temporary) || !File.Exists(file) || new FileInfo(file).Length == 0))
                        throw new InvalidOperationException("Tamamlanmış çıktı bulunamadı.");
                    foreach (var path in paths)
                    {
                        SetStatus("Klasöre kaydediliyor", Path.GetFileName(path));
                        var destination = await OutputFiles.CopyUniqueAsync(path!, folder, token);
                        Record("✓ " + Path.GetFileName(destination) + " — İndirildi");
                    }
                    succeeded++;
                }
                catch (OperationCanceledException) { throw; }
                catch (Exception error) { failed++; Record("✕ " + item.Title + " — " + Explain(error)); }
                finally
                {
                    // Only this operation's private staging folder; never the selected media folder.
                    try { Directory.Delete(temporary, recursive: true); } catch (IOException) { } catch (UnauthorizedAccessException) { }
                }
            }
            SetStatus(failed > 0 ? "Kısmen tamamlandı" : "Tamamlandı", $"{succeeded} içerik indirildi • {failed} hata", failed > 0 ? 0 : 100);
        });
        await operation;
    }

    private void Record(string value)
    {
        history.Insert(0, value);
        if (history.Count > 100) history.RemoveAt(history.Count - 1);
        HistoryText.Text = string.Join("\n\n", history);
    }

    private async void Update_Click(object sender, RoutedEventArgs e)
    {
        if (cancellation != null) return;
        operation = BusyAsync(async token =>
        {
            EnsureTools();
            SetStatus("İndirme altyapısı güncelleniyor…", "Resmî yt-dlp kararlı sürümü denetleniyor.");
            var result = await ProcessRunner.RunAsync(Path.Combine(tools, "yt-dlp.exe"), ["--ignore-config", "--no-plugin-dirs", "--update"], tools, token);
            SetStatus("Güncelleme kontrolü tamamlandı", result.Trim(), 0);
        });
        await operation;
    }

    private async Task BusyAsync(Func<CancellationToken, Task> work)
    {
        using var source = new CancellationTokenSource();
        cancellation = source;
        ControlsPanel.IsEnabled = PreferencesPanel.IsEnabled = UpdateButton.IsEnabled = false;
        CancelButton.IsEnabled = true;
        try { await work(source.Token); }
        catch (OperationCanceledException) { SetStatus("İptal edildi", "İşlem durduruldu. Tamamlanan dosyalar korunur.", 0); }
        catch (Exception error) { SetStatus("İşlem tamamlanamadı", Explain(error), 0); ShowError(error); }
        finally
        {
            cancellation = null;
            ControlsPanel.IsEnabled = PreferencesPanel.IsEnabled = UpdateButton.IsEnabled = true;
            CancelButton.IsEnabled = false;
            DownloadProgress.IsIndeterminate = false;
            UpdateSelection();
        }
    }

    private void EnsureTools()
    {
        foreach (var name in new[] { "yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe", "deno.exe" })
            if (!File.Exists(Path.Combine(tools, name))) throw new FileNotFoundException("İndirme altyapısı eksik: " + name + ". Setup ile yeniden kur.");
    }

    private void Cancel_Click(object sender, RoutedEventArgs e) { cancellation?.Cancel(); CancelButton.IsEnabled = false; }
    private void SetStatus(string status, string? detail, double progress = -1)
    {
        StatusText.Text = status; DetailText.Text = detail ?? "";
        DownloadProgress.IsIndeterminate = progress < 0;
        if (progress >= 0) DownloadProgress.Value = progress;
    }

    private void OpenFolder_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            Directory.CreateDirectory(settings.Folder);
            Process.Start(new ProcessStartInfo("explorer.exe") { ArgumentList = { settings.Folder }, UseShellExecute = false });
        }
        catch (Exception error) { ShowError(error); }
    }

    private static string Explain(Exception error)
    {
        var message = error.Message;
        if (message.Contains("Unsupported URL", StringComparison.OrdinalIgnoreCase)) return PlatformPolicy.Error;
        if (message.Contains("429")) return "Platform çok fazla istek aldı. Bir süre sonra yeniden dene.";
        if (message.Contains("403") || message.Contains("sign in", StringComparison.OrdinalIgnoreCase) || message.Contains("login", StringComparison.OrdinalIgnoreCase))
            return "Platform erişimi reddetti veya giriş istiyor. Halka açık başka bir bağlantı dene; indirme altyapısını güncelle.";
        return message.Length > 1800 ? message[^1800..] : message;
    }

    private void ShowError(Exception error) => MessageBox.Show(this, Explain(error), "İndirX", MessageBoxButton.OK, MessageBoxImage.Warning);

    private async void Window_Closing(object? sender, CancelEventArgs e)
    {
        if (closing) return;
        if (cancellation == null) return;
        e.Cancel = true;
        if (MessageBox.Show(this, "Devam eden işlem iptal edilip uygulama kapatılsın mı?", "İndirX", MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
        closing = true;
        cancellation.Cancel();
        if (operation != null) await operation;
        Close();
    }
}
