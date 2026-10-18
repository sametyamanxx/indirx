using System.Windows;
using IndirX.Core;

namespace IndirX.Windows;

public partial class MainWindow
{
    // Exercise the actual TextChanged, result renderer and selection handlers.
    // No clipboard changes, settings writes or network/download requests.
    private bool ContentActionFlowMatches()
    {
        if (items.Count != 0 || history.Count != 0 || cancellation != null) return false;
        var originalText = LinkBox.Text;
        var originalFormat = FormatBox.SelectedItem;
        var originalSettings = settings;
        const string firstLink = "https://www.youtube.com/watch?v=BaW_jenozKc";
        const string nextLink = "https://www.instagram.com/reel/example/";
        var single = new MediaResult("Örnek müzik", [new MediaItem("Örnek müzik", firstLink, "0:30")], false);
        var collection = new MediaResult("Örnek liste", Enumerable.Range(1, 3)
            .Select(index => new MediaItem($"Örnek müzik {index}", firstLink, "0:30")).ToArray(), true);
        bool ButtonMatches(string caption, bool enabled) =>
            ContentActionButton.Content?.ToString() == caption && ContentActionButton.IsEnabled == enabled;
        bool DownloadMatches(bool visible, bool enabled) => DownloadButton.Content?.ToString() == "İndir" &&
            DownloadButton.Visibility == (visible ? Visibility.Visible : Visibility.Collapsed) && DownloadButton.IsEnabled == enabled;
        bool Cleared() => items.Count == 0 && resolvedLink == null && !playlist &&
            ResultsPanel.Visibility == Visibility.Collapsed && EmptyContentHint.Visibility == Visibility.Visible &&
            OperationPanel.Visibility == Visibility.Collapsed && !HasCurrentResults;
        try
        {
            if (!ContentActionButton.IsDescendantOf(LinkCard) || ContentActionButton.IsDescendantOf(PreferencesCard) ||
                DownloadButton.Parent != MainLayout || System.Windows.Controls.Grid.GetRow(DownloadButton) <= System.Windows.Controls.Grid.GetRow(VersionText)) return false;
            LinkBox.Text = "";
            if (!ButtonMatches("İçerikleri bul", false) || !DownloadMatches(false, false)) return false;
            LinkBox.Text = firstLink;
            if (!ButtonMatches("İçerikleri bul", true) || !Cleared()) return false;
            CompleteFind(single, linkRevision);
            if (!HasCurrentResults || !ButtonMatches("İçerikleri bul", true) || !DownloadMatches(true, true) || !items.Single().Selected) return false;

            SetStatus("Tamamlandı", "Örnek işlem", 100);
            var staleRevision = linkRevision;
            LinkBox.Clear();
            if (!Cleared() || !ButtonMatches("İçerikleri bul", false) || !DownloadMatches(false, false)) return false;
            LinkBox.Text = firstLink;
            CompleteFind(single, staleRevision);
            if (!Cleared() || !ButtonMatches("İçerikleri bul", true)) return false;

            LinkBox.Text = nextLink;
            if (!Cleared() || !ButtonMatches("İçerikleri bul", true)) return false;
            CompleteFind(collection, linkRevision);
            if (!HasCurrentResults || !ButtonMatches("İçerikleri bul", true) || !DownloadMatches(true, false) || items.Any(item => item.Selected)) return false;
            items[0].Selected = true;
            if (!ButtonMatches("İçerikleri bul", true) || !DownloadMatches(true, true)) return false;
            SelectAll_Click(this, new RoutedEventArgs());
            if (!items.All(item => item.Selected) || !DownloadMatches(true, true)) return false;
            SelectAll_Click(this, new RoutedEventArgs());
            if (items.Any(item => item.Selected) || !DownloadMatches(true, false) || !ButtonMatches("İçerikleri bul", true)) return false;
            items[0].Selected = true;
            SetBusyControls(true);
            if (ContentActionButton.IsEnabled || DownloadButton.IsEnabled || LinkBox.IsEnabled) return false;
            SetBusyControls(false);
            if (!ButtonMatches("İçerikleri bul", true) || !DownloadMatches(true, true)) return false;

            // Replacing text directly (Ctrl+A / paste), not only clearing first,
            // must invalidate the playlist and return to finding new content.
            LinkBox.Text = "desteklenmeyen bağlantı";
            if (!Cleared() || !ButtonMatches("İçerikleri bul", true)) return false;
            LinkBox.Text = "   ";
            if (!Cleared() || !ButtonMatches("İçerikleri bul", false)) return false;
            LinkBox.Text = nextLink;
            if (!Cleared() || !ButtonMatches("İçerikleri bul", true)) return false;
            return settings == originalSettings && FormatBox.SelectedItem == originalFormat;
        }
        finally
        {
            SetBusyControls(false);
            LinkBox.Text = originalText;
            ResetFound(); ClearStatus(); UpdateLayout();
        }
    }
}
