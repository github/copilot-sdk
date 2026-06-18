"""Tests for the experimental-API runtime gating in :mod:`copilot.experimental`."""

from __future__ import annotations

import inspect
import warnings

import pytest

import copilot
from copilot import (
    ExperimentalWarning,
    allow_experimental,
    experimental,
    is_experimental,
    set_experimental_policy,
)


def test_experimental_function_warns() -> None:
    @experimental
    def add_one(x: int) -> int:
        return x + 1

    with pytest.warns(ExperimentalWarning):
        result = add_one(1)
    assert result == 2
    assert is_experimental(add_one)


def test_experimental_with_since_includes_version_in_message() -> None:
    @experimental(since="1.2")
    def g() -> str:
        return "ok"

    with pytest.warns(ExperimentalWarning, match="experimental since 1.2"):
        assert g() == "ok"


async def test_experimental_async_function_warns() -> None:
    @experimental
    async def double(x: int) -> int:
        return x * 2

    with pytest.warns(ExperimentalWarning):
        result = await double(3)
    assert result == 6


def test_experimental_class_warns_on_construction() -> None:
    @experimental
    class Widget:
        def __init__(self, value: int) -> None:
            self.value = value

    with pytest.warns(ExperimentalWarning):
        instance = Widget(5)
    assert instance.value == 5
    assert is_experimental(Widget)
    assert is_experimental(instance)


def test_stable_callable_does_not_warn() -> None:
    def stable() -> int:
        return 1

    assert not is_experimental(stable)
    with warnings.catch_warnings():
        warnings.simplefilter("error")
        assert stable() == 1


def test_allow_experimental_silences_warning() -> None:
    @experimental
    def f() -> int:
        return 7

    with warnings.catch_warnings():
        warnings.simplefilter("error", ExperimentalWarning)
        with allow_experimental():
            assert f() == 7


def test_policy_error_raises() -> None:
    @experimental
    def f() -> int:
        return 1

    with warnings.catch_warnings():
        set_experimental_policy("error")
        with pytest.raises(ExperimentalWarning):
            f()


def test_policy_ignore_silences() -> None:
    @experimental
    def f() -> int:
        return 1

    with warnings.catch_warnings():
        warnings.simplefilter("error", ExperimentalWarning)
        set_experimental_policy("ignore")
        assert f() == 1


def test_invalid_policy_raises_value_error() -> None:
    with pytest.raises(ValueError):
        set_experimental_policy("bogus")


def test_signature_is_preserved() -> None:
    @experimental
    def f(a: int, b: str = "x") -> str:
        return f"{a}{b}"

    sig = inspect.signature(f)
    assert list(sig.parameters) == ["a", "b"]
    with allow_experimental():
        assert f(1) == "1x"


def test_public_exports() -> None:
    assert copilot.experimental is experimental
    assert copilot.ExperimentalWarning is ExperimentalWarning
    assert copilot.is_experimental is is_experimental
    assert copilot.allow_experimental is allow_experimental
    assert copilot.set_experimental_policy is set_experimental_policy
