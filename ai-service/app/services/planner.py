"""Stage planning service: /ai/plan-stage and /ai/replan-stage."""
from __future__ import annotations

import logging

from app.llm.provider import get_provider, parse_json_response
from app.prompts.builders import build_plan_prompt, log_request_shape
from app.schemas.models import PlanStageRequest, PlanStageResponse
from app.services.mock_builders import mock_plan

logger = logging.getLogger(__name__)


def _plan(req: PlanStageRequest) -> PlanStageResponse:
    provider = get_provider()
    if provider.is_mock:
        return mock_plan(req)
    system, user = build_plan_prompt(req)
    # A real provider can occasionally return truncated or otherwise malformed
    # JSON even when the prompt requires a strict structured response. Retry one
    # fresh completion before failing the request; stage creation is idempotent
    # until this function returns, so this does not duplicate business writes.
    for attempt in range(2):
        raw = provider.complete(user, system=system)
        try:
            return parse_json_response(PlanStageResponse, raw)
        except ValueError:
            if attempt == 1:
                raise
            logger.warning("Planner returned malformed structured output; retrying once")

    raise AssertionError("unreachable")


def plan_stage(req: PlanStageRequest) -> PlanStageResponse:
    log_request_shape("plan-stage", req)
    return _plan(req)


def replan_stage(req: PlanStageRequest) -> PlanStageResponse:
    # Re-planning uses the same structured contract; the caller supplies updated
    # constraints / state to steer a different outcome.
    log_request_shape("replan-stage", req)
    return _plan(req)
