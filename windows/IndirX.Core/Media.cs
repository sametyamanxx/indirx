using System.Text.Json;

namespace IndirX.Core;

public sealed record MediaItem(string Title, string Url, string Duration);
public sealed record MediaResult(string Title, IReadOnlyList<MediaItem> Items, bool IsPlaylist);

public static class MediaParser
{
    public static MediaResult Parse(string json, string sourceUrl)
    {
        if (!PlatformPolicy.Accepts(sourceUrl)) throw new ArgumentException(PlatformPolicy.Error);
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        var playlist = root.TryGetProperty("entries", out var entries) && entries.ValueKind == JsonValueKind.Array;
        var items = new List<MediaItem>();
        void Add(JsonElement item)
        {
            if (item.ValueKind != JsonValueKind.Object) return;
            if (Text(item, "availability") is "private" or "premium_only" or "subscriber_only" or "needs_auth") return;
            var url = Text(item, "webpage_url");
            if (!PlatformPolicy.Accepts(url)) url = Text(item, "url");
            if (!PlatformPolicy.Accepts(url) && Text(item, "ie_key").Equals("Youtube", StringComparison.OrdinalIgnoreCase))
            {
                var id = Text(item, "id");
                if (id.Length > 0) url = "https://www.youtube.com/watch?v=" + Uri.EscapeDataString(id);
            }
            if (!PlatformPolicy.Accepts(url) && !playlist) url = sourceUrl;
            if (!PlatformPolicy.Accepts(url)) return;
            double seconds = item.TryGetProperty("duration", out var duration) && duration.ValueKind == JsonValueKind.Number && duration.TryGetDouble(out var number) && double.IsFinite(number)
                ? Math.Clamp(number, 0, 315360000) : 0;
            var span = TimeSpan.FromSeconds(seconds);
            var label = seconds > 0 ? $"{(int)span.TotalMinutes}:{span.Seconds:00}" : "Video / ses";
            items.Add(new MediaItem(Text(item, "title", "Başlıksız içerik"), url, label));
        }
        if (playlist) foreach (var item in entries.EnumerateArray()) Add(item);
        else Add(root);
        if (items.Count == 0) throw new InvalidOperationException("Erişilebilir içerik bulunamadı. Bağlantıyı kontrol et.");
        return new MediaResult(Text(root, "title", "Bulunan içerikler"), items, playlist);
    }

    private static string Text(JsonElement item, string name, string fallback = "") =>
        item.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.String && !string.IsNullOrWhiteSpace(value.GetString())
            ? value.GetString()! : fallback;
}
