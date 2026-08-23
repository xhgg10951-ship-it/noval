"""AC-103 Real-LLM Chapter Length Acceptance harness (TASK-121).

target = 3000 characters
5 chapters
PASS: >= 4/5 land in [2250, 3750] with no obvious padding.

Uses the real LangChainProvider + build_generate_prompt. Generates 5 chapters
sequentially, feeding the prior chapter ending as recent context (mirrors the
real ChapterGenerationService flow via StoryContextReader.getRecentContextWithEnding).
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
from app.prompts.builders import build_generate_prompt
from app.llm.provider import get_provider, parse_json_response
from app.services import writer as writer_service
from app.config import settings


def count_chars(text: str) -> int:
    # Mirrors backend TextLengthUtil: code points, CJK==Latin==1.
    return len(text) if text else 0


def main() -> int:
    print("using_mock_llm =", settings.using_mock_llm)
    assert not settings.using_mock_llm, "Real LLM credential required for AC-103"

    provider = get_provider()

    common_state = [
        StateItem(category="IDENTITY", subject="林夜", field="身份", value="穿越者（已穿越）"),
        StateItem(category="LOCATION", subject="林夜", field="所在地", value="艾琳家中（已住进艾琳提供的房间）"),
        StateItem(category="RELATIONSHIP", subject="林夜", field="与艾琳", value="已认识并被收留"),
        StateItem(category="GOAL", subject="林夜", field="当前目标", value="成为冒险者，接取委托"),
    ]
    common_mem = [
        MemoryItem(type="STORY_MEMORY", subject="林夜", description="林夜是从现代地球穿越而来的青年。"),
        MemoryItem(type="STORY_MEMORY", subject="艾琳", description="艾琳是救下林夜并收留他的本地少女。"),
    ]
    common_rel = [
        RelationshipItem(subjectA="林夜", subjectB="艾琳", description="艾琳救下并收留了林夜，二人初识为同伴。"),
    ]
    constraints = [
        ConstraintItem(type="TONE", content="偏轻松冒险，带日常感"),
        ConstraintItem(type="WORLD", content="剑与魔法异世界，存在冒险者公会"),
    ]

    # ChapterSpec for each of the 5 chapters (guild arc, advancing).
    specs = [
        ("林夜和艾琳前往冒险者公会，在路上交谈，抵达公会门口。",
         ["到达公会所在街道", "两人边走边聊展现关系"],
         ["不得重复穿越/初遇", "不得重新安顿住处"],
         ["出发", "街头见闻", "抵达公会"],
         "以二人站在公会大门前结尾，引出入会。"),
        ("在公会柜台办理入会手续，填入会表，进行简单的资质确认。",
         ["完成入会登记", "获得冒险者身份/徽章"],
         ["不得跳出公会场景", "不得引入无关新角色抢戏"],
         ["柜台登记", "资质询问", "领取徽章"],
         "以获得初级冒险者徽章结尾，准备接取委托。"),
        ("林夜接取第一个委托（例如采集或护送），了解任务要求与报酬。",
         ["接取第一个委托", "明确任务目标与报酬"],
         ["不得拖延不入正题", "不得再次描写入会"],
         ["浏览委托板", "选定委托", "与委托人确认"],
         "以接下委托、约定出发时间结尾。"),
        ("林夜执行委托的途中，遇到第一个小波折并初步展现能力与成长。",
         ["出发执行委托", "遇到并应对第一个波折"],
         ["不得让波折过于离谱", "不得削弱已有角色关系"],
         ["踏上路程", "突发状况", "临机应对"],
         "以化险为夷、对冒险有了实感结尾。"),
        ("林夜完成委托回到公会交付，获得报酬与初步认可，与艾琳汇合。",
         ["交付委托成果", "获得报酬与认可"],
         ["不得虎头蛇尾", "不得偏离已完成委托的事实"],
         ["返回公会", "交付验收", "结算报酬"],
         "以受到公会初步认可、与艾琳一起离开结尾。"),
    ]

    results = []
    recent_context = "前情：林夜穿越后被艾琳救下并收留，决定成为冒险者。"
    out_dir = os.environ.get(
        "ACCEPTANCE_EVIDENCE_DIR",
        os.path.dirname(os.path.abspath(__file__)),
    )
    os.makedirs(out_dir, exist_ok=True)
    ts = dt.datetime.now().strftime("%Y%m%d_%H%M%S")

    for i, (goal, must_advance, must_not, beats, ending) in enumerate(specs, start=4):
        req = GenerateChapterRequest(
            coreIdea="现代青年林夜意外穿越到一个剑与魔法的异世界，被少女艾琳所救，暂住在她家中，正成为冒险者。",
            constraints=constraints,
            stageDirection="林夜成为冒险者并接取、完成第一个委托的连载过程。",
            chapterGoal=goal,
            chapterOrder=i,
            currentState=common_state,
            storyMemories=common_mem,
            relationshipState=common_rel,
            recentContext=recent_context,
            targetCharacters=3000,
            mustAdvance="；".join(must_advance),
            mustNotDo="；".join(must_not),
            storyBeats="；".join(beats),
            endingIntent=ending,
        )
        parsed = writer_service.generate_chapter(req)
        n = count_chars(parsed.content)
        in_band = 2250 <= n <= 3750
        results.append((i, n, in_band, parsed.title, parsed.content))
        print(f"ch{i}: chars={n} in_band={in_band} title={parsed.title}")
        # Feed next chapter's recent context with this chapter's ending excerpt.
        recent_context = f"上一章（第{i}章）《{parsed.title}》结尾：{parsed.content[-300:]}"

    with open(os.path.join(out_dir, f"EVIDENCE_AC103_raw_{ts}.json"), "w", encoding="utf-8") as f:
        json.dump(
            [{"order": o, "chars": n, "in_band": ib, "title": t, "content": c} for (o, n, ib, t, c) in results],
            f, ensure_ascii=False, indent=2,
        )

    in_band_count = sum(1 for (_, _, ib, _, _) in results if ib)
    total = len(results)
    print(f"\nIn-band {in_band_count}/{total} (need >=4/5)")
    verdict = "PASS" if in_band_count >= 4 else "FAIL"
    print("AC-103 VERDICT:", verdict)
    return 0 if verdict == "PASS" else 2


if __name__ == "__main__":
    sys.exit(main())
