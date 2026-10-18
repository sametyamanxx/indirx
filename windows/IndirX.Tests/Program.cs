using System.Diagnostics;
using System.Text.Json;
using IndirX.Core;

// A dependency-free test executable: a failing assertion returns a nonzero exit code.
if (args.FirstOrDefault() == "--echo") { Console.Write(JsonSerializer.Serialize(args.Skip(1))); return; }
if (args.FirstOrDefault() == "--fail") { Console.Error.WriteLine("test failure detail"); Environment.ExitCode = 7; return; }
if (args.FirstOrDefault() == "--wait") { Console.WriteLine(Environment.ProcessId); await Task.Delay(TimeSpan.FromMinutes(5)); return; }

var assertions = 0;
void Check(bool condition, string message)
{
    assertions++;
    if (!condition) throw new Exception("FAIL: " + message);
}
async Task Throws<T>(Func<Task> action) where T : Exception
{
    try { await action(); } catch (T) { assertions++; return; }
    throw new Exception("Expected " + typeof(T).Name);
}

foreach (var url in new[] { "https://www.youtube.com/watch?v=BaW_jenozKc", "https://youtu.be/BaW_jenozKc", "HTTPS://M.YOUTUBE.COM/shorts/test", "https://youtube.com/playlist?list=test", "https://www.instagram.com/reel/test/", "https://instagram.com/p/test/" })
    Check(PlatformPolicy.Accepts(url), "supported: " + url);
foreach (var url in new[] { "https://tiktok.com/@name/video/123", "https://vm.tiktok.com/test/", "https://vt.tiktok.com/test/", "https://www.tiktok.com/t/test/", "https://facebook.com/test", "https://x.com/test", "https://vimeo.com/test", "https://youtube.com.evil.test/test", "https://notyoutube.com/test", "https://youtube.com@evil.test/test", "https://evil.test@youtube.com/test", "https://youtube.com:1234/test", "https://youtube.com\\@evil.test", "file:///youtube.com/test", "ftp://youtube.com/test", "not a url" })
    Check(!PlatformPolicy.Accepts(url), "rejected: " + url);
Check(PlatformPolicy.FromText("Paylaşılan video: https://youtu.be/BaW_jenozKc\nİzle") == "https://youtu.be/BaW_jenozKc", "shared text");

var single = MediaParser.Parse("""{"title":"Türkçe 🎵","duration":null,"url":"https://cdn.example.test/file.mp4"}""", "https://www.instagram.com/reel/test/");
Check(single.Items.Count == 1 && single.Items[0].Title == "Türkçe 🎵", "single media / Unicode / null duration");
Check(single.Items[0].Url == "https://www.instagram.com/reel/test/" && !single.IsPlaylist, "use original page, not CDN");
var playlist = MediaParser.Parse("""{"title":"Liste","entries":[{"title":"One","id":"abc","ie_key":"Youtube","duration":61},null,{"title":"Private","availability":"private","url":"https://youtu.be/private"},{"url":"https://tiktok.com/test"}]}""", "https://youtube.com/playlist?list=test");
Check(playlist.IsPlaylist && playlist.Items.Count == 1, "filter private, null and unsupported playlist entries");
Check(playlist.Items[0].Url == "https://www.youtube.com/watch?v=abc" && playlist.Items[0].Duration == "1:01", "flat playlist ID and duration");

