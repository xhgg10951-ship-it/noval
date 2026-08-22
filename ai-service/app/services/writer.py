"""Chapter writing service: /ai/generate-chapter."""
from __future__ import annotations

from app.llm.provider import get_provider, parse_json_response
from app.prompts.builders import build_generate_prompt, log_request_shape
from app.schemas.models import GenerateChapterRequest, GenerateChapterResponse
from app.services.mock_builders import mock_generate


def generate_chapter(req: GenerateChapterRequest) -> GenerateChapterResponse:
    log_request_shape("generate-chapter", req)
    provider = get_provider()
    if provider.is_mock:
        return mock_generate(req)
    system, user = build_generate_prompt(req)
    raw = provider.complete(user, system=system)
    return parse_json_response(GenerateChapterResponse, raw)
