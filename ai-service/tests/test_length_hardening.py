import json

import pytest

from app.prompts.builders import build_generate_prompt
from app.schemas.models import GenerateChapterRequest
from app.services import writer


class SequencedProvider:
    is_mock = False

    def __init__(self, lengths: list[int]):
        self.lengths = list(lengths)
        self.calls = 0

    def complete(self, prompt: str, *, system: str | None = None) -> str:
        self.calls += 1
        length = self.lengths.pop(0)
        return json.dumps({
            "title": "动态长度",
            "content": "字" * length,
            "summary": "摘要",
        }, ensure_ascii=False)


def request_for(target: int) -> GenerateChapterRequest:
    return GenerateChapterRequest(
        coreIdea="动态长度测试",
        stageDirection="推进主线",
        chapterGoal="完成本章目标",
        chapterOrder=1,
        targetCharacters=target,
    )


@pytest.mark.parametrize(
    ("target", "lengths", "expected_calls"),
    [
        (1500, [1200, 1300, 1400, 1500], 1),
        (3000, [2200, 2300], 2),
        (5000, [3000, 3800], 2),
    ],
)
def test_expand_guard_uses_target_specific_floor(
    monkeypatch: pytest.MonkeyPatch,
    target: int,
    lengths: list[int],
    expected_calls: int,
) -> None:
    provider = SequencedProvider(lengths)
    monkeypatch.setattr(writer, "get_provider", lambda: provider)

    writer.generate_chapter(request_for(target))

    assert provider.calls == expected_calls


@pytest.mark.parametrize(
    ("target", "floor", "ceiling"),
    [(1500, 1125, 1875), (3000, 2250, 3750), (5000, 3750, 6250)],
)
def test_writer_prompt_uses_same_dynamic_bounds(
    target: int, floor: int, ceiling: int
) -> None:
    _, prompt = build_generate_prompt(request_for(target))

    assert f"{floor}–{ceiling} 字" in prompt
    assert f"不足 {floor} 字" in prompt
