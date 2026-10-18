using System.Net.Http;
using System.Security.Cryptography;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace IndirX.Core;

public sealed record AppUpdate(string Version, string Name, string Url, long Size, string Sha256);

public static partial class Updates
{
    public const string ReleasesUrl = "https://api.github.com/repos/sametyamanxx/indirx/releases?per_page=100";
    public const string EngineUrl = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest";
    public static HttpClient CreateClient()
    {
        var client = new HttpClient { Timeout = TimeSpan.FromMinutes(10) };
        client.DefaultRequestHeaders.UserAgent.ParseAdd("IndirX-Update/1.0");
        return client;
    }

    public static AppUpdate? FindApp(string json, string current)
    {
        using var document = JsonDocument.Parse(json);
        var candidates = new List<AppUpdate>();
        foreach (var release in document.RootElement.EnumerateArray())
        {
            if (release.GetProperty("draft").GetBoolean() || release.GetProperty("prerelease").GetBoolean()) continue;
            var tag = release.GetProperty("tag_name").GetString() ?? "";
            if (!tag.StartsWith("windows-v", StringComparison.Ordinal)) continue;
            var version = tag[9..];
            if (!AppVersion().IsMatch(version) || !Newer(version, current)) continue;
            var name = $"IndirX-{version}-Windows-x64-Setup.exe";
            foreach (var asset in release.GetProperty("assets").EnumerateArray())
            {
                if (asset.GetProperty("name").GetString() != name || asset.GetProperty("state").GetString() != "uploaded") continue;
                var url = asset.GetProperty("browser_download_url").GetString() ?? "";
                var digest = asset.TryGetProperty("digest", out var value) ? value.GetString() ?? "" : "";
                var size = asset.GetProperty("size").GetInt64();
                if (TrustedUrl(url, version, name) && digest.StartsWith("sha256:", StringComparison.Ordinal) && Hash().IsMatch(digest[7..]) && size is > 0 and <= 1_000_000_000)
                    candidates.Add(new(version, name, url, size, digest[7..]));
            }
        }
        return candidates.OrderByDescending(candidate => Version.Parse(candidate.Version)).FirstOrDefault();
    }

    public static bool Newer(string candidate, string current) =>
        Version.TryParse(candidate, out var next) && Version.TryParse(current, out var installed) && next > installed;

    public static bool TrustedUrl(string url, string version, string name) => Uri.TryCreate(url, UriKind.Absolute, out var uri) &&
        uri.Scheme == "https" && uri.Host == "github.com" && uri.IsDefaultPort && uri.UserInfo.Length == 0 && uri.Query.Length == 0 && uri.Fragment.Length == 0 &&
        uri.AbsolutePath == $"/sametyamanxx/indirx/releases/download/windows-v{version}/{name}";

    public static async Task<string> GetJsonAsync(HttpClient client, string url, CancellationToken token)
    {
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(token);
        timeout.CancelAfter(TimeSpan.FromSeconds(25));
        var json = await client.GetStringAsync(url, timeout.Token);
        if (json.Length > 4_000_000) throw new InvalidDataException("Güncelleme yanıtı çok büyük.");
        return json;
    }

    public static async Task DownloadAsync(HttpClient client, AppUpdate update, string destination, CancellationToken token)
    {
        using var limit = CancellationTokenSource.CreateLinkedTokenSource(token);
        limit.CancelAfter(TimeSpan.FromMinutes(10));
        token = limit.Token;
        if (!TrustedUrl(update.Url, update.Version, update.Name) || !Hash().IsMatch(update.Sha256) || update.Size is <= 0 or > 1_000_000_000)
            throw new InvalidDataException("Güvenilmeyen güncelleme bilgisi.");
        var created = false;
        try
        {
            using var response = await client.GetAsync(update.Url, HttpCompletionOption.ResponseHeadersRead, token);
            response.EnsureSuccessStatusCode();
            if (response.RequestMessage?.RequestUri?.Scheme != "https") throw new InvalidDataException("Güvensiz indirme adresi.");
            await using var input = await response.Content.ReadAsStreamAsync(token);
            await using var output = new FileStream(destination, FileMode.CreateNew, FileAccess.Write, FileShare.None);
            created = true;
            using var hash = IncrementalHash.CreateHash(HashAlgorithmName.SHA256);
            var buffer = new byte[128 * 1024];
            long total = 0;
            int read;
            while ((read = await input.ReadAsync(buffer, token)) > 0)
            {
                total += read;
                if (total > update.Size) throw new InvalidDataException("Güncelleme boyutu uyuşmuyor.");
                hash.AppendData(buffer, 0, read);
                await output.WriteAsync(buffer.AsMemory(0, read), token);
            }
            if (total != update.Size || !Convert.ToHexString(hash.GetHashAndReset()).Equals(update.Sha256, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("Güncelleme SHA-256 doğrulaması başarısız.");
        }
        catch { if (created && File.Exists(destination)) File.Delete(destination); throw; }
    }

    [GeneratedRegex(@"^\d+\.\d+\.\d+$", RegexOptions.CultureInvariant)] private static partial Regex AppVersion();
    [GeneratedRegex(@"^[a-fA-F0-9]{64}$", RegexOptions.CultureInvariant)] private static partial Regex Hash();
}
