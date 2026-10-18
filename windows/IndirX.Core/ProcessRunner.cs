using System.Diagnostics;
using System.Text;

namespace IndirX.Core;

public sealed class ToolException(string message) : Exception(message);

public static class ProcessRunner
{
    public static async Task<string> RunAsync(string executable, IReadOnlyList<string> arguments, string directory,
        CancellationToken cancellationToken, Action<string>? onLine = null)
    {
        cancellationToken.ThrowIfCancellationRequested();
        var start = new ProcessStartInfo(executable)
        {
            UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true,
            RedirectStandardError = true, StandardOutputEncoding = Encoding.UTF8,
            StandardErrorEncoding = Encoding.UTF8, WorkingDirectory = directory
        };
        foreach (var argument in arguments) start.ArgumentList.Add(argument);
        using var process = new Process { StartInfo = start };
        if (!process.Start()) throw new ToolException("İndirme altyapısı başlatılamadı.");
        using var registration = cancellationToken.Register(() =>
        {
            try { if (!process.HasExited) process.Kill(entireProcessTree: true); }
            catch (InvalidOperationException) { }
            catch (System.ComponentModel.Win32Exception) { }
        });
        var output = new StringBuilder();
        var errors = new Queue<string>();
        var readOutput = Task.Run(async () =>
        {
            try
            {
                while (await process.StandardOutput.ReadLineAsync() is { } line)
                {
                    if (output.Length + line.Length > 32 * 1024 * 1024)
                        throw new ToolException("İçerik listesi çok büyük. Daha küçük bir liste kullan.");
                    output.AppendLine(line);
                    onLine?.Invoke(line);
                }
            }
            catch
            {
                try { if (!process.HasExited) process.Kill(entireProcessTree: true); }
                catch (InvalidOperationException) { }
                catch (System.ComponentModel.Win32Exception) { }
                throw;
            }
        });
        var readErrors = Task.Run(async () =>
        {
            while (await process.StandardError.ReadLineAsync() is { } line)
            {
                errors.Enqueue(line.Length > 1000 ? line[..1000] : line);
                if (errors.Count > 12) errors.Dequeue();
            }
        });
        await process.WaitForExitAsync(CancellationToken.None);
        await Task.WhenAll(readOutput, readErrors);
        cancellationToken.ThrowIfCancellationRequested();
        if (process.ExitCode != 0)
            throw new ToolException(string.Join(Environment.NewLine, errors).Trim() is { Length: > 0 } detail
                ? detail : $"İşlem tamamlanamadı (kod {process.ExitCode}).");
        return output.ToString();
    }
}
