using System;
using System.ComponentModel;
using System.Diagnostics;
using System.Runtime.InteropServices;

namespace GitHub.Copilot.Ci;

// The supervisor joins before spawning anything. All descendants inherit this
// nested job, including children whose parent exits before the next snapshot.
internal sealed class WindowsJob : IDisposable
{
    private readonly IntPtr handle;

    public WindowsJob()
    {
        handle = CreateJobObject(IntPtr.Zero, null);
        Check(handle != IntPtr.Zero);
        SetKillOnClose(true);
        Check(AssignProcessToJobObject(handle, Process.GetCurrentProcess().Handle));
    }

    public int[] ProcessIds()
    {
        // A bounded allocation, with a visible error rather than a truncated tree.
        const int capacity = 4096;
        IntPtr buffer = Marshal.AllocHGlobal(8 + capacity * IntPtr.Size);
        try
        {
            Check(QueryInformationJobObject(handle, 3, buffer, 8 + capacity * IntPtr.Size, IntPtr.Zero));
            int count = Marshal.ReadInt32(buffer, 4);
            int[] ids = new int[count];
            for (int i = 0; i < count; i++)
                ids[i] = checked((int)Marshal.ReadIntPtr(buffer, 8 + i * IntPtr.Size));
            return ids;
        }
        finally
        {
            Marshal.FreeHGlobal(buffer);
        }
    }

    public void Complete()
    {
        // Only the supervisor may remain when disabling kill-on-close.
        int[] ids = ProcessIds();
        if (ids.Length != 1 || ids[0] != Environment.ProcessId)
            throw new InvalidOperationException("The owned job is not empty.");
        SetKillOnClose(false);
    }

    public bool Owns(IntPtr process)
    {
        Check(IsProcessInJob(process, handle, out bool owned));
        return owned;
    }

    public void Abort(int exitCode) => Check(TerminateJobObject(handle, unchecked((uint)exitCode)));

    private void SetKillOnClose(bool enabled)
    {
        var limits = new ExtendedLimits();
        limits.Basic.LimitFlags = enabled ? 0x2000u : 0;
        Check(SetInformationJobObject(handle, 9, ref limits, Marshal.SizeOf<ExtendedLimits>()));
    }

    private static void Check(bool succeeded)
    {
        if (!succeeded)
            throw new Win32Exception(Marshal.GetLastWin32Error());
    }

    public void Dispose() => Check(CloseHandle(handle));

    [StructLayout(LayoutKind.Sequential)]
    private struct BasicLimits
    {
        public long PerProcessUserTime, PerJobUserTime;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSet, MaximumWorkingSet;
        public uint ActiveProcessLimit;
        public UIntPtr Affinity;
        public uint PriorityClass, SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct IoCounters
    {
        public ulong ReadOperations, WriteOperations, OtherOperations;
        public ulong ReadBytes, WriteBytes, OtherBytes;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct ExtendedLimits
    {
        public BasicLimits Basic;
        public IoCounters Io;
        public UIntPtr ProcessMemory, JobMemory, PeakProcessMemory, PeakJobMemory;
    }

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr CreateJobObject(IntPtr attributes, string? name);

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool SetInformationJobObject(IntPtr job, int infoClass, ref ExtendedLimits info, int length);

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool IsProcessInJob(IntPtr process, IntPtr job, [MarshalAs(UnmanagedType.Bool)] out bool owned);

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool TerminateJobObject(IntPtr job, uint exitCode);

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool QueryInformationJobObject(IntPtr job, int infoClass, IntPtr info, int length, IntPtr returnedLength);

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CloseHandle(IntPtr handle);
}
