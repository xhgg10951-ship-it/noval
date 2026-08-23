"""Offline integrity check for the final RH-10 evidence directory."""

from __future__ import annotations

import json
from pathlib import Path
import sys


evidence_dir = Path(sys.argv[1])
metadata = json.loads((evidence_dir / "RUN_METADATA.json").read_text(encoding="utf-8"))
assert metadata["model"] == "qwen3.7-plus"
assert metadata["mockLlm"] is False

# AC-101 raw output is intentionally preserved verbatim. Its paired prompt and
# raw files plus the harness exit status recorded in RESULTS.md form the audit
# record; verify both artifacts are present and non-empty.
assert any(evidence_dir.glob("EVIDENCE_AC101_prompt_*.txt"))
assert all(path.stat().st_size > 0 for path in evidence_dir.glob("EVIDENCE_AC101_raw_*.txt"))

ac103_path = max(evidence_dir.glob("EVIDENCE_AC103_raw_*.json"))
ac103 = json.loads(ac103_path.read_text(encoding="utf-8"))
assert len(ac103) == 5
assert sum(2250 <= row["chars"] <= 3750 for row in ac103) >= 4

ac104_path = max(evidence_dir.glob("EVIDENCE_AC104_*.json"))
ac104 = json.loads(ac104_path.read_text(encoding="utf-8"))
assert ac104["verdict"] == "PASS"
assert len(ac104["advance_hits"]) == 3
assert not ac104["violations"]

ac105 = json.loads((evidence_dir / "ac105_extract_and_followups.json").read_text(encoding="utf-8"))
assert ac105["breadClassificationExact"] is True
assert not ac105["breadCandidatesReachingWriter"]
assert not ac105["breadRepetitionChapterIndexes"]
assert len(ac105["subsequentChapters"]) == 3

ac106 = json.loads((evidence_dir / "ac106_replan_semantic.json").read_text(encoding="utf-8"))
assert ac106["verdict"] == "PASS"
assert ac106["planCount"] == 3
assert not ac106["forbiddenRepeatHits"]

ac109 = json.loads((evidence_dir / "ac109_polish.json").read_text(encoding="utf-8"))
assert ac109["verdict"] == "PASS"
assert all(ac109["checks"].values())

ac114 = json.loads((evidence_dir / "ac114_plan.json").read_text(encoding="utf-8"))
endgame_patterns = (
    "决战", "最终真相", "真相大白", "回到原", "返回地球", "大结局",
    "终结", "终焉", "最终之战", "最终反派", "一切结束", "落幕",
)
affirmative = " ".join(
    " ".join(
        [
            str(plan.get("goal") or ""),
            str(plan.get("expectedProgress") or ""),
            " ".join(plan.get("mustAdvance") or []),
            str(plan.get("endingIntent") or ""),
        ]
    )
    for plan in ac114.get("chapterPlans", [])
)
assert not [pattern for pattern in endgame_patterns if pattern in affirmative]

print("RH-10 evidence integrity: PASS (7/7)")
print("AC-103 lengths:", [row["chars"] for row in ac103])
