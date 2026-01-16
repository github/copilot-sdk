"""Shared pytest fixtures for e2e tests."""

import pytest
import pytest_asyncio

from .testharness import E2ETestContext


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """Track test failures to avoid writing corrupted snapshots."""
    outcome = yield
    rep = outcome.get_result()
    if rep.when == "call" and rep.failed:
        # Store on the item's stash so the fixture can access it
        item.session.stash.setdefault("any_test_failed", False)
        item.session.stash["any_test_failed"] = True


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def ctx(request):
    """Create and teardown a test context shared across all tests in this module."""
    context = E2ETestContext()
    await context.setup()
    yield context
    any_failed = request.session.stash.get("any_test_failed", False)
    await context.teardown(test_failed=any_failed)


@pytest_asyncio.fixture(autouse=True, loop_scope="module")
async def configure_test(request, ctx):
    """Automatically configure the proxy for each test."""
    # Extract test file name from module (e.g., "test_session" -> "session")
    module_name = request.module.__name__.split(".")[-1]
    if module_name.startswith("test_"):
        test_file = module_name[5:]  # Remove "test_" prefix
    else:
        test_file = module_name

    # Extract test name (e.g., "test_should_create_sessions" -> "should_create_sessions")
    test_name = request.node.name
    if test_name.startswith("test_"):
        test_name = test_name[5:]  # Remove "test_" prefix

    await ctx.configure_for_test(test_file, test_name)
    yield