var root = Path.Combine(Path.GetTempPath(), "indirx-tests-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(root);
try
{
    var inspect = Commands.Inspect("https://youtube.com/playlist?list=a&index=2", root);
    Check(inspect.Contains(PlatformPolicy.Extractors) && inspect.Contains("--no-plugin-dirs") && inspect.Contains("--ignore-config"), "restrict external configuration and extractors");
    Check(inspect[^2] == "--" && inspect[^1].Contains("&index=2"), "URL remains one argument after separator");
    foreach (var format in Commands.Formats)
    {
        var command = Commands.Download("https://youtu.be/BaW_jenozKc", format, root);
        Check(command.Contains(format.Value) && command.Contains("--no-simulate"), "format " + format.Name);
    }
    await Throws<ArgumentException>(() => Task.FromResult(Commands.Download("https://tiktok.com/test", Commands.Formats[0], root)));
    await Throws<ArgumentException>(() => Task.FromResult(MediaParser.Parse("{}", "https://facebook.com/test")));
    var file = Path.Combine(root, "source", "Türkçe video.mp4");
    Directory.CreateDirectory(Path.GetDirectoryName(file)!);
    await File.WriteAllTextAsync(file, "test bytes");
    var output = Path.Combine(root, "media");
    var first = await OutputFiles.CopyUniqueAsync(file, output, CancellationToken.None);
    var second = await OutputFiles.CopyUniqueAsync(file, output, CancellationToken.None);
    Check(Path.GetFileName(second) == "Türkçe video (2).mp4" && await File.ReadAllTextAsync(first) == "test bytes", "collision keeps original");
    Check(OutputFiles.IsInside(file, Path.Combine(root, "source")), "inside staging");
    Check(!OutputFiles.IsInside(first, Path.Combine(root, "source")), "outside staging rejected");
    using (var cancellation = new CancellationTokenSource())
    {
        cancellation.Cancel();
        await Throws<OperationCanceledException>(() => OutputFiles.CopyUniqueAsync(file, output, cancellation.Token));
        Check(Directory.GetFiles(output).Length == 2, "cancel creates no media");
    }
    var preferencesFile = Path.Combine(root, "preferences", "settings.json");
    var store = new SettingsStore(preferencesFile);
    var preferences = new UserSettings(output, "mkv", "flac", true);
    store.Save(preferences);
    Check(store.Load() == preferences, "settings round trip");
    var settingsJson = await File.ReadAllTextAsync(preferencesFile);
    Check(!settingsJson.Contains("history", StringComparison.OrdinalIgnoreCase) && !settingsJson.Contains("https://"), "no history or link persistence");
    await File.WriteAllTextAsync(preferencesFile, "broken");
    Check(store.Load().VideoFormat == "mp4", "corrupt settings fallback");

    // Use this test executable as an offline fake child process.
    var executable = Environment.ProcessPath!;
    string[] prefix = Path.GetFileNameWithoutExtension(executable).Equals("dotnet", StringComparison.OrdinalIgnoreCase)
        ? [typeof(PlatformPolicy).Assembly.Location.Replace("IndirX.Core.dll", "IndirX.Tests.dll")] : [];
    var maliciousLooking = "title & echo SHOULD_NOT_RUN | <input> \"quoted\" 🎵";
    var echoed = await ProcessRunner.RunAsync(executable, [.. prefix, "--echo", maliciousLooking], root, CancellationToken.None);
    Check(JsonSerializer.Deserialize<string[]>(echoed)![0] == maliciousLooking, "no shell interpretation / Unicode arguments");
    try { await ProcessRunner.RunAsync(executable, [.. prefix, "--fail"], root, CancellationToken.None); throw new Exception("Expected process failure"); }
    catch (ToolException error) { Check(error.Message.Contains("test failure detail"), "stderr preserved"); }
    using var stop = new CancellationTokenSource();
    var started = new TaskCompletionSource<int>(TaskCreationOptions.RunContinuationsAsynchronously);
    var running = ProcessRunner.RunAsync(executable, [.. prefix, "--wait"], root, stop.Token, line => started.TrySetResult(int.Parse(line)));
    var pid = await started.Task.WaitAsync(TimeSpan.FromSeconds(20));
    stop.Cancel();
    await Throws<OperationCanceledException>(() => running);
    var exited = false;
    try { using var child = Process.GetProcessById(pid); exited = child.HasExited; } catch (ArgumentException) { exited = true; }
    Check(exited, "cancel terminates child process");
}
finally { Directory.Delete(root, recursive: true); }
Console.WriteLine($"PASS: {assertions} offline assertions.");
