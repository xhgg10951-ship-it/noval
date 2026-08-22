"""AC-104 Real-LLM Chapter Goal Acceptance harness (TASK-122).

Verifies the Writer actually obeys the ChapterSpec:
  - Must Advance items are realized in the chapter content;
  - Must Not items are NOT violated;
  - Chapter does not loop around irrelevant prior-chapter details
    (we seed a prior-chapter detail that should NOT be re-centralized).

Uses the real LangChainProvider via writer.generate_chapter (with length guard).
Heuristics are conservative keyword/phrase checks; AC-104 is a semantic gate,
so the harness reports evidence and a best-effort verdict, not a hard parse.
"""
from __future__ import annotations

import json
import os
import sys
import datetime as dt

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ai-service"))

from app.schemas.models import (
    GenerateChapterRequest,
    ConstraintItem,
    StateItem,
    MemoryItem,
    RelationshipItem,
    GenerateChapterResponse,
)
from app.services import writer as writer_service
from app.prompts.builders import build_generate_prompt
from app.llm.provider import get_provider
from app.config import settings


def main() -> int:
    print("using_mock_llm =", settings.using_mock_llm)
    assert not settings.using_mock_llm, "Real LLM credential required for AC-104"

    # Seed an irrelevant prior detail that must NOT be re-centralized:
    # an old bread-buying detail from chapter 1 that AC-104 says chapters
    # must not loop around.
    recent_context = (
        "前情摘要：第1章林夜穿越、被艾琳救下；第2章在艾琳家苏醒；第3章决定成为冒险者。"
        "（注：第1章曾提到林夜在街角买了一块面包，这是无关低价值细节，本章无需再提。）"
    )

    req = GenerateChapterRequest(
        coreIdea="现代青年林夜穿越到剑与魔法异世界，被少女艾琳收留，正成为冒险者。",
        constraints=[
            ConstraintItem(type="TONE", content="偏轻松冒险"),
            ConstraintItem(type="WORLD", content="剑与魔法异世界"),
        ],
        stageDirection="林夜在冒险者公会办理入会，并接取第一个委托。",
        chapterGoal="林夜在公会柜台完成入会登记，领取冒险者徽章，并接取第一个委托。",
        chapterOrder=5,
        currentState=[
            StateItem(category="IDENTITY", subject="林夜", field="身份", value="穿越者（已穿越）"),
            StateItem(category="LOCATION", subject="林夜", field="所在地", value="冒险者公会"),
            StateItem(category="GOAL", subject="林夜", field="当前目标", value="入会并接取委托"),
        ],
        storyMemories=[
            MemoryItem(type="STORY_MEMORY", subject="林夜", description="林夜是从现代地球穿越而来的青年。"),
        ],
        relationshipState=[
            RelationshipItem(subjectA="林夜", subjectB="艾琳", description="艾琳救下并收留了林夜。"),
        ],
        recentContext=recent_context,
        targetCharacters=3000,
        mustAdvance="；".join([
            "在公会柜台完成入会登记",
            "领取初级冒险者徽章",
            "向委托发布板了解并接取第一个委托",
        ]),
        mustNotDo="；".join([
            "不得重复描写穿越或初遇艾琳",
            "不得重新安顿住处",
            "不得把无关旧细节（如买面包）当作本章重点",
        ]),
        storyBeats="；".join([
            "走到柜台",
            "填写入会表并资质确认",
            "领取徽章",
            "浏览委托板选定第一个委托",
        ]),
        endingIntent="以接下第一个委托、约定出发时间结尾。",
    )

    parsed = writer_service.generate_chapter(req)
    content = parsed.content
    print("title:", parsed.title)
    print("chars:", len(content))

    must_advance = ["入会", "登记", "徽章", "委托"]
    must_not_violations = []
    for bad in ["穿越", "初遇", "面包", "住处", "安顿"]:
        if bad in content:
            must_not_violations.append(bad)

    advance_hits = [k for k in must_advance if k in content]
    print("mustAdvance realized keywords:", advance_hits)
    print("mustNotDo violation keywords present:", must_not_violations if must_not_violations else "NONE")

    # Verdict: all three core advances realized + no forbidden-loop keyword.
    realized = len(advance_hits) >= 3
    no_violation = len(must_not_violations) == 0
    verdict = "PASS" if (realized and no_violation) else "FAIL"
    print(f"\nmustAdvance realized (>=3): {realized}")
    print(f"mustNotDo clean: {no_violation}")
    print("AC-104 VERDICT:", verdict)

    out_dir = os.path.dirname(os.path.abspath(__file__))
    ts = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
    with open(os.path.join(out_dir, f"EVIDENCE_AC104_{ts}.json"), "w", encoding="utf-8") as f:
        json.dump({
            "title": parsed.title, "chars": len(content),
            "advance_hits": advance_hits, "violations": must_not_violations,
            "verdict": verdict, "content": content,
        }, f, ensure_ascii=False, indent=2)
    return 0 if verdict == "PASS" else 2


if __name__ == "__main__":
    sys.exit(main())
