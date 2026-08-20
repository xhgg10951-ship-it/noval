"""Runtime configuration sourced from environment variables."""
from __future__ import annotations

import os


class Settings:
    llm_api_key: str | None = os.environ.get("LLM_API_KEY")
    llm_model: str = os.environ.get("LLM_MODEL", "gpt-4o-mini")
    llm_base_url: str | None = os.environ.get("LLM_BASE_URL")
    ai_service_port: int = int(os.environ.get("AI_SERVICE_PORT", "8000"))
    ai_service_host: str = os.environ.get("AI_SERVICE_HOST", "0.0.0.0")
    log_level: str = os.environ.get("LOG_LEVEL", "INFO")

    @property
    def using_mock_llm(self) -> bool:
        return not bool(self.llm_api_key)


settings = Settings()
