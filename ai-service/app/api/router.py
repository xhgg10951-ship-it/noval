"""FastAPI router exposing the six AI capabilities."""
from __future__ import annotations

from fastapi import APIRouter

from app.llm.provider import get_provider, parse_json_response
from app.prompts.builders import build_polish_prompt, build_suggest_prompt
from app.schemas.models import (
    ExtractMemoryRequest,
    ExtractMemoryResponse,
    GenerateChapterRequest,
    GenerateChapterResponse,
    PlanStageRequest,
    PlanStageResponse,
    PolishChapterRequest,
    PolishChapterResponse,
    StoryQueryRequest,
    StoryQueryResponse,
    SuggestDirectionsRequest,
    SuggestDirectionsResponse,
)
from app.services import memory_extractor, planner, story_query as story_query_service, writer
from app.services.mock_builders import mock_suggest

router = APIRouter()


@router.post("/plan-stage", response_model=PlanStageResponse)
def plan_stage(req: PlanStageRequest) -> PlanStageResponse:
    return planner.plan_stage(req)


@router.post("/replan-stage", response_model=PlanStageResponse)
def replan_stage(req: PlanStageRequest) -> PlanStageResponse:
    return planner.replan_stage(req)


@router.post("/generate-chapter", response_model=GenerateChapterResponse)
def generate_chapter(req: GenerateChapterRequest) -> GenerateChapterResponse:
    return writer.generate_chapter(req)


@router.post("/polish-chapter", response_model=PolishChapterResponse)
def polish_chapter(req: PolishChapterRequest) -> PolishChapterResponse:
    """v0.1.1 Phase 8 (TASK-167/168): style polish with fact preservation."""
    provider = get_provider()
    if provider.is_mock:
        return PolishChapterResponse(polishedContent=req.content)
    system, user = build_polish_prompt(req)
    raw = provider.complete(user, system=system)
    polished = parse_json_response(PolishChapterResponse, raw)
    return polished


@router.post("/extract-memory", response_model=ExtractMemoryResponse)
def extract_memory(req: ExtractMemoryRequest) -> ExtractMemoryResponse:
    return memory_extractor.extract_memory(req)


@router.post("/suggest-directions", response_model=SuggestDirectionsResponse)
def suggest_directions(req: SuggestDirectionsRequest) -> SuggestDirectionsResponse:
    provider = get_provider()
    if provider.is_mock:
        return mock_suggest(req)
    system, user = build_suggest_prompt(req)
    raw = provider.complete(user, system=system)
    return parse_json_response(SuggestDirectionsResponse, raw)


@router.post("/story-query", response_model=StoryQueryResponse)
def story_query(req: StoryQueryRequest) -> StoryQueryResponse:
    return story_query_service.query_story(req)
