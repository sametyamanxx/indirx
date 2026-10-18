using System.Text.RegularExpressions;

namespace IndirX.Core;

public static class PlatformPolicy
{
    public const string DisplayName = "YouTube · Instagram";
    public const string Extractors = "youtube.*,instagram.*";
    public const string Error = "Yalnızca YouTube veya Instagram bağlantısı kullan.";
    private static readonly string[] Domains = ["youtube.com", "youtu.be", "instagram.com"];

    public static bool Accepts(string value) =>
        Uri.TryCreate(value, UriKind.Absolute, out var uri) &&
        uri.Scheme is "https" or "http" && string.IsNullOrEmpty(uri.UserInfo) &&
        uri.Port is 80 or 443 && !value.Contains('\\') &&
        Domains.Any(domain => uri.Host.Equals(domain, StringComparison.OrdinalIgnoreCase) ||
            uri.Host.EndsWith("." + domain, StringComparison.OrdinalIgnoreCase));

    public static string FromText(string text)
    {
        var match = Regex.Match(text, "https?://[^\\s<>\"']+", RegexOptions.IgnoreCase);
        var value = match.Success ? match.Value.TrimEnd(')', ']', '.', ',', ';') : text.Trim();
        if (!Accepts(value)) throw new ArgumentException(Error);
        return value;
    }
}
