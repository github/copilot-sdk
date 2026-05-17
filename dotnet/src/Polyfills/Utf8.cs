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

        if (byteCount == value.Length)
        {
            for (var i = 0; i < value.Length; i++)
            {
                destination[i] = (byte)value[i];
            }

            bytesWritten = byteCount;
            return true;
        }

        var bytes = Encoding.UTF8.GetBytes(value);
        bytes.CopyTo(destination);
        bytesWritten = byteCount;
        return true;
    }
}
