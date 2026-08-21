"""Pluggable LLM provider.

Without an API key the system uses MockProvider so the entire generation
pipeline runs and is testable locally (per AGENTS.md: mock for early
integration, clearly marked). Provide an API key (LLM_API_KEY or API_KEY) to
enable the real LangChain-backed provider.

All configuration is read from `app.config.settings`, so a single, consistent
set of environment variables drives both the /health report and the live
provider selection.
"""
from __future__ import annotations

import json
import re
from typing import List, Optional, Type, TypeVar

from app.config import settings

T = TypeVar("T")


# --------------------------------------------------------------------------- #
# Robust JSON extraction
# --------------------------------------------------------------------------- #
def _strip_think(text: str) -> str:
    """Remove qwen3 (and similar) <think>...</think> reasoning blocks."""
    return re.sub(r"<think>.*?</think>", "", text, flags=re.DOTALL)


def _strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    text = re.sub(r"//[^\n]*", "", text)
    return text


def _strip_trailing_commas(text: str) -> str:
    """Drop trailing commas before } or ] (a common LLM mistake).

    Uses a lookahead so the closing bracket is preserved.
    """
    return re.sub(r",\s*(?=[}\]])", "", text)


def _extract_all_balanced(text: str, open_ch: str, close_ch: str) -> List[str]:
    """Return every balanced open_ch..close_ch span in `text`.

    Brace depth is tracked while correctly skipping characters that appear
    inside JSON string literals (so braces inside a string value do not
    disturb the count). This avoids the classic greedy ``{.*}`` over-match
    that breaks the moment the model emits the object twice or adds prose
    containing stray braces.
    """
    results: List[str] = []
    n = len(text)
    i = 0
    while i < n:
        start = text.find(open_ch, i)
        if start == -1:
            break
        depth = 0
        in_str = False
        esc = False
        j = start
        matched = False
        while j < n:
            ch = text[j]
            if in_str:
                if esc:
                    esc = False
                elif ch == "\\":
                    esc = True
                elif ch == '"':
                    in_str = False
            else:
                if ch == '"':
                    in_str = True
                elif ch == open_ch:
                    depth += 1
                elif ch == close_ch:
                    depth -= 1
                    if depth == 0:
                        results.append(text[start:j + 1])
                        matched = True
                        break
            j += 1
        i = (j + 1) if matched else (start + 1)
    return results


def parse_json_response(cls: Type[T], raw: str) -> T:
    """Best-effort parse of an LLM string into a Pydantic model `cls`.

    Tolerant of ``` fences, <think> reasoning blocks, prose wrappers,
    comments, trailing commas and unescaped control characters — all common
    ways an LLM emits "almost JSON". Raises on hard failure so the caller can
    surface a clear error instead of returning garbage.
    """
    text = (raw or "").strip()

    # 1. Fences
    fenced = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL)
    if fenced:
        text = fenced.group(1).strip()

    # 2. Reasoning blocks
    text = _strip_think(text).strip()

    # 3. Candidate spans: objects first, then arrays
    candidates: List[str] = []
    for open_ch, close_ch in (("{", "}"), ("[", "]")):
        candidates.extend(_extract_all_balanced(text, open_ch, close_ch))
    # Also try the whole cleaned blob as a last resort.
    candidates.append(text)

    last_err: Optional[Exception] = None
    for cand in candidates:
        cleaned = _strip_trailing_commas(_strip_comments(cand))
        if not cleaned.strip():
            continue
        # Primary: standard JSON, tolerating control chars inside strings.
        try:
            return cls(**json.loads(cleaned, strict=False))
        except Exception as exc:  # noqa: BLE001 - try next candidate
            last_err = exc
        # Fallback: json5 if installed (handles single quotes, etc.)
        try:
            import json5  # type: ignore
            return cls(**json5.loads(cleaned))
        except Exception:  # noqa: BLE001 - json5 not available / still invalid
            pass

    raise ValueError(
        f"Could not parse LLM output as {cls.__name__}: {last_err}\n"
        f"--- raw head ---\n{text[:500]}"
    )


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

        # qwen3 (and several reasoning models) emit a <think>…</think> block
        # by default, which both slows generation and pollutes the JSON. We
        # disable thinking when we recognise such a model. This is a harmless
        # no-op for models/endpoints that ignore the flag.
        model_kwargs: dict = {}
        if "qwen" in (settings.llm_model or "").lower():
            model_kwargs["extra_body"] = {"enable_thinking": False}

        self._llm = ChatOpenAI(
            model=settings.llm_model,
            temperature=0.7,
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url,  # None -> default OpenAI endpoint
            model_kwargs=model_kwargs,
        )

    def complete(self, prompt: str, *, system: Optional[str] = None) -> str:
        messages = []
        if system:
            messages.append(("system", system))
        messages.append(("human", prompt))
        return self._llm.invoke(messages).content


def get_provider() -> LLMProvider:
    # Single source of truth: the configured API key.
    if settings.llm_api_key:
        return LangChainProvider()
    return MockProvider()
