# -*- coding: utf-8 -*-
"""TASK-165 / AC-105 — bread-loop regression (real LLM).

A chapter mentions an ordinary bread purchase. The Memory v2 extractor must
classify it as TRANSIENT_DETAIL with low importance / CHAPTER scope and a
non-AUTO suggested action, so the writer never re-receives it as context.
"""
import json
import os
import sys
import urllib.request

BASE = "http://127.0.0.1:8000"

request = {
    "chapterContent": (
        "清晨的阳光洒进小屋。林夜起床后去街角的面包店买了一个普通的面包当早餐，"
        "边走边吃。回到公会大厅时，艾琳已经在等他了。她把一张委托单拍在桌上："
        "幽影森林又有人失踪了，这次是铁匠的小儿子。两人决定立刻出发。"
        "出了城门，林夜注意到森林上空的云层泛着不寻常的紫色微光。"
    ),
    "chapterSummary": "林夜与艾琳接下调查幽影森林失踪案的委托并启程。",
    "chapterOrder": 6,
    "existingState": [
        {"category": "LOCATION", "subject": "林夜", "field": "location", "value": "冒险者公会"},
    ],
    "constraints": [],
}

req = urllib.request.Request(
    BASE + "/ai/extract-memory",
    data=json.dumps(request).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(req, timeout=300) as resp:
    out = json.loads(resp.read().decode("utf-8"))

out_dir = os.environ.get("ACCEPTANCE_EVIDENCE_DIR", ".agent/evidence")
os.makedirs(out_dir, exist_ok=True)

bread_rows = [c for c in out.get("candidates", [])
              if "面包" in " ".join(str(c.get(k) or "") for k in ("type", "subject", "field", "value"))]
missing_v2_fields = any(
    c.get("importance") is None or c.get("scope") is None
    for c in out.get("candidates", []))
print("--- all candidates ---")
for c in out.get("candidates", []):
    print(f"[{c.get('type')}] {c.get('subject')}/{c.get('field')} imp={c.get('importance')} "
          f"scope={c.get('scope')} action={c.get('suggestedAction')}: "
          f"{str(c.get('value'))[:60]}")


def would_reach_writer(c):
    """Mirrors the Java processing pipeline: does this candidate ever become
    writer-visible context (Current State slot or selected story memory)?"""
    if c.get("suggestedAction") != "AUTO":
        return False  # IGNORE / REVIEW never auto-applied
    if int(c.get("importance") or 3) <= 2:
        return False  # TASK-164 selection: importance <= 2 excluded
    if c.get("type") == "TRANSIENT_DETAIL":
        return False  # TASK-164 selection: transient excluded
    if str(c.get("field") or "").startswith("item:"):
        # TASK-165 Java guard: low-importance inventory demoted to REVIEW
        return int(c.get("importance") or 0) >= 4
    return True


classification_ok = any(
    c.get("type") == "TRANSIENT_DETAIL"
    and int(c.get("importance") or 0) == 1
    and c.get("scope") == "CHAPTER"
    for c in bread_rows
)
reaching = [c for c in bread_rows if would_reach_writer(c)]

# Frozen AC-105 also requires three unrelated subsequent ChapterSpecs not to
# loop around the low-value detail. Generate those chapters with only the
# writer-visible memory projection.
writer_memories = [
    {
        "type": c.get("type"),
        "subject": c.get("subject"),
        "description": str(c.get("value") or ""),
    }
    for c in out.get("candidates", [])
    if would_reach_writer(c)
]
chapter_specs = [
    (7, "林夜与艾琳沿森林小径调查失踪者足迹", "发现通往溪谷的新鲜脚印"),
    (8, "二人在溪谷辨认魔兽活动痕迹并避开伏击", "确认失踪案与异常魔兽有关"),
    (9, "林夜和艾琳追踪线索到废弃哨塔", "在哨塔入口发现失踪者遗留物"),
]
generated = []
for order, goal, ending in chapter_specs:
    writer_request = {
        "coreIdea": "林夜与艾琳调查幽影森林失踪案。",
        "constraints": [{"type": "风格", "content": "冷峻克制"}],
        "stageDirection": "调查幽影森林失踪案并逐步逼近真相。",
        "chapterGoal": goal,
        "chapterOrder": order,
        "currentState": request["existingState"],
        "storyMemories": writer_memories,
        "relationshipState": [
            {"subjectA": "林夜", "subjectB": "艾琳", "description": "并肩调查的同伴"}
        ],
        "recentContext": "两人已离开公会，进入幽影森林调查失踪案。",
        "targetCharacters": 1500,
        "mustAdvance": goal,
        "mustNotDo": "不得回到无关日常琐事；不得重复穿越或初遇",
        "storyBeats": goal,
        "endingIntent": ending,
    }
    writer_req = urllib.request.Request(
        BASE + "/ai/generate-chapter",
        data=json.dumps(writer_request).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(writer_req, timeout=300) as resp:
        chapter = json.loads(resp.read().decode("utf-8"))
    generated.append(chapter)

repetitions = [
    index + 1 for index, chapter in enumerate(generated)
    if "面包" in str(chapter.get("content") or "")
]
evidence = {
    "extraction": out,
    "breadClassificationExact": classification_ok,
    "breadCandidatesReachingWriter": reaching,
    "subsequentChapters": generated,
    "breadRepetitionChapterIndexes": repetitions,
}
with open(os.path.join(out_dir, "ac105_extract_and_followups.json"), "w", encoding="utf-8") as f:
    json.dump(evidence, f, ensure_ascii=False, indent=2)

if classification_ok and not reaching and not repetitions:
    print("VERDICT: PASS — exact transient classification and 0/3 later repetitions")
    sys.exit(0)

print("classification_exact:", classification_ok)
print("later_bread_repetitions:", repetitions)
for b in reaching:
    print("VIOLATION:", json.dumps(b, ensure_ascii=False))
print("VERDICT: FAIL")
sys.exit(1)
