using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using IndirX.Core;

internal static class UpdateTests
{
    public static async Task Run(Action<bool, string> check)
    {
        var bytes = Encoding.UTF8.GetBytes("verified update fixture");
        var digest = Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();
        object Release(string tag, string name, string url, string hash, bool prerelease = false, bool draft = false, string state = "uploaded") => new
        {
            tag_name = tag, prerelease, draft,
            assets = new[] { new { name, browser_download_url = url, digest = "sha256:" + hash, size = bytes.Length, state } }
        };
        const string name = "IndirX-0.2.15-Windows-x64-Setup.exe";
        const string url = "https://github.com/sametyamanxx/indirx/releases/download/windows-v0.2.15/" + name;
        var valid = Release("windows-v0.2.15", name, url, digest);
        string Feed(params object[] releases) => JsonSerializer.Serialize(releases);
        check(Updates.Newer("0.2.10", "0.2.9") && !Updates.Newer("0.2.9", "0.2.10"), "numeric update version comparison");
        var update = Updates.FindApp(Feed(valid), "0.2.14");
        check(update?.Version == "0.2.15", "stable Windows installer is selected");
        check(Updates.FindApp(Feed(valid), "0.2.15") == null, "same version is not offered");
        check(Updates.FindApp(Feed(valid), "0.3.0") == null, "downgrade is not offered");
        foreach (var invalid in new[] {
            Release("v0.2.15", name, url, digest), Release("windows-v0.2.15-beta", name, url, digest),
            Release("windows-v0.2.15", name, url, digest, prerelease: true),
            Release("windows-v0.2.15", name, url, digest, draft: true),
            Release("windows-v0.2.15", name, url, digest, state: "new"),
            Release("windows-v0.2.15", "Portable.zip", url, digest),
            Release("windows-v0.2.15", name, url.Replace("github.com", "evil.test"), digest),
            Release("windows-v0.2.15", name, url.Replace("https://", "http://"), digest),
            Release("windows-v0.2.15", name, url, "invalid") })
            check(Updates.FindApp(Feed(invalid), "0.2.14") == null, "reject unsafe / wrong-platform / incomplete update");
        check(Updates.FindApp(Feed(valid, Release("windows-v9.0.0", "unfinished", url, digest)), "0.2.14") != null,
            "unfinished latest release does not hide a published installer");
        var root = Path.Combine(Path.GetTempPath(), "indirx-update-test-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(root);
        try
        {
            using var client = new HttpClient(new FixtureHandler(bytes));
            var path = Path.Combine(root, "setup.exe");
            await Updates.DownloadAsync(client, update!, path, CancellationToken.None);
            check(File.ReadAllBytes(path).SequenceEqual(bytes), "verified installer is staged");
            try { await Updates.DownloadAsync(client, update!, path, CancellationToken.None); check(false, "existing file must not be overwritten"); }
            catch (IOException) { check(File.Exists(path), "pre-existing file survives a staging collision"); }
            foreach (var invalid in new[] { update! with { Sha256 = new string('0', 64) }, update! with { Size = bytes.Length - 1 }, update! with { Size = bytes.Length + 1 } })
            {
                var bad = Path.Combine(root, Guid.NewGuid() + ".exe");
                try { await Updates.DownloadAsync(client, invalid, bad, CancellationToken.None); check(false, "invalid update bytes must fail"); }
                catch (InvalidDataException) { check(!File.Exists(bad), "failed hash/size validation removes only its own partial file"); }
            }
            using var stop = new CancellationTokenSource(); stop.Cancel();
            var cancelled = Path.Combine(root, "cancelled.exe");
            try { await Updates.DownloadAsync(client, update!, cancelled, stop.Token); check(false, "cancelled update must not stage"); }
            catch (OperationCanceledException) { check(!File.Exists(cancelled), "cancelled update does not leave a partial installer"); }
        }
        finally { Directory.Delete(root, recursive: true); }
    }
    private sealed class FixtureHandler(byte[] bytes) : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken token)
        {
            token.ThrowIfCancellationRequested();
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK) { Content = new ByteArrayContent(bytes), RequestMessage = request });
        }
    }
}
