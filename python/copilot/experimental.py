"""Runtime gating for experimental Copilot SDK APIs.

Python has no compile step, so the closest analog to C# ``[Experimental]`` and
Java ``@CopilotExperimental`` is a runtime diagnostic that fires when an
experimental API is used and that the consumer must explicitly silence. This
mirrors the long-established pattern used by NumPy
(``VisibleDeprecationWarning``) and matplotlib.

Experimental SDK functions, methods, and classes are decorated with
:func:`experimental`. Using one emits an :class:`ExperimentalWarning` that
points at the calling code::

    import warnings

    import copilot

    # Opt in for a region of code (the equivalent of suppressing the
    # compiler diagnostic in C#/Java):
    with copilot.allow_experimental():
        await client.some_experimental_method()

    # Opt in globally:
    warnings.filterwarnings("ignore", category=copilot.ExperimentalWarning)

    # Forbid experimental APIs (recommended for CI):
    copilot.set_experimental_policy("error")
    #   or set the COPILOT_EXPERIMENTAL=error environment variable
"""

from __future__ import annotations

import functools
import inspect
import os
import warnings
from collections.abc import Callable
from contextlib import contextmanager
from typing import TYPE_CHECKING, Any, Literal, TypeVar, overload

if TYPE_CHECKING:
    from collections.abc import Iterator

__all__ = [
    "ExperimentalWarning",
    "allow_experimental",
    "experimental",
    "is_experimental",
    "set_experimental_policy",
]

_T = TypeVar("_T")

_MARKER = "__copilot_experimental__"

_POLICY_ACTIONS: dict[str, Literal["default", "error", "ignore"]] = {
    "warn": "default",
    "error": "error",
    "ignore": "ignore",
}


class ExperimentalWarning(Warning):
    """Warning category for Copilot SDK APIs that are experimental.

    Experimental APIs may change in backwards-incompatible ways or be removed
    entirely in any release, without notice and without a deprecation period.
    They are surfaced as a warning (rather than hidden in docs) so that usage is
    a deliberate, acknowledged choice.
    """


def _message(qualname: str, since: str | None) -> str:
    msg = (
        f"{qualname} is an experimental Copilot SDK API and may change or be "
        "removed in any release without notice."
    )
    if since:
        msg += f" (experimental since {since})"
    msg += (
        " Silence this warning with `copilot.allow_experimental()` to acknowledge "
        "that you are depending on an experimental API."
    )
    return msg


@overload
def experimental(obj: _T) -> _T: ...


@overload
def experimental(*, since: str | None = ...) -> Callable[[_T], _T]: ...


def experimental(obj: Any = None, *, since: str | None = None) -> Any:
    """Mark a function, method, or class as experimental.

    Usable bare (``@experimental``) or parameterized
    (``@experimental(since="1.2")``). Calling the decorated function/method or
    constructing the decorated class emits an :class:`ExperimentalWarning`
    pointing at the caller. The wrapper is signature- and type-preserving, so
    decorated APIs keep their original typing.
    """

    def decorate(target: Any) -> Any:
        qualname = getattr(target, "__qualname__", getattr(target, "__name__", repr(target)))
        message = _message(qualname, since)

        if inspect.isclass(target):
            original_init = target.__init__

            @functools.wraps(original_init)
            def __init__(self: Any, *args: Any, **kwargs: Any) -> None:
                warnings.warn(message, ExperimentalWarning, stacklevel=2)
                original_init(self, *args, **kwargs)

            target.__init__ = __init__
            setattr(target, _MARKER, True)
            return target

        if inspect.iscoroutinefunction(target):

            @functools.wraps(target)
            async def async_wrapper(*args: Any, **kwargs: Any) -> Any:
                warnings.warn(message, ExperimentalWarning, stacklevel=2)
                return await target(*args, **kwargs)

            setattr(async_wrapper, _MARKER, True)
            return async_wrapper

        @functools.wraps(target)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            warnings.warn(message, ExperimentalWarning, stacklevel=2)
            return target(*args, **kwargs)

        setattr(wrapper, _MARKER, True)
        return wrapper

    # @experimental (bare): ``obj`` is the decorated target.
    if obj is not None:
        return decorate(obj)
    # @experimental(...): return the decorator.
    return decorate


def is_experimental(obj: Any) -> bool:
    """Return ``True`` if ``obj`` (a function, method, class, or instance) is experimental."""
    if getattr(obj, _MARKER, False):
        return True
    return bool(getattr(type(obj), _MARKER, False))


@contextmanager
def allow_experimental() -> Iterator[None]:
    """Locally silence :class:`ExperimentalWarning`.

    This is the explicit, scoped opt-in — the Python equivalent of suppressing
    the compiler diagnostic in C#/Java::

        with copilot.allow_experimental():
            await client.some_experimental_method()
    """
    with warnings.catch_warnings():
        warnings.simplefilter("ignore", ExperimentalWarning)
        yield


def set_experimental_policy(policy: str) -> None:
    """Set the process-wide policy for experimental-API usage.

    * ``"warn"``   — emit an :class:`ExperimentalWarning` (the default)
    * ``"error"``  — raise on use (recommended for CI to forbid experimental APIs)
    * ``"ignore"`` — silence entirely (a blanket opt-in)

    This can also be driven by the ``COPILOT_EXPERIMENTAL`` environment variable,
    which is applied automatically on import.
    """
    normalized = policy.strip().lower()
    action = _POLICY_ACTIONS.get(normalized)
    if action is None:
        raise ValueError(
            f"unknown experimental policy: {policy!r} (expected 'warn', 'error', or 'ignore')"
        )
    warnings.filterwarnings(action, category=ExperimentalWarning)


# Honor an environment-driven default so CI can flip an entire run to "error".
_env_policy = os.environ.get("COPILOT_EXPERIMENTAL")
if _env_policy:
    set_experimental_policy(_env_policy)
