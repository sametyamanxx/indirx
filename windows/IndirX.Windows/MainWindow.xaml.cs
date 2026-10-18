using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
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
    private string? resolvedLink;
    private long linkRevision;
    private string resultsLayoutDetail = "";

    public MainWindow()
    {
        store = new SettingsStore(Path.Combine(data, "settings.json"));
        settings = store.Load();
        InitializeComponent();
        ResultsList.ItemsSource = items;
        PageScroller.SizeChanged += (_, _) => UpdateResultsLayout();
        PlatformLabel.Text = PlatformPolicy.DisplayName;
        VersionText.Text = $"İndirX • {Assembly.GetExecutingAssembly().GetName().Version?.ToString(3)} • Windows x64\nYalnızca indirme hakkın olan içerikleri kaydet.";
        AudioMode.IsChecked = settings.AudioMode;
        VideoMode.IsChecked = !settings.AudioMode;
        RefreshFormat();
        FolderText.Text = "Kayıt yeri: " + settings.Folder;
        initialized = true;
        UpdateSelection();
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);
        NativeChrome.Apply(this);
    }

    protected override void OnContentRendered(EventArgs e)
    {
        base.OnContentRendered(e);
        NativeChrome.Apply(this);
    }

    private Point Position(FrameworkElement element) => element.TransformToAncestor(MainLayout).Transform(new Point());

    private bool LayoutMatchesRequest()
    {
        var logo = Position(BrandLogo); var title = Position(BrandTitle);
        var link = Position(LinkCard); var preferences = Position(PreferencesCard);
        var downloads = Position(DownloadsCard);
        return Math.Abs(logo.X + BrandLogo.ActualWidth / 2 - MainLayout.ActualWidth / 2) < 2 &&
            Math.Abs(title.X + BrandTitle.ActualWidth / 2 - MainLayout.ActualWidth / 2) < 2 &&
            logo.Y + BrandLogo.ActualHeight <= title.Y && title.Y + BrandTitle.ActualHeight < link.Y &&
            Math.Abs(link.X - preferences.X) < 2 && Math.Abs(LinkCard.ActualWidth - PreferencesCard.ActualWidth) < 2 &&
            Math.Abs(MainLayout.TranslatePoint(new Point(MainLayout.ActualWidth / 2, 0), PageScroller).X - PageScroller.ViewportWidth / 2) < 2 &&
            (DownloadsCard.Visibility != Visibility.Visible || (Math.Abs(downloads.X - link.X) < 2 &&
                Math.Abs(DownloadsCard.ActualWidth - LinkCard.ActualWidth) < 2 && downloads.Y >= preferences.Y + PreferencesCard.ActualHeight)) &&
            Math.Abs(preferences.Y - link.Y - LinkCard.ActualHeight - LinkCard.Margin.Bottom) < 2 &&
            (DownloadButton.Visibility != Visibility.Visible ||
                (Position(DownloadButton).Y >= Position(VersionText).Y + VersionText.ActualHeight &&
                 Math.Abs(Position(DownloadButton).X + DownloadButton.ActualWidth / 2 - MainLayout.ActualWidth / 2) < 2));
    }

    private bool CenteredCardsResize()
    {
        var originalPreferences = PreferencesCard.MinHeight;
        var originalLink = LinkCard.MinHeight;
        try
        {
            PreferencesCard.MinHeight = PreferencesCard.ActualHeight + 120;
            UpdateLayout();
            if (!LayoutMatchesRequest()) return false;
            LinkCard.MinHeight = PreferencesCard.ActualHeight + 120;
            UpdateLayout();
            return LayoutMatchesRequest();
        }
        finally { PreferencesCard.MinHeight = originalPreferences; LinkCard.MinHeight = originalLink; UpdateLayout(); }
    }

    private bool ResultsStayInFlow(string? previewFile)
    {
        // Exercise the real renderer and routed wheel event, without network or
        // settings writes. Single media stays compact; playlists fill the column.
        if (items.Count != 0 || history.Count != 0 || cancellation != null) return false;
        var originalWidth = Width; var originalHeight = Height;
        try
        {
            foreach (var size in new[] { (900, 900), (480, 640), (1120, 900), (1120, 800), (1440, 1000) })
            {
                ResetFound(); ClearStatus();
                Width = size.Item1; Height = size.Item2;
                UpdateLayout();
                foreach (var (collection, count) in new[] { (false, 1), (true, 1), (true, 500), (false, 1) })
                {
                    var media = Enumerable.Range(1, count)
                        .Select(index => new MediaItem($"Örnek video {index} — yerleşim testi", "https://www.youtube.com/watch?v=BaW_jenozKc", "0:30")).ToArray();
                    RenderFound(new MediaResult("Örnek liste — yerleşim testi", media, collection));
                    UpdateLayout();
                    var expectedOffset = Math.Min(Math.Max(0, Position(DownloadsCard).Y - 8), PageScroller.ScrollableHeight);
                    if (Math.Abs(PageScroller.VerticalOffset - expectedOffset) > 2) { resultsLayoutDetail = $"Found-content scroll: {size}"; return false; }
                    if (!LayoutMatchesRequest() ||
                        !ResultsPanel.IsDescendantOf(DownloadsCard) || ResultsList.Items.Count != media.Length ||
                        EmptyContentHint.Visibility != Visibility.Collapsed || OperationPanel.Visibility != Visibility.Collapsed ||
                        PlaylistHeader.Visibility != (collection ? Visibility.Visible : Visibility.Collapsed) ||
                        (!collection && SelectionText.Text.Length != 0)) { resultsLayoutDetail = $"Flow: {size}, playlist={collection}"; return false; }
                    if (previewFile != null && size == (1120, 900))
                        SaveThemePreview(collection ? previewFile.Replace(".detected.png", ".playlist.png") : previewFile);
                    if (collection)
                    {
                        if (ResultsList.ActualHeight < 30 || ResultsList.ActualHeight > ResultsList.MaxHeight + 2) { resultsLayoutDetail = $"List viewport: {size}"; return false; }
                        if (items.Any(item => item.Selected)) return false;
                        SelectAll_Click(this, new RoutedEventArgs());
                        if (!items.All(item => item.Selected)) return false;
                        SelectAll_Click(this, new RoutedEventArgs());
                        if (items.Any(item => item.Selected)) return false;
                        if (count > 1 && !PlaylistWheelMatches()) { resultsLayoutDetail = $"Wheel: {size}"; return false; }
                    }
                    else
                    {
                        if (!items.Single().Selected || ResultsList.ItemContainerGenerator.ContainerFromIndex(0) is not ListBoxItem row ||
                            Math.Abs(ResultsList.ActualHeight - row.ActualHeight) > 2) { resultsLayoutDetail = $"Single: {size}, list={ResultsList.ActualHeight}, row={(ResultsList.ItemContainerGenerator.ContainerFromIndex(0) as ListBoxItem)?.ActualHeight}, card={DownloadsCard.ActualHeight}"; return false; }
                    }
                }
            }
            resultsLayoutDetail = "PASS: centered column, compact single, scrollable playlists, routed wheel at five window sizes";
            return true;
        }
        finally { ResetFound(); ClearStatus(); Width = originalWidth; Height = originalHeight; UpdateLayout(); }
    }

    private bool PlaylistWheelMatches()
    {
        if (ResultsList.Template.FindName("PART_ScrollViewer", ResultsList) is not ScrollViewer scroller || scroller.ScrollableHeight <= 0) return false;
        scroller.ScrollToTop(); UpdateLayout();
        var pageOffset = PageScroller.VerticalOffset;
        var wheel = new MouseWheelEventArgs(Mouse.PrimaryDevice, Environment.TickCount, -120) { RoutedEvent = Mouse.PreviewMouseWheelEvent };
        ResultsList.RaiseEvent(wheel); UpdateLayout();
        if (!wheel.Handled || scroller.VerticalOffset <= 0 || Math.Abs(PageScroller.VerticalOffset - pageOffset) > 1) return false;
        scroller.ScrollToEnd(); UpdateLayout();
        wheel = new MouseWheelEventArgs(Mouse.PrimaryDevice, Environment.TickCount, -120) { RoutedEvent = Mouse.PreviewMouseWheelEvent };
        ResultsList.RaiseEvent(wheel); UpdateLayout();
        if (!wheel.Handled || Math.Abs(PageScroller.VerticalOffset - pageOffset) > 1) return false;
        wheel = new MouseWheelEventArgs(Mouse.PrimaryDevice, Environment.TickCount, 120) { RoutedEvent = Mouse.PreviewMouseWheelEvent };
        var bottom = scroller.VerticalOffset;
        ResultsList.RaiseEvent(wheel); UpdateLayout();
        var correct = wheel.Handled && scroller.VerticalOffset < bottom && Math.Abs(PageScroller.VerticalOffset - pageOffset) <= 1;
        scroller.ScrollToTop(); UpdateLayout();
        return correct;
    }

    private static bool HasFolderOpenButton(DependencyObject parent)
    {
        if (parent is Button button && string.Equals(button.Content?.ToString(), "Kayıt klasörünü aç", StringComparison.Ordinal)) return true;
        for (var i = 0; i < VisualTreeHelper.GetChildrenCount(parent); i++)
            if (HasFolderOpenButton(VisualTreeHelper.GetChild(parent, i))) return true;
        return false;
    }

    public object Diagnostics(string? detectedPreview = null, string? downloadingPreview = null) => new
    {
        Title, Platforms = PlatformLabel.Text, ItemCount = items.Count, HistoryCount = history.Count,
        ToolsPresent = new[] { "yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe", "deno.exe" }.All(file => File.Exists(Path.Combine(tools, file))),
        Background = Background.ToString(), Format = (FormatBox.SelectedItem as MediaFormat)?.Name,
        DownloadEnabled = DownloadButton.IsEnabled,
        ContentActionFlowCorrect = ContentActionFlowMatches(),
        WindowIconPresent = Icon != null, BrandLogoPresent = BrandLogo.Source != null,
        LayoutCorrect = LayoutMatchesRequest(), FolderOpenRemoved = !HasFolderOpenButton(this),
        CenteredCards = CenteredCardsResize(), ResultsFlowCorrect = ResultsStayInFlow(detectedPreview),
        UpdateControlsRemoved = !HasRemovedText(this),
        ResultsLayoutDetail = resultsLayoutDetail,
        ResultsInDownloads = ResultsPanel.IsDescendantOf(DownloadsCard),
        IdleStatusHidden = OperationPanel.Visibility == Visibility.Collapsed && StatusText.Text.Length == 0,
        DownloadThemeCorrect = DownloadThemeMatches(downloadingPreview),
        DownloadSurfaceLightRatio = downloadSurfaceLightRatio,
        DarkTitleBar = NativeChrome.IsDark(this), CaptionMatchesTheme = NativeChrome.HasMatchingCaption(this),
        NativeCaption = NativeChrome.Diagnostics(this),
        AndroidLogoHash = File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "Assets", "Branding.sha256")).Trim(),
        Version = Assembly.GetExecutingAssembly().GetName().Version?.ToString(3)
    };

    private void Paste_Click(object sender, RoutedEventArgs e)
    {
        try { LinkBox.Text = Clipboard.GetText(); }
        catch (Exception error) { ShowError(error); }
    }

    private bool HasCurrentResults => items.Count > 0 && resolvedLink == LinkBox.Text;

    private void LinkBox_TextChanged(object sender, TextChangedEventArgs e)
    {
        if (!initialized) return;
        linkRevision++;
        ResetFound();
        if (cancellation == null) ClearStatus();
    }

    private async void ContentAction_Click(object sender, RoutedEventArgs e)
    {
        if (cancellation != null) return;
        await FindAsync();
    }

    private async void Download_Click(object sender, RoutedEventArgs e) => await DownloadAsync();

    private async Task FindAsync()
    {
        if (cancellation != null) return;
        string url;
        try { url = PlatformPolicy.FromText(LinkBox.Text); }
        catch (Exception error) { ShowError(error); return; }
        LinkBox.Text = url;
        ResetFound();
        var revision = linkRevision;
        operation = BusyAsync(async token =>
        {
            EnsureTools();
            SetStatus("İçerikler aranıyor…", "Bağlantı çözümleniyor. Çalma listelerinde en fazla 500 içerik gösterilir.");
            var json = await ProcessRunner.RunAsync(Path.Combine(tools, "yt-dlp.exe"), Commands.Inspect(url, tools), tools, token);
            var found = MediaParser.Parse(json, url);
            // Never restore results for a link that was replaced while resolving.
            CompleteFind(found, revision);
        });
        await operation;
    }

    private void CompleteFind(MediaResult found, long revision)
    {
        if (revision == linkRevision) RenderFound(found);
        else ClearStatus();
    }

    private void SelectAll_Click(object sender, RoutedEventArgs e)
    {
        var select = items.Any(item => !item.Selected);
        foreach (var item in items) item.Selected = select;
        UpdateSelection();
    }

    private void ResetFound()
    {
        resolvedLink = null;
        items.Clear(); playlist = false;
        ResultsPanel.Visibility = PlaylistHeader.Visibility = Visibility.Collapsed;
        EmptyContentHint.Visibility = Visibility.Visible;
        CollectionTitle.Text = "";
        UpdateResultsLayout();
        UpdateSelection();
    }

    private void RenderFound(MediaResult found)
    {
        items.Clear();
        resolvedLink = LinkBox.Text;
        playlist = found.IsPlaylist;
        CollectionTitle.Text = playlist ? found.Title : "";
        foreach (var item in found.Items)
        {
            var choice = new SelectableItem(item, playlist);
            choice.PropertyChanged += (_, _) => UpdateSelection();
            items.Add(choice);
        }
        ResultsPanel.Visibility = Visibility.Visible;
        PlaylistHeader.Visibility = playlist ? Visibility.Visible : Visibility.Collapsed;
        EmptyContentHint.Visibility = Visibility.Collapsed;
        UpdateResultsLayout();
        UpdateSelection();
        ClearStatus();
        ScrollToFound();
    }

    private void ScrollToFound()
    {
        if (items.Count == 0) return;
        if (LinkBox.IsKeyboardFocusWithin) Keyboard.ClearFocus();
        UpdateLayout();
        PageScroller.ScrollToVerticalOffset(Math.Max(0, Position(DownloadsCard).Y - 8));
        PageScroller.UpdateLayout();
    }

    private void UpdateResultsLayout()
    {
        DownloadsCard.Visibility = items.Count > 0 || OperationPanel.Visibility == Visibility.Visible || HistoryPanel.Visibility == Visibility.Visible
            ? Visibility.Visible : Visibility.Collapsed;
        ResultsList.MaxHeight = playlist ? Math.Max(180, PageScroller.ViewportHeight * .45) : double.PositiveInfinity;
        DownloadsContentRow.Height = MediaRow.Height = GridLength.Auto;
    }

    private static bool HasRemovedText(DependencyObject parent)
    {
        var text = parent is Button button ? button.Content?.ToString() : (parent as TextBlock)?.Text;
        if (text != null && (text.Contains("İndirme altyapısını güncelle", StringComparison.OrdinalIgnoreCase) ||
            text.Contains("Yalnızca YouTube ve Instagram bağlantıları", StringComparison.OrdinalIgnoreCase) ||
            text.Contains("Geçmiş yalnızca bu oturumda gösterilir", StringComparison.OrdinalIgnoreCase))) return true;
        for (var i = 0; i < VisualTreeHelper.GetChildrenCount(parent); i++)
            if (HasRemovedText(VisualTreeHelper.GetChild(parent, i))) return true;
        return false;
    }

    private void ResultsList_PreviewMouseWheel(object sender, MouseWheelEventArgs e)
    {
        if (ResultsList.Template.FindName("PART_ScrollViewer", ResultsList) is not ScrollViewer scroller) return;
        // Keep the wheel inside the results, including at the first/last item,
        // rather than letting the page scroll away from the playlist.
        e.Handled = true;
        var lines = SystemParameters.WheelScrollLines;
        var distance = lines < 0 ? scroller.ViewportHeight : lines * 16;
        scroller.ScrollToVerticalOffset(scroller.VerticalOffset - e.Delta / 120.0 * distance);
    }

    private void UpdateSelection()
    {
        SelectionText.Text = playlist ? $"{items.Count} içerik • {items.Count(item => item.Selected)} seçili" : "";
        SelectAllButton.Content = items.Count > 0 && items.All(item => item.Selected) ? "Tümünü Kaldır" : "Tümünü Seç";
        var idle = cancellation == null && ControlsPanel.IsEnabled;
        ContentActionButton.Content = "İçerikleri bul";
        ContentActionButton.IsEnabled = idle && !string.IsNullOrWhiteSpace(LinkBox.Text);
        DownloadButton.Visibility = HasCurrentResults ? Visibility.Visible : Visibility.Collapsed;
        DownloadButton.IsEnabled = idle && HasCurrentResults && items.Any(item => item.Selected);
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

    private async Task DownloadAsync()
    {
        if (cancellation != null || !HasCurrentResults || FormatBox.SelectedItem is not MediaFormat format) return;
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
        HistoryPanel.Visibility = Visibility.Visible;
    }

    private async Task BusyAsync(Func<CancellationToken, Task> work)
    {
        using var source = new CancellationTokenSource();
        cancellation = source;
        SetBusyControls(true);
        try { await work(source.Token); }
        catch (OperationCanceledException) { SetStatus("İptal edildi", "İşlem durduruldu. Tamamlanan dosyalar korunur.", 0); }
        catch (Exception error) { SetStatus("İşlem tamamlanamadı", Explain(error), 0); ShowError(error); }
        finally
        {
            cancellation = null;
            SetBusyControls(false);
            DownloadProgress.IsIndeterminate = false;
            UpdateSelection();
        }
    }

    private void SetBusyControls(bool busy)
    {
        ControlsPanel.IsEnabled = ResultsPanel.IsEnabled = PreferencesPanel.IsEnabled = !busy;
        UpdateSelection();
        CancelButton.IsEnabled = busy;
    }

    private void EnsureTools()
    {
        foreach (var name in new[] { "yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe", "deno.exe" })
            if (!File.Exists(Path.Combine(tools, name))) throw new FileNotFoundException("İndirme altyapısı eksik: " + name + ". Setup ile yeniden kur.");
    }

    private void Cancel_Click(object sender, RoutedEventArgs e) { cancellation?.Cancel(); CancelButton.IsEnabled = false; }
    private void SetStatus(string status, string? detail, double progress = -1)
    {
        OperationPanel.Visibility = Visibility.Visible;
        UpdateResultsLayout();
        StatusText.Text = status; DetailText.Text = detail ?? "";
        DownloadProgress.IsIndeterminate = progress < 0;
        if (progress >= 0) DownloadProgress.Value = progress;
    }

    private void ClearStatus()
    {
        OperationPanel.Visibility = Visibility.Collapsed;
        StatusText.Text = DetailText.Text = "";
        DownloadProgress.IsIndeterminate = false;
        DownloadProgress.Value = 0;
        UpdateResultsLayout();
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
