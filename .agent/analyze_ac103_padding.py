"""Deterministic duplicate-prose check for an AC-103 raw evidence file."""

from __future__ import annotations

import difflib
import itertools
import json
import re
import sys


rows = json.load(open(sys.argv[1], encoding="utf-8"))
all_sentences: list[tuple[int, list[str]]] = []
duplicate_total = 0
max_internal = 0.0

for row in rows:
    sentences = [
        re.sub(r"\s+", "", part)
        for part in re.split(r"[。！？!?\n]+", row["content"])
        if len(re.sub(r"\s+", "", part)) >= 20
    ]
    duplicates = len(sentences) - len(set(sentences))
    duplicate_total += duplicates
    similarities = [
        difflib.SequenceMatcher(None, left, right).ratio()
        for left, right in itertools.combinations(sentences, 2)
    ]
    peak = max(similarities, default=0.0)
    max_internal = max(max_internal, peak)
    print(
        f"ch{row['order']}: chars={row['chars']} sentences={len(sentences)} "
        f"exact_duplicate_sentences={duplicates} max_sentence_similarity={peak:.3f}"
    )
    all_sentences.append((row["order"], sentences))

max_cross = 0.0
cross_pair: tuple[int, int] | None = None
for (left_order, left), (right_order, right) in itertools.combinations(all_sentences, 2):
    for left_sentence in left:
        for right_sentence in right:
            similarity = difflib.SequenceMatcher(None, left_sentence, right_sentence).ratio()
            if similarity > max_cross:
                max_cross = similarity
                cross_pair = (left_order, right_order)

passed = duplicate_total == 0 and max_internal < 0.9 and max_cross < 0.9
print(f"exact_duplicate_sentences_total={duplicate_total}")
print(f"max_cross_chapter_sentence_similarity={max_cross:.3f} chapters={cross_pair}")
print(f"padding_review={'PASS' if passed else 'REVIEW'}")
sys.exit(0 if passed else 1)
