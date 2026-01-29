#!/usr/bin/env python3
"""
Managing Local Files - Using Copilot to organize and manage files.
Run: python managing_local_files.py [--quick]
"""

import asyncio
import os
import sys
from pathlib import Path

from copilot import CopilotClient
from copilot.types import SessionEventType


# =============================================================================
# Event Handler
# =============================================================================


def create_event_handler(verbose=True):
    """Create an event handler for progress display."""

    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"\n🤖 Copilot:\n{event.data.content}\n")
        elif event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA and verbose:
            delta = getattr(event.data, "delta_content", "")
            if delta:
                print(delta, end="", flush=True)
        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            print(f"  ⚙️  Starting: {event.data.tool_name}")
        elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
            print(f"  ✓ Completed")
        elif event.type == SessionEventType.SESSION_ERROR:
            message = getattr(event.data, "message", str(event.data))
            print(f"  ✗ Error: {message}")

    return handle_event


# =============================================================================
# Permission Handler
# =============================================================================


def create_permission_handler(auto_approve=False):
    """Create a permission handler for file operations."""

    def handle_permission(request, context):
        kind = request.get("kind", "unknown")

        if auto_approve:
            print(f"  🔓 Auto-approved: {kind}")
            return {"kind": "approved"}

        print(f"\n⚠️  Permission Request: {kind}")
        try:
            response = input("   Approve? (y/n): ").strip().lower()
            if response in ("y", "yes"):
                return {"kind": "approved"}
            return {"kind": "denied-interactively-by-user"}
        except (EOFError, KeyboardInterrupt):
            return {"kind": "denied-interactively-by-user"}

    return handle_permission


# =============================================================================
# File Organization Strategies
# =============================================================================


async def organize_by_extension(session, target_folder, dry_run=True):
    """Organize files by extension into subfolders."""
    action = "show me a preview of" if dry_run else "execute"
    prompt = f"""
Analyze the files in "{target_folder}" and {action} organizing them by file extension.

Grouping: images/, documents/, videos/, audio/, archives/, code/, data/, other/
{"Only show the plan, DO NOT move any files." if dry_run else "Create folders and move files."}
"""
    await session.send_and_wait({"prompt": prompt}, timeout=120.0)


async def organize_by_date(session, target_folder, dry_run=True):
    """Organize files by modification date into year/month folders."""
    action = "show me a preview of" if dry_run else "execute"
    prompt = f"""
Analyze the files in "{target_folder}" and {action} organizing them by modification date.

Structure: year/month folders (e.g., "2024/01-January/")
{"Only show the plan, DO NOT move any files." if dry_run else "Create folders and move files."}
"""
    await session.send_and_wait({"prompt": prompt}, timeout=120.0)


async def organize_by_size(session, target_folder, dry_run=True):
    """Organize files by size into size-based folders."""
    action = "show me a preview of" if dry_run else "execute"
    prompt = f"""
Analyze the files in "{target_folder}" and {action} organizing them by file size.

Categories: tiny-under-1kb/, small-under-1mb/, medium-under-100mb/, large-under-1gb/, huge-over-1gb/
{"Only show the plan, DO NOT move any files." if dry_run else "Create folders and move files."}
"""
    await session.send_and_wait({"prompt": prompt}, timeout=120.0)


async def smart_organize(session, target_folder, dry_run=True):
    """Let Copilot analyze and suggest the best organization strategy."""
    prompt = f"""
Analyze ALL files in "{target_folder}" and suggest the best organization.

Consider file names, types, sizes, and patterns.
{"Show what files would go where (DO NOT move anything)" if dry_run else "Create folders and organize files"}
"""
    await session.send_and_wait({"prompt": prompt}, timeout=180.0)


# =============================================================================
# Interactive Demo
# =============================================================================


async def interactive_demo():
    """Run an interactive file organization demo."""
    print("=" * 60)
    print("📁 Copilot File Organizer")
    print("=" * 60)

    default_folder = os.path.expanduser("~/Downloads")
    print(f"\nDefault folder: {default_folder}")
    folder_input = input("Enter folder path (or Enter for default): ").strip()
    target_folder = folder_input if folder_input else default_folder

    if not Path(target_folder).is_dir():
        print(f"✗ Error: '{target_folder}' is not a valid directory")
        return

    print("\nStrategies:")
    print("  1. By extension  2. By date  3. By size  4. Smart organize")

    strategy_input = input("Choose (1-4): ").strip()
    strategies = {
        "1": ("extension", organize_by_extension),
        "2": ("date", organize_by_date),
        "3": ("size", organize_by_size),
        "4": ("smart", smart_organize),
    }
    strategy_name, strategy_func = strategies.get(strategy_input, strategies["1"])

    dry_run = input("Dry run only? (Y/n): ").strip().lower() not in ("n", "no")

    print(f"\n{'📋 DRY RUN' if dry_run else '⚠️ LIVE MODE'} | {target_folder} | {strategy_name}\n")

    client = CopilotClient({"log_level": "error"})

    try:
        await client.start()
        session = await client.create_session({
            "on_permission_request": create_permission_handler(auto_approve=dry_run),
        })
        session.on(create_event_handler(verbose=False))

        await strategy_func(session, target_folder, dry_run)

        print("\n" + "-" * 40)
        print("💡 Ask follow-up questions or 'exit' to quit.")

        while True:
            try:
                user_input = input("\nYou: ").strip()
            except (EOFError, KeyboardInterrupt):
                print("\n👋 Goodbye!")
                break

            if user_input.lower() in ("exit", "quit", "q"):
                break

            if user_input:
                await session.send_and_wait({"prompt": user_input}, timeout=120.0)

        await session.destroy()

    except Exception as e:
        print(f"✗ Error: {e}")
    finally:
        await client.stop()


# =============================================================================
# Quick Start
# =============================================================================


async def quick_start():
    """Minimal file organization example."""
    print("\n=== Quick Start: File Organization ===\n")

    client = CopilotClient()

    try:
        await client.start()
        session = await client.create_session()

        def handle_event(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                print(f"\n{event.data.content}\n")

        session.on(handle_event)

        target = os.path.expanduser("~/Downloads")
        await session.send_and_wait(
            {"prompt": f"List 10 files in '{target}' and suggest organization. Don't move anything."},
            timeout=60.0,
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Main
# =============================================================================


async def main():
    """Main entry point."""
    if len(sys.argv) > 1 and sys.argv[1] == "--quick":
        await quick_start()
    else:
        await interactive_demo()


if __name__ == "__main__":
    asyncio.run(main())
