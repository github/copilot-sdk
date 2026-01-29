#!/usr/bin/env python3
"""
PR Age Chart Generator - Visualizes pull request age distribution.
Run: python pr_visualization.py [--repo owner/repo] [--output path]
"""

import asyncio
import os
import re
import subprocess
import sys

from copilot import CopilotClient
from copilot.types import SessionEventType


# =============================================================================
# Git & GitHub Detection
# =============================================================================


def is_git_repo():
    """Check if current directory is inside a Git repository."""
    try:
        subprocess.run(["git", "rev-parse", "--git-dir"], check=True, capture_output=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


def get_github_remote():
    """Extract the GitHub owner/repo from git remote URL."""
    try:
        result = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            check=True, capture_output=True, text=True,
        )
        remote_url = result.stdout.strip()

        if match := re.search(r"git@github\.com:(.+/.+?)(?:\.git)?$", remote_url):
            return match[1]
        if match := re.search(r"https://github\.com/(.+/.+?)(?:\.git)?$", remote_url):
            return match[1]
        return None
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None


def get_git_branch():
    """Get current git branch name."""
    try:
        result = subprocess.run(
            ["git", "branch", "--show-current"],
            check=True, capture_output=True, text=True,
        )
        return result.stdout.strip()
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None


def parse_args():
    """Parse command line arguments."""
    args = sys.argv[1:]
    result = {}

    if "--repo" in args:
        idx = args.index("--repo")
        if idx + 1 < len(args):
            result["repo"] = args[idx + 1]

    if "--output" in args:
        idx = args.index("--output")
        if idx + 1 < len(args):
            result["output"] = args[idx + 1]

    if "--help" in args or "-h" in args:
        result["help"] = "true"

    return result


def print_help():
    """Print usage information."""
    print("""
PR Age Chart Generator

Usage: python pr_visualization.py [options]

Options:
    --repo OWNER/REPO    GitHub repository (e.g., github/copilot-sdk)
    --output PATH        Output path for chart (default: pr-age-chart.png)
    --help               Show this help
""")


def prompt_for_repo():
    """Prompt user for a repository."""
    return input("Enter GitHub repo (owner/repo): ").strip()


# =============================================================================
# Event Handler
# =============================================================================


def create_event_handler(verbose=True):
    """Create an event handler for displaying progress."""

    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"\n🤖 {event.data.content}\n")
        elif event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA and verbose:
            delta = getattr(event.data, "delta_content", "")
            if delta:
                print(delta, end="", flush=True)
        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            print(f"  ⚙️  {event.data.tool_name}")
        elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
            print("  ✓ Done")
        elif event.type == SessionEventType.SESSION_ERROR:
            message = getattr(event.data, "message", str(event.data))
            print(f"  ✗ Error: {message}")

    return handle_event


# =============================================================================
# PR Analysis Functions
# =============================================================================


async def analyze_pr_age(session, owner, repo_name, output_path):
    """Analyze PR age distribution and generate a chart."""
    prompt = f"""
Fetch open pull requests for {owner}/{repo_name}.

Calculate age and last activity for each PR.
Generate a bar chart grouping by age (<1 day, 1-3 days, 3-7 days, 1-2 weeks, 2-4 weeks, >1 month).
Save as "{output_path}".
Summarize: total open PRs, average/median age, oldest PR, stale PRs (>2 weeks).
"""
    await session.send_and_wait({"prompt": prompt}, timeout=300.0)


async def analyze_pr_by_author(session, owner, repo_name):
    """Analyze PRs grouped by author."""
    prompt = f"""
Fetch open pull requests for {owner}/{repo_name}.
Group by author, show: count per author, average age, stale PRs.
Generate a horizontal bar chart and save as "pr-by-author.png".
"""
    await session.send_and_wait({"prompt": prompt}, timeout=300.0)


async def analyze_pr_review_status(session, owner, repo_name):
    """Analyze PR review status."""
    prompt = f"""
Fetch open pull requests for {owner}/{repo_name}.
Analyze review status: waiting for review, changes requested, approved but not merged.
Identify bottlenecks. Generate a pie chart as "pr-review-status.png".
"""
    await session.send_and_wait({"prompt": prompt}, timeout=300.0)


# =============================================================================
# Interactive Loop
# =============================================================================


async def interactive_loop(session):
    """Run interactive follow-up loop."""
    print("\n" + "-" * 50)
    print("💡 Ask follow-up questions or 'exit' to quit.")
    print("Examples: 'Show oldest PRs', 'Group by author', 'Generate pie chart'\n")

    while True:
        try:
            user_input = input("You: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n👋 Goodbye!")
            break

        if user_input.lower() in ("exit", "quit", "q"):
            break

        if user_input:
            await session.send_and_wait({"prompt": user_input}, timeout=300.0)


# =============================================================================
# Main
# =============================================================================


async def main():
    """Main entry point for PR Age Chart Generator."""
    print("=" * 60)
    print("🔍 PR Age Chart Generator")
    print("=" * 60)

    args = parse_args()

    if args.get("help"):
        print_help()
        return

    # Determine repository
    repo = None
    if "repo" in args:
        repo = args["repo"]
        print(f"\n📦 Using: {repo}")
    elif is_git_repo():
        detected = get_github_remote()
        if detected:
            repo = detected
            branch = get_git_branch()
            print(f"\n📦 Detected: {repo}" + (f" ({branch})" if branch else ""))
        else:
            repo = prompt_for_repo()
    else:
        repo = prompt_for_repo()

    if not repo or "/" not in repo:
        print("❌ Invalid repo format. Expected: owner/repo")
        sys.exit(1)

    owner, repo_name = repo.split("/", 1)
    output_path = args.get("output", "pr-age-chart.png")

    client = CopilotClient({"log_level": "error"})

    try:
        await client.start()
        print("✓ Connected to Copilot")

        session = await client.create_session({
            "system_message": {
                "mode": "append",
                "content": f"""
<context>
Analyzing PRs for: {owner}/{repo_name}
Working directory: {os.getcwd()}
Output path: {output_path}
</context>
""",
            },
        })

        session.on(create_event_handler(verbose=False))

        print("\n📊 Starting PR analysis...\n")
        await analyze_pr_age(session, owner, repo_name, output_path)
        await interactive_loop(session)
        await session.destroy()

    except Exception as e:
        print(f"\n✗ Error: {e}")
        sys.exit(1)

    finally:
        await client.stop()


if __name__ == "__main__":
    asyncio.run(main())
