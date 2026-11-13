# ---------------------------------------------------------------------------------------------
# Copyright (c) Microsoft Corporation. All rights reserved.
# ---------------------------------------------------------------------------------------------

"""Authentication host E2E coverage."""

import os

import pytest
import pytest_asyncio

from .testharness import E2ETestContext


@pytest_asyncio.fixture(loop_scope="module")
async def ctx(request):
    context = E2ETestContext()
    await context.setup()
    yield context
    await context.teardown(
        test_failed=request.session.stash.get("any_test_failed", False)
        or bool(os.environ.get("GITHUB_ACTIONS"))
    )


@pytest.mark.asyncio(loop_scope="module")
@pytest.mark.parametrize(
    ("copilot_host", "gh_host", "expected_host"),
    [
        ("tenant.ghe.example", "fallback.ghe.example", "https://tenant.ghe.example"),
        ("", "fallback.ghe.example", "https://fallback.ghe.example"),
        ("", "", "https://github.com"),
    ],
)
async def test_should_report_selected_github_auth_host(ctx, copilot_host, gh_host, expected_host):
    ctx.add_runtime_env("COPILOT_GH_HOST", copilot_host)
    ctx.add_runtime_env("GH_HOST", gh_host)

    await ctx.client.start()
    status = await ctx.client.get_auth_status()
    assert status.isAuthenticated, status.statusMessage
    assert status.host == expected_host
