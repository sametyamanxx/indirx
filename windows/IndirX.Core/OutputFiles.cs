namespace IndirX.Core;

public static class OutputFiles
{
    public static bool IsInside(string file, string directory) =>
        Path.GetFullPath(file).StartsWith(Path.GetFullPath(directory).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar,
            OperatingSystem.IsWindows() ? StringComparison.OrdinalIgnoreCase : StringComparison.Ordinal);

    public static async Task<string> CopyUniqueAsync(string source, string folder, CancellationToken cancellationToken)
    {
        Directory.CreateDirectory(folder);
        var name = Path.GetFileName(source);
        for (var index = 1; ; index++)
        {
            cancellationToken.ThrowIfCancellationRequested();
            var candidate = Path.Combine(folder, index == 1 ? name : $"{Path.GetFileNameWithoutExtension(name)} ({index}){Path.GetExtension(name)}");
            FileStream output;
            try { output = new FileStream(candidate, FileMode.CreateNew, FileAccess.Write, FileShare.None, 262144, useAsync: true); }
            catch (IOException) when (File.Exists(candidate)) { continue; }
            var completed = false;
            try
            {
                await using (output)
                await using (var input = new FileStream(source, FileMode.Open, FileAccess.Read, FileShare.Read, 262144, useAsync: true))
                {
                    await input.CopyToAsync(output, cancellationToken);
                    await output.FlushAsync(cancellationToken);
                    cancellationToken.ThrowIfCancellationRequested();
                }
                completed = true;
                return candidate;
            }
            finally { if (!completed) File.Delete(candidate); }
        }
    }
}
