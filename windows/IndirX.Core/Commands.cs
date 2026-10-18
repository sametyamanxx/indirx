namespace IndirX.Core;

public sealed record MediaFormat(string Name, string Value, bool IsAudio);

public static class Commands
{
    public static readonly MediaFormat[] Formats =
    [
        new("MP4", "mp4", false), new("MKV", "mkv", false), new("WEBM", "webm", false),
        new("MOV", "mov", false), new("AVI", "avi", false), new("FLV", "flv", false),
        new("MP3", "mp3", true), new("M4A", "m4a", true), new("FLAC", "flac", true),
        new("OPUS", "opus", true), new("OGG", "vorbis", true), new("WAV", "wav", true),
        new("AAC", "aac", true), new("ALAC", "alac", true)
    ];
    public const string FilePrefix = "__INDIRX_FILE__";
    public const string ProgressPrefix = "__INDIRX_PROGRESS__";

    private static List<string> Base(string tools) =>
    [
        "--ignore-config", "--no-plugin-dirs", "--no-remote-components", "--no-js-runtimes",
        "--js-runtimes", "deno:" + Path.Combine(tools, "deno.exe"),
        "--ffmpeg-location", tools, "--use-extractors", PlatformPolicy.Extractors,
        "--socket-timeout", "30", "--retries", "3", "--color", "never", "--encoding", "utf-8",
        "--xff", "never"
    ];

    public static IReadOnlyList<string> Inspect(string url, string tools)
    {
        Validate(url);
        var args = Base(tools);
        args.AddRange(["--dump-single-json", "--flat-playlist", "--skip-download", "--yes-playlist", "--playlist-end", "500", "--", url]);
        return args;
    }

    public static IReadOnlyList<string> Download(string url, MediaFormat format, string tools)
    {
        Validate(url);
        if (!Formats.Contains(format)) throw new ArgumentException("Geçersiz dosya biçimi.");
        var args = Base(tools);
        args.AddRange([
            "--no-playlist", "--newline", "--no-mtime", "--windows-filenames", "--trim-filenames", "140",
            "--no-overwrites", "--embed-metadata", "--output", "%(title)s.%(ext)s",
            "--progress-template", "download:" + ProgressPrefix + "%(progress._percent_str)s",
            "--print", "after_move:" + FilePrefix + "%(filepath)j"
        ]);
        // --print implies simulation unless an after-video stage is given; be explicit.
        args.Add("--no-simulate");
        if (format.IsAudio)
        {
            args.AddRange(["--format", "bestaudio/best", "--extract-audio", "--audio-format", format.Value, "--audio-quality", "0"]);
            if (format.Value is "mp3" or "m4a" or "flac" or "opus" or "vorbis")
                args.AddRange(["--embed-thumbnail", "--convert-thumbnails", "jpg"]);
        }
        else args.AddRange(["--format", "bestvideo*+bestaudio/best", "--merge-output-format", "mkv", "--recode-video", format.Value]);
        args.AddRange(["--", url]);
        return args;
    }

    private static void Validate(string url)
    {
        if (!PlatformPolicy.Accepts(url)) throw new ArgumentException(PlatformPolicy.Error);
    }
}
