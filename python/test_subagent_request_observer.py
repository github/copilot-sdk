# Copyright (c) Microsoft Corporation. All rights reserved.

import pytest

from e2e.inference_request import user_message_text


@pytest.mark.parametrize(
    ("body", "expected"),
    [
        ({"messages": [{"role": "user", "content": "chat"}]}, ["chat"]),
        ({"input": "responses"}, ["responses"]),
        ({"input": [{"role": "user", "content": "responses"}]}, ["responses"]),
        (
            {
                "input": [
                    {"role": "assistant", "content": "ignored"},
                    {"type": "function_call_output", "output": "ignored"},
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": "chat"},
                            {"type": "input_text", "text": "responses"},
                            {"type": "input_image", "text": "ignored"},
                            {"type": "input_text", "text": 42},
                            None,
                            "ignored",
                        ],
                    },
                ]
            },
            ["chat", "responses"],
        ),
        ({}, []),
    ],
)
def test_user_message_text(body, expected):
    assert user_message_text(body) == expected
