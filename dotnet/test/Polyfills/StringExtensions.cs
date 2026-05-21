/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Polyfills for string APIs not available on .NET Framework.
// These are test-only and not optimized for production use.

#if !NET8_0_OR_GREATER

using System.Text.RegularExpressions;

namespace System;

internal static class TestDownlevelStringExtensions
{
    extension(string s)
    {
        public bool Contains(string value, StringComparison comparisonType)
            => s.IndexOf(value, comparisonType) >= 0;

        public bool Contains(char value)
            => s.IndexOf(value) >= 0;

        public bool StartsWith(char value)
            => s.Length > 0 && s[0] == value;

        public bool EndsWith(char value)
            => s.Length > 0 && s[s.Length - 1] == value;

        public string[] Split(char separator, StringSplitOptions options)
            => s.Split([separator], options);

        public string ReplaceLineEndings()
            => Regex.Replace(s, @"\r\n|\r|\n", "\n");

        public string ReplaceLineEndings(string replacementText)
            => Regex.Replace(s, @"\r\n|\r|\n", replacementText);
    }

    extension(string)
    {
        public static string Create<TState>(int length, TState state, TestStringCreateSpanAction<TState> action)
        {
            var array = new char[length];
            action(array, state);
            return new string(array);
        }
    }

    internal delegate void TestStringCreateSpanAction<in TArg>(Span<char> span, TArg arg);
}

#endif
