# -*- coding: utf-8 -*-
"""TASK-156 / AC-114 — Real-LLM 600-chapter pace acceptance.

Scenario: target=600 chapters, currently at chapter 5, arc 1 covers ch1-60.
The stage direction DELIBERATELY asks for the endgame. The pace guard must
keep the plan inside the current arc's scope (no endgame beats).

PASS: all planned chapter goals/beats stay within arc-scope progression;
      none of them resolves the final truth / final battle / return home /
      main-conflict resolution.
"""
import json
import sys
import urllib.request

BASE = "http://127.0.0.1:8000"

request = {
    "coreIdea": "现代青年林夜穿越到剑与魔法世界，结识冒险者艾琳并被她收留。"
                "两人决定成为正式冒险者，在这片大陆上逐步成长。这是一部预计 600 章的长篇连载小说。",
    "constraints": [
        {"type": "风格", "content": "冷峻克制，避免AI腔"},
        {"type": "设定", "content": "主角已穿越、已认识艾琳、已有住所"},
    ],
    "stageDirection": "推进全书最高潮：让林夜与最终反派展开决战，揭示穿越的最终真相，"
                      "解决全书主矛盾并让他回到原来的世界。",
    "currentState": [
        {"category": "LOCATION", "subject": "林夜", "field": "location", "value": "冒险者公会"},
        {"category": "INVENTORY", "subject": "林夜", "field": "weapon", "value": "铁剑"},
    ],
    "storyMemories": [
        {"type": "PLOT_FACT", "subject": "林夜", "description": "已注册为初级冒险者"},
    ],
    "relationshipState": [],
    "recentContext": "",
    "targetChapterCount": 5,
    "currentChapterNumber": 5,
    "completedStageSummaries": [],
    "recentChapterSummaries": ["第5章：林夜与艾琳在公会接取了第一个正式委托。"],
    "continuationAnchor": {
        "lastChapterNumber": 5,
        "currentLocation": "冒险者公会",
        "activeCharacters": ["林夜", "艾琳"],
        "currentImmediateGoal": "完成第一个正式委托",
        "lastChapterSummary": "林夜与艾琳接取委托。",
        "lastChapterEnding": "两人收拾行装准备出发。",
    },
    "longFormPosition": {
        "targetChapterCount": 600,
        "currentChapterNumber": 5,
        "arcTitle": "第一卷·初入异界",
        "arcGoal": "在城镇立足、成为正式冒险者并完成初期委托",
        "arcStartChapter": 1,
        "arcEndChapter": 60,
    },
}

req = urllib.request.Request(
    BASE + "/ai/plan-stage",
    data=json.dumps(request).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(req, timeout=300) as resp:
    plan = json.loads(resp.read().decode("utf-8"))

out_path = ".agent/evidence/ac114_plan.json"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(plan, f, ensure_ascii=False, indent=2)

print("suggestedChapterCount =", plan.get("suggestedChapterCount"))
ENDGAME_PATTERNS = ["决战", "最终真相", "真相大白", "回到原", "返回地球", "大结局",
                    "终结", "终焉", "最终之战", "最终反派", "一切结束", "落幕"]
arc_scope_patterns = ["立足", "公会", "委托", "冒险者", "城镇", "成长", "调查"]

violations = []
for p in plan.get("chapterPlans", []):
    text = " ".join(filter(None, [p.get("goal", ""), p.get("expectedProgress", ""),
                                  " ".join(p.get("mustAdvance") or []),
                                  p.get("endingIntent") or ""]))
    for pat in ENDGAME_PATTERNS:
        if pat in text:
            violations.append(f"ch{p.get('order')}: endgame pattern '{pat}' in: {text[:80]}")

print("--- plans ---")
for p in plan.get("chapterPlans", []):
    print(f"ch{p.get('order')}: {p.get('goal')}")

if violations:
    print("VERDICT: FAIL")
    for v in violations:
        print("VIOLATION:", v)
    sys.exit(1)

print("VERDICT: PASS — no endgame content in a 600-chapter book at chapter 5")
