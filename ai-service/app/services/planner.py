"""Stage planning service: /ai/plan-stage and /ai/replan-stage."""
from __future__ import annotations

from app.llm.provider import get_provider, parse_json_response
from app.prompts.builders import build_plan_prompt
from app.schemas.models import PlanStageRequest, PlanStageResponse
from app.services.mock_builders import mock_plan


def _plan(req: PlanStageRequest) -> PlanStageResponse:
    provider = get_provider()
    if provider.is_mock:
        return mock_plan(req)
    system, user = build_plan_prompt(req)
    raw = provider.complete(user, system=system)
    return parse_json_response(PlanStageResponse, raw)


def plan_stage(req: PlanStageRequest) -> PlanStageResponse:
    return _plan(req)


def replan_stage(req: PlanStageRequest) -> PlanStageResponse:
    # Re-planning uses the same structured contract; the caller supplies updated
    # constraints / state to steer a different outcome.
    return _plan(req)
