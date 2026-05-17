/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text;

namespace System.Text.Unicode;

internal static class Utf8
{
    public static bool TryWrite(Span<byte> destination, string value, out int bytesWritten)
    {
        var byteCount = Encoding.UTF8.GetByteCount(value);
        if (byteCount > destination.Length)
        {
            bytesWritten = 0;
            return false;
        }

        var bytes = Encoding.UTF8.GetBytes(value);
        bytes.CopyTo(destination);
        bytesWritten = byteCount;
        return true;
    }
}
