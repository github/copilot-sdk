/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Polyfills for System.IO APIs not available on .NET Framework.
// These are test-only and not optimized for production use.

#if !NET8_0_OR_GREATER

using System.Threading;
using System.Threading.Tasks;

namespace System.IO;

internal static class TestDownlevelPathExtensions
{
    extension(Path)
    {
        public static string Join(string? path1, string? path2)
            => JoinCore(path1, path2);

        public static string Join(string? path1, string? path2, string? path3)
            => JoinCore(path1, path2, path3);

        public static string Join(string? path1, string? path2, string? path3, string? path4)
            => JoinCore(path1, path2, path3, path4);

        public static string Join(params string?[] paths)
            => JoinCore(paths);
    }

    private static string JoinCore(params string?[] paths)
    {
        var sb = new System.Text.StringBuilder();
        foreach (var path in paths)
        {
            if (string.IsNullOrEmpty(path))
            {
                continue;
            }

            if (sb.Length > 0 && !EndsWithSeparator(sb))
            {
                sb.Append(Path.DirectorySeparatorChar);
            }

            sb.Append(path);
        }

        return sb.ToString();
    }

    private static bool EndsWithSeparator(System.Text.StringBuilder sb) =>
        sb.Length > 0 && (sb[sb.Length - 1] == Path.DirectorySeparatorChar || sb[sb.Length - 1] == Path.AltDirectorySeparatorChar);
}

internal static class TestDownlevelFileExtensions
{
    extension(File)
    {
        public static Task<string> ReadAllTextAsync(string path, CancellationToken cancellationToken = default)
        {
            cancellationToken.ThrowIfCancellationRequested();
            return Task.Run(() => File.ReadAllText(path), cancellationToken);
        }

        public static Task WriteAllTextAsync(string path, string? contents, CancellationToken cancellationToken = default)
        {
            cancellationToken.ThrowIfCancellationRequested();
            return Task.Run(() => File.WriteAllText(path, contents), cancellationToken);
        }

        public static Task<byte[]> ReadAllBytesAsync(string path, CancellationToken cancellationToken = default)
        {
            cancellationToken.ThrowIfCancellationRequested();
            return Task.Run(() => File.ReadAllBytes(path), cancellationToken);
        }

        public static Task WriteAllBytesAsync(string path, byte[] bytes, CancellationToken cancellationToken = default)
        {
            cancellationToken.ThrowIfCancellationRequested();
            return Task.Run(() => File.WriteAllBytes(path, bytes), cancellationToken);
        }

        public static Task AppendAllTextAsync(string path, string? contents, CancellationToken cancellationToken = default)
        {
            cancellationToken.ThrowIfCancellationRequested();
            return Task.Run(() => File.AppendAllText(path, contents), cancellationToken);
        }

        public static void Move(string sourceFileName, string destFileName, bool overwrite)
        {
            if (overwrite && File.Exists(destFileName))
            {
                File.Delete(destFileName);
            }

            File.Move(sourceFileName, destFileName);
        }
    }
}

internal static class TestDownlevelFileSystemInfoExtensions
{
#pragma warning disable CA1822 // Mark members as static - extension members cannot be static
    extension(FileSystemInfo info)
    {
        public string? LinkTarget => null;

        public FileSystemInfo? ResolveLinkTarget(bool returnFinalTarget) => null;
    }
#pragma warning restore CA1822
}

internal static class TestDownlevelTextReaderExtensions
{
    extension(TextReader reader)
    {
        public Task<string> ReadToEndAsync(CancellationToken cancellationToken)
        {
            if (cancellationToken.IsCancellationRequested)
            {
                return Task.FromCanceled<string>(cancellationToken);
            }

            return reader.ReadToEndAsync();
        }
    }
}

#endif
