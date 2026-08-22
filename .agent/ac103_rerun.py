# -*- coding: utf-8 -*-
"""TASK-177 — re-run AC-103 under full frozen conditions.

target=3000 chars/chapter, 5 chapters, real backend -> real writer (qwen3-8b).
PASS requires >=4/5 chapters in [2250, 3750].
"""
import json
import time
import urllib.request

BASE = "http://127.0.0.1:8080"


def call(path, method="GET", body=None):
    data = json.dumps(body).encode("utf-8") if body else None
    req = urllib.request.Request(
        BASE + path, data=data,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method=method)
    with urllib.request.urlopen(req, timeout=300) as r:
        return json.loads(r.read().decode("utf-8"))


story = call("/api/stories", "POST", {
    "name": "AC103复验-长度控制",
    "coreIdea": "现代青年林夜穿越到剑与魔法世界，结识冒险者艾琳并被她收留。两人决定成为正式冒险者。",
    "defaultTargetCharacters": 3000,
})
sid = story["id"]
print("storyId =", sid)

stage = call(f"/api/stories/{sid}/stages", "POST", {
    "direction": "林夜和艾琳完成公会入会，接取幽影森林失踪案委托，"
                 "深入森林调查线索并遭遇第一次真正的危险，最终带着发现返回城镇。",
    "targetChapterCount": 5,
})
stage_id = stage["id"]
print("stageId =", stage_id, "plans =", len(stage.get("plans", [])))

call(f"/api/stages/{stage_id}/confirm", "POST")
job = call(f"/api/stages/{stage_id}/generate?mode=CONTINUOUS", "POST")
jid = job["id"]
print("job =", jid)

deadline = time.time() + 900
while time.time() < deadline:
    j = call(f"/api/generation-jobs/{jid}")
    if j["status"] in ("COMPLETED", "FAILED"):
        break
    time.sleep(5)
print("job:", j["status"], "err:", j.get("lastError"))

chapters = call(f"/api/stages/{stage_id}/chapters")
lengths = [(c["chapterNumber"], c.get("actualCharacterCount")) for c in chapters]
print("--- lengths ---")
for n, ln in sorted(lengths):
    in_band = ln is not None and 2250 <= ln <= 3750
    print(f"ch{n}: {ln} {'IN-BAND' if in_band else 'OUT'}")

in_count = sum(1 for _, ln in lengths if ln and 2250 <= ln <= 3750)
verdict = "PASS" if in_count >= 4 and len(lengths) == 5 else "FAIL"
print(f"in-band: {in_count}/5  VERDICT: {verdict}")

with open(".agent/evidence/ac103_rerun.json", "w", encoding="utf-8") as f:
    json.dump({"storyId": sid, "stageId": stage_id, "lengths": lengths,
               "inBand": in_count, "verdict": verdict}, f, ensure_ascii=False, indent=2)
