# Copyright (c) Microsoft Corporation. All rights reserved.


def user_message_text(body: dict) -> list[str]:
    if isinstance(body.get("input"), str):
        return [body["input"]]
    messages = body.get("messages")
    if messages is None:
        messages = body.get("input", [])
    texts: list[str] = []
    if not isinstance(messages, list):
        return texts
    for message in messages:
        if not isinstance(message, dict) or message.get("role") != "user":
            continue
        content = message.get("content")
        if isinstance(content, str):
            texts.append(content)
        elif isinstance(content, list):
            texts.extend(
                part["text"]
                for part in content
                if isinstance(part, dict)
                and part.get("type") in ("text", "input_text")
                and isinstance(part.get("text"), str)
            )
    return texts
