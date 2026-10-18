using System.Text.Json;

namespace IndirX.Core;

public sealed record UserSettings(string Folder, string VideoFormat = "mp4", string AudioFormat = "mp3", bool AudioMode = false);

public sealed class SettingsStore(string file)
{
    public UserSettings Load()
    {
        try
        {
            var value = JsonSerializer.Deserialize<UserSettings>(File.ReadAllText(file));
            if (value != null && !string.IsNullOrWhiteSpace(value.Folder)) return value;
        }
        catch (Exception e) when (e is IOException or JsonException or UnauthorizedAccessException) { }
        return new UserSettings(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "Downloads", "IndirX"));
    }

    public void Save(UserSettings value)
    {
        var parent = Path.GetDirectoryName(Path.GetFullPath(file))!;
        Directory.CreateDirectory(parent);
        var temporary = Path.Combine(parent, "settings-" + Guid.NewGuid().ToString("N") + ".tmp");
        try
        {
            File.WriteAllText(temporary, JsonSerializer.Serialize(value));
            File.Move(temporary, file, overwrite: true);
        }
        finally { if (File.Exists(temporary)) File.Delete(temporary); }
    }
}
