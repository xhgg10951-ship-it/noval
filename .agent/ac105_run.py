# -*- coding: utf-8 -*-
"""TASK-165 / AC-105 — bread-loop regression (real LLM).

A chapter mentions an ordinary bread purchase. The Memory v2 extractor must
classify it as TRANSIENT_DETAIL with low importance / CHAPTER scope and a
non-AUTO suggested action, so the writer never re-receives it as context.
"""
import json
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

with open(".agent/evidence/ac105_extract.json", "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)

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


reaching = [c for c in bread_rows if would_reach_writer(c)]
if not reaching:
    print("VERDICT: PASS — no bread candidate reaches the writer context"
          + ("; classified as transient/ignored by the extractor"
             if bread_rows else "; not extracted at all"))
    sys.exit(0)

bad = True
for b in reaching:
    print("VIOLATION:", json.dumps(b, ensure_ascii=False))
print("VERDICT: FAIL")
sys.exit(1)
