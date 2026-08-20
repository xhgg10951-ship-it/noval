"""Pluggable LLM provider.

Without LLM_API_KEY the system uses MockProvider so the entire generation
pipeline runs and is testable locally (per AGENTS.md §55: mock for early
integration, clearly marked). Supply LLM_API_KEY to enable the real
LangChain-backed provider.
"""
from __future__ import annotations

import json
import os
import re
from typing import Optional, Type, TypeVar

T = TypeVar("T")


def parse_json_response(cls: Type[T], raw: str) -> T:
    """Best-effort parse of an LLM string into a Pydantic model.

    Handles ```json fences and extracts the first JSON object if the model
    wraps it in prose. Raises on hard failure so callers surface a clear error.
    """
    text = (raw or "").strip()
    fenced = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL)
    if fenced:
        text = fenced.group(1).strip()
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        obj = re.search(r"\{.*\}", text, re.DOTALL)
        if not obj:
            raise
        data = json.loads(obj.group(0))
    return cls(**data)


class LLMProvider:
    is_mock: bool = False

    def complete(self, prompt: str, *, system: Optional[str] = None) -> str:
        raise NotImplementedError


class MockProvider(LLMProvider):
    is_mock = True

    def complete(self, prompt: str, *, system: Optional[str] = None) -> str:
        # Mock responses are produced deterministically by the service layer
        # (see app.services.mock_builders) so we should never reach here for
        # the structured endpoints. Raise to make that explicit.
        raise RuntimeError("MockProvider.complete should not be called directly")


class LangChainProvider(LLMProvider):
    is_mock = False

    def __init__(self) -> None:
        from langchain_openai import ChatOpenAI

        self._llm = ChatOpenAI(
            model=os.environ.get("LLM_MODEL", "gpt-4o-mini"),
            temperature=0.7,
            api_key=os.environ["LLM_API_KEY"],
            base_url=os.environ.get("LLM_BASE_URL"),  # None -> default
        )

    def complete(self, prompt: str, *, system: Optional[str] = None) -> str:
        messages = []
        if system:
            messages.append(("system", system))
        messages.append(("human", prompt))
        return self._llm.invoke(messages).content


def get_provider() -> LLMProvider:
    if os.environ.get("LLM_API_KEY"):
        return LangChainProvider()
    return MockProvider()
