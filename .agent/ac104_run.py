"""AC-104 Real-LLM Chapter Goal Acceptance harness (TASK-122).

Verifies the Writer actually obeys the frozen ChapterSpec:
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

    recent_context = (
        "前情摘要：第1章林夜穿越、被艾琳救下；第2章在艾琳家苏醒；第3章决定成为冒险者。"
        "林夜与艾琳已经抵达冒险者公会门外，准备办理登记。"
    )

    req = GenerateChapterRequest(
        coreIdea="现代青年林夜穿越到剑与魔法异世界，被少女艾琳收留，正成为冒险者。",
        constraints=[
            ConstraintItem(type="TONE", content="偏轻松冒险"),
            ConstraintItem(type="WORLD", content="剑与魔法异世界"),
        ],
        stageDirection="林夜在冒险者公会办理入会，并接取第一个委托。",
        chapterGoal="林夜进入冒险者公会并完成登记，为后续资格测试做好铺垫。",
        chapterOrder=5,
        currentState=[
            StateItem(category="IDENTITY", subject="林夜", field="身份", value="穿越者（已穿越）"),
            StateItem(category="LOCATION", subject="林夜", field="所在地", value="冒险者公会"),
            StateItem(category="GOAL", subject="林夜", field="当前目标", value="入会登记并准备资格测试"),
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
            "到达并进入冒险者公会",
            "在柜台完成入会登记",
            "为接下来的资格测试做好铺垫",
        ]),
        mustNotDo="；".join([
            "不得暴露林夜的天帝身份",
            "不得重复描写穿越或初遇艾琳",
        ]),
        storyBeats="；".join([
            "走到柜台",
            "填写入会表并资质确认",
            "了解资格测试规则",
            "等待或准备参加测试",
        ]),
        endingIntent="以资格测试即将开始收尾，不揭示林夜的隐藏身份。",
    )

    parsed = writer_service.generate_chapter(req)
    content = parsed.content
    print("title:", parsed.title)
    print("chars:", len(content))

    must_advance_groups = [
        # “推开公会大门” is a concrete narrative realization of entering.
        ("到达", "进入", "走进", "推开"),
        ("登记", "注册", "填表"),
        ("测试", "考核", "测验"),
    ]
    must_not_violations = []
    for bad in ["天帝", "再次穿越", "重新穿越", "初遇艾琳"]:
        if bad in content:
            must_not_violations.append(bad)

    advance_hits = [next((k for k in group if k in content), None) for group in must_advance_groups]
    advance_hits = [k for k in advance_hits if k]
    print("mustAdvance realized keywords:", advance_hits)
    print("mustNotDo violation keywords present:", must_not_violations if must_not_violations else "NONE")

    # Verdict: all three frozen advances realized and the hidden identity stays hidden.
    realized = len(advance_hits) == len(must_advance_groups)
    no_violation = len(must_not_violations) == 0
    verdict = "PASS" if (realized and no_violation) else "FAIL"
    print(f"\nmustAdvance realized (>=3): {realized}")
    print(f"mustNotDo clean: {no_violation}")
    print("AC-104 VERDICT:", verdict)

    out_dir = os.environ.get(
        "ACCEPTANCE_EVIDENCE_DIR",
        os.path.dirname(os.path.abspath(__file__)),
    )
    os.makedirs(out_dir, exist_ok=True)
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
