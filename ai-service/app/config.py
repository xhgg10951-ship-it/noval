"""Runtime configuration sourced from environment variables.

The LLM backend is configured via environment variables. Two naming
conventions are accepted so the service works whether you follow the project
docs (LLM_API_KEY / LLM_BASE_URL / LLM_MODEL) or a provider-specific layout
(API_KEY / API_URL / model). The first non-empty value wins.
"""
from __future__ import annotations

import os


def _first_present(*names: str) -> str | None:
    """Return the first non-empty environment variable among *names*."""
    for name in names:
        value = os.environ.get(name)
        if value:
            return value
    return None


class Settings:
    # API key — either convention is accepted.
    llm_api_key: str | None = _first_present("LLM_API_KEY", "API_KEY")
    # Base URL — either convention is accepted (None -> default OpenAI endpoint).
    llm_base_url: str | None = _first_present("LLM_BASE_URL", "API_URL")
    # Model — either convention, case-insensitive for `model` / `MODEL`.
    llm_model: str = _first_present("LLM_MODEL", "MODEL", "model") or "qwen3-8b"
    ai_service_port: int = int(os.environ.get("AI_SERVICE_PORT", "8000"))
    ai_service_host: str = os.environ.get("AI_SERVICE_HOST", "0.0.0.0")
    log_level: str = os.environ.get("LOG_LEVEL", "INFO")

    @property
    def using_mock_llm(self) -> bool:
        return not bool(self.llm_api_key)


settings = Settings()
