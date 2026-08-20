"""Story query service: /ai/story-query."""
from __future__ import annotations

from app.llm.provider import get_provider, parse_json_response
from app.prompts.builders import build_query_prompt
from app.schemas.models import StoryQueryRequest, StoryQueryResponse
from app.services.mock_builders import mock_query


def query_story(req: StoryQueryRequest) -> StoryQueryResponse:
    provider = get_provider()
    if provider.is_mock:
        return mock_query(req)
    system, user = build_query_prompt(req)
    raw = provider.complete(user, system=system)
    return parse_json_response(StoryQueryResponse, raw)
