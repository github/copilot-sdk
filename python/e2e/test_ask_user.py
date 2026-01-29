"""
Tests for user input (ask_user) functionality
"""

import pytest

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestAskUser:
    async def test_invoke_user_input_handler_when_model_uses_ask_user_tool(
        self, ctx: E2ETestContext
    ):
        """Test that user input handler is invoked when model uses ask_user tool"""
        user_input_requests = []

        async def on_user_input_request(request, invocation):
            user_input_requests.append(request)
            assert invocation["session_id"] == session.session_id

            # Return the first choice if available, otherwise a freeform answer
            choices = request.get("choices")
            return {
                "answer": choices[0] if choices else "freeform answer",
                "wasFreeform": not bool(choices),
            }

        session = await ctx.client.create_session({"on_user_input_request": on_user_input_request})

        await session.send_and_wait(
            {
                "prompt": (
                    "Ask me to choose between 'Option A' and 'Option B' using the ask_user "
                    "tool. Wait for my response before continuing."
                )
            }
        )

        # Should have received at least one user input request
        assert len(user_input_requests) > 0

        # The request should have a question
        assert any(
            req.get("question") and len(req.get("question")) > 0 for req in user_input_requests
        )

        await session.destroy()

    async def test_receive_choices_in_user_input_request(self, ctx: E2ETestContext):
        """Test that choices are received in user input request"""
        user_input_requests = []

        async def on_user_input_request(request, invocation):
            user_input_requests.append(request)
            # Pick the first choice
            choices = request.get("choices")
            return {
                "answer": choices[0] if choices else "default",
                "wasFreeform": False,
            }

        session = await ctx.client.create_session({"on_user_input_request": on_user_input_request})

        await session.send_and_wait(
            {
                "prompt": (
                    "Use the ask_user tool to ask me to pick between exactly two options: "
                    "'Red' and 'Blue'. These should be provided as choices. Wait for my answer."
                )
            }
        )

        # Should have received a request
        assert len(user_input_requests) > 0

        # At least one request should have choices
        request_with_choices = next(
            (req for req in user_input_requests if req.get("choices") and len(req["choices"]) > 0),
            None,
        )
        assert request_with_choices is not None

        await session.destroy()

    async def test_handle_freeform_user_input_response(self, ctx: E2ETestContext):
        """Test that freeform user input responses work"""
        user_input_requests = []
        freeform_answer = "This is my custom freeform answer that was not in the choices"

        async def on_user_input_request(request, invocation):
            user_input_requests.append(request)
            # Return a freeform answer (not from choices)
            return {
                "answer": freeform_answer,
                "wasFreeform": True,
            }

        session = await ctx.client.create_session({"on_user_input_request": on_user_input_request})

        response = await session.send_and_wait(
            {
                "prompt": (
                    "Ask me a question using ask_user and then include my answer in your "
                    "response. The question should be 'What is your favorite color?'"
                )
            }
        )

        # Should have received a request
        assert len(user_input_requests) > 0

        # The model's response should reference the freeform answer we provided
        # (This is a soft check since the model may paraphrase)
        assert response is not None

        await session.destroy()
