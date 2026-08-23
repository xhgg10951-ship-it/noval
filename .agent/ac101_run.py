"""AC-101 Real-LLM Continuation Acceptance harness.

Known prior state (frozen in TASK-112):
  已穿越 / 已认识艾琳 / 已住进艾琳提供的房间
New Stage direction:
  第二天和艾琳去冒险者公会

PASS: Planner continues from existing state — no repeated crossing /
first meeting / re-acquiring housing / repeating completed stage.
"""
from __future__ import annotations

import json
import os
import sys
import datetime as dt

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ai-service"))

from app.schemas.models import (
    PlanStageRequest,
    ConstraintItem,
    StateItem,
    MemoryItem,
    RelationshipItem,
    ContinuationAnchor,
    PlanStageResponse,
)
from app.prompts.builders import build_plan_prompt
from app.llm.provider import get_provider, parse_json_response
from app.config import settings


def main() -> int:
    print("using_mock_llm =", settings.using_mock_llm)
    assert not settings.using_mock_llm, "Real LLM credential required for AC-101"

    req = PlanStageRequest(
        coreIdea="现代青年林夜意外穿越到一个剑与魔法的异世界，被少女艾琳所救，暂住在她家中。",
        constraints=[
            ConstraintItem(type="TONE", content="偏轻松冒险，带日常感"),
            ConstraintItem(type="WORLD", content="剑与魔法异世界，存在冒险者公会"),
        ],
        stageDirection="第二天，林夜和艾琳一起去冒险者公会，办理入会手续并接取第一个委托。",
        currentState=[
            StateItem(category="IDENTITY", subject="林夜", field="身份", value="穿越者（已穿越）"),
            StateItem(category="LOCATION", subject="林夜", field="所在地", value="艾琳家中（已住进艾琳提供的房间）"),
            StateItem(category="RELATIONSHIP", subject="林夜", field="与艾琳", value="已认识并被收留"),
            StateItem(category="GOAL", subject="林夜", field="当前目标", value="成为冒险者，接取委托"),
        ],
        storyMemories=[
            MemoryItem(type="STORY_MEMORY", subject="林夜", description="林夜是从现代地球穿越而来的青年。"),
            MemoryItem(type="STORY_MEMORY", subject="艾琳", description="艾琳是救下林夜并收留他的本地少女。"),
        ],
        relationshipState=[
            RelationshipItem(subjectA="林夜", subjectB="艾琳", description="艾琳救下并收留了林夜，二人初识为同伴。"),
        ],
        recentContext="",
        targetChapterCount=None,
        currentChapterNumber=3,
        completedStageSummaries=[
            "第 1 阶段：林夜穿越到异世界，被艾琳救下。",
            "第 2 阶段：林夜在艾琳家中安顿，初步了解这个世界。",
        ],
        recentChapterSummaries=[
            "第 1 章：林夜穿越，濒死时被艾琳发现并救回。",
            "第 2 章：林夜在艾琳家中苏醒，了解异世界基本状况。",
            "第 3 章：林夜决定成为冒险者，艾琳提议明天带他去公会。",
        ],
        continuationAnchor=ContinuationAnchor(
            lastChapterNumber=3,
            currentLocation="艾琳家中",
            activeCharacters=["林夜", "艾琳"],
            currentImmediateGoal="前往冒险者公会办理入会",
            lastChapterSummary="林夜决定成为冒险者，艾琳提议明天带他去公会。",
            lastChapterEnding="夜深了，林夜想着明天的公会之行，渐渐睡去。",
        ),
    )

    system, user = build_plan_prompt(req)
    provider = get_provider()

    raw = provider.complete(user, system=system)

    out_dir = os.environ.get(
        "ACCEPTANCE_EVIDENCE_DIR",
        os.path.join(os.path.dirname(os.path.abspath(__file__))),
    )
    os.makedirs(out_dir, exist_ok=True)
    ts = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
    with open(os.path.join(out_dir, f"EVIDENCE_AC101_prompt_{ts}.txt"), "w", encoding="utf-8") as f:
        f.write("===== SYSTEM =====\n" + system + "\n\n===== USER =====\n" + user)
    with open(os.path.join(out_dir, f"EVIDENCE_AC101_raw_{ts}.txt"), "w", encoding="utf-8") as f:
        f.write(raw)

    parsed = parse_json_response(PlanStageResponse, raw)
    print("suggestedChapterCount =", parsed.suggestedChapterCount)
    for p in parsed.chapterPlans:
        print(f"  ch{p.order}: goal={p.goal}")
        print(f"    mustAdvance={p.mustAdvance}")
        print(f"    mustNotDo={p.mustNotDo}")
        print(f"    targetCharacters={p.targetCharacters}")

    # ---- AC-101 verdict heuristics ----
    # Scan ONLY the affirmative planned content (goal / mustAdvance / expectedProgress
    # / storyBeats / endingIntent). The model's own `mustNotDo` prohibition text must
    # NOT count as a violation -- it is the Planner forbidding those acts.
    forbidden_patterns = [
        "再次穿越", "重新穿越", "二次穿越", "初遇", "初次遇见", "第一次遇见",
        "重新获得", "再次获得", "找住处", "找房子", "重新安顿", "重新开篇", "从头开始",
    ]
    affirmative = []
    for p in parsed.chapterPlans:
        affirmative.append(p.goal or "")
        affirmative.extend(p.mustAdvance or [])
        affirmative.append(p.expectedProgress or "")
        affirmative.extend(p.storyBeats or [])
        affirmative.append(p.endingIntent or "")
    aff_text = " ".join(affirmative)
    hits = [p for p in forbidden_patterns if p in aff_text]
    must_not_text = " ".join(
        " ".join(p.mustNotDo or []) for p in parsed.chapterPlans
    )
    print("\nFORBIDDEN PATTERN HITS in affirmative plan content:", hits if hits else "NONE")
    print("Model mustNotDo (should forbid repeats):", must_not_text[:200] if must_not_text else "(none)")

    # AC-101 PASS requires: continuation from existing state (chapter >=4, not a reset)
    # AND no affirmative repeat of resolved plot.
    continued = parsed.suggestedChapterCount >= 1 and all(
        p.order >= 4 for p in parsed.chapterPlans
    )
    verdict = "PASS" if (continued and not hits) else "FAIL"
    print("continued_from_existing (order>=4):", continued)
    print("\nAC-101 VERDICT:", verdict)
    return 0 if verdict == "PASS" else 2


if __name__ == "__main__":
    sys.exit(main())
