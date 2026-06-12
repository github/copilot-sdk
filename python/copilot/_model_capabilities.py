"""Model capability override types."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ModelVisionLimitsOverride:
    supported_media_types: list[str] | None = None
    max_prompt_images: int | None = None
    max_prompt_image_size: int | None = None


@dataclass
class ModelLimitsOverride:
    max_prompt_tokens: int | None = None
    max_output_tokens: int | None = None
    max_context_window_tokens: int | None = None
    vision: ModelVisionLimitsOverride | None = None


@dataclass
class ModelSupportsOverride:
    vision: bool | None = None
    reasoning_effort: bool | None = None


@dataclass
class ModelCapabilitiesOverride:
    supports: ModelSupportsOverride | None = None
    limits: ModelLimitsOverride | None = None


def capabilities_to_dict(caps: ModelCapabilitiesOverride) -> dict:
    result: dict = {}
    if caps.supports is not None:
        supports: dict = {}
        if caps.supports.vision is not None:
            supports["vision"] = caps.supports.vision
        if caps.supports.reasoning_effort is not None:
            supports["reasoningEffort"] = caps.supports.reasoning_effort
        if supports:
            result["supports"] = supports
    if caps.limits is not None:
        limits: dict = {}
        if caps.limits.max_prompt_tokens is not None:
            limits["max_prompt_tokens"] = caps.limits.max_prompt_tokens
        if caps.limits.max_output_tokens is not None:
            limits["max_output_tokens"] = caps.limits.max_output_tokens
        if caps.limits.max_context_window_tokens is not None:
            limits["max_context_window_tokens"] = caps.limits.max_context_window_tokens
        if caps.limits.vision is not None:
            vision: dict = {}
            if caps.limits.vision.supported_media_types is not None:
                vision["supported_media_types"] = caps.limits.vision.supported_media_types
            if caps.limits.vision.max_prompt_images is not None:
                vision["max_prompt_images"] = caps.limits.vision.max_prompt_images
            if caps.limits.vision.max_prompt_image_size is not None:
                vision["max_prompt_image_size"] = caps.limits.vision.max_prompt_image_size
            if vision:
                limits["vision"] = vision
        if limits:
            result["limits"] = limits
    return result
