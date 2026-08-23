# -*- coding: utf-8 -*-
"""AC-106 real-LLM semantic half: replan continues after completed chapters."""

import json
import os
import sys
import urllib.request


BASE = "http://127.0.0.1:8000"
OUT_DIR = os.environ.get("ACCEPTANCE_EVIDENCE_DIR", ".agent/evidence")

request = {
    "coreIdea": "林夜穿越到剑与魔法世界，被艾琳收留，正成长为冒险者。",
    "constraints": [
        {"type": "设定", "content": "林夜已穿越、已认识艾琳、已有住所"},
        {"type": "风格", "content": "冷峻克制"},
    ],
    "stageDirection": "将尚未完成的剧情收束为3章：调查幽影森林失踪案，发现幕后线索并安全返回公会。",
    "currentState": [
        {"category": "LOCATION", "subject": "林夜", "field": "location", "value": "幽影森林入口"},
        {"category": "GOAL", "subject": "林夜", "field": "current_goal", "value": "调查失踪案"},
    ],
    "storyMemories": [
        {"type": "PLOT_FACT", "subject": "林夜", "description": "已完成冒险者登记并接取失踪案委托"},
    ],
    "relationshipState": [
        {"subjectA": "林夜", "subjectB": "艾琳", "description": "共同执行委托的同伴"},
    ],
    "recentContext": "第3章结尾：林夜与艾琳抵达幽影森林入口，发现失踪者留下的脚印。",
    "targetChapterCount": 3,
    "currentChapterNumber": 3,
    "completedStageSummaries": [
        "第1章：林夜与艾琳前往公会。",
        "第2章：林夜完成登记并取得冒险者资格。",
        "第3章：两人接取失踪案并抵达森林入口。",
    ],
    "recentChapterSummaries": [
        "第2章：林夜完成登记并取得冒险者资格。",
        "第3章：两人接取失踪案并抵达森林入口。",
    ],
    "continuationAnchor": {
        "lastChapterNumber": 3,
        "currentLocation": "幽影森林入口",
        "activeCharacters": ["林夜", "艾琳"],
        "currentImmediateGoal": "调查失踪案",
        "lastChapterSummary": "两人接取失踪案并抵达森林入口。",
        "lastChapterEnding": "湿泥里的脚印一直延伸向森林深处。",
    },
}

http_request = urllib.request.Request(
    BASE + "/ai/replan-stage",
    data=json.dumps(request).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(http_request, timeout=300) as response:
    plan = json.loads(response.read().decode("utf-8"))

plans = plan.get("chapterPlans") or []
affirmative = " ".join(
    " ".join(
        [
            str(item.get("goal") or ""),
            str(item.get("expectedProgress") or ""),
            " ".join(item.get("mustAdvance") or []),
            " ".join(item.get("storyBeats") or []),
        ]
    )
    for item in plans
)
forbidden = [
    pattern for pattern in ["再次穿越", "重新穿越", "初遇", "找住处", "重新登记", "再次登记"]
    if pattern in affirmative
]
continuation_terms = [term for term in ["森林", "失踪", "调查", "线索", "脚印"] if term in affirmative]
verdict = len(plans) == 3 and not forbidden and len(continuation_terms) >= 2

evidence = {
    "request": request,
    "response": plan,
    "planCount": len(plans),
    "forbiddenRepeatHits": forbidden,
    "continuationTermHits": continuation_terms,
    "verdict": "PASS" if verdict else "FAIL",
}
os.makedirs(OUT_DIR, exist_ok=True)
with open(os.path.join(OUT_DIR, "ac106_replan_semantic.json"), "w", encoding="utf-8") as file:
    json.dump(evidence, file, ensure_ascii=False, indent=2)

print("plan_count:", len(plans))
print("forbidden_repeat_hits:", forbidden)
print("continuation_term_hits:", continuation_terms)
print("AC-106 SEMANTIC VERDICT:", evidence["verdict"])
sys.exit(0 if verdict else 1)
