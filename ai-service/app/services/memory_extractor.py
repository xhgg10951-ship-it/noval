"""Memory extraction service: /ai/extract-memory."""
from __future__ import annotations

from app.llm.provider import get_provider, parse_json_response
from app.prompts.builders import build_extract_prompt
from app.schemas.models import ExtractMemoryRequest, ExtractMemoryResponse
from app.services.mock_builders import mock_extract


def extract_memory(req: ExtractMemoryRequest) -> ExtractMemoryResponse:
    provider = get_provider()
    if provider.is_mock:
        return mock_extract(req)
    system, user = build_extract_prompt(req)
    raw = provider.complete(user, system=system)
    return parse_json_response(ExtractMemoryResponse, raw)
