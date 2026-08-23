"""Deterministic regressions extracted from the product-wired RH-10 run."""
from __future__ import annotations

from app.api import router
from app.prompts.builders import build_extract_prompt
from app.schemas.models import ExtractMemoryRequest, PolishChapterRequest


def test_extract_prompt_requires_low_value_food_to_be_classified_not_omitted():
    request = ExtractMemoryRequest(
        chapterContent=(
            "林夜处理完伤口。早餐时，他吃掉一块普通面包，只是填饱肚子，"
            "随后不再关注它。"
        ),
        chapterSummary="林夜处理伤口并继续准备公会登记。",
        chapterOrder=2,
    )

    system, user = build_extract_prompt(request)
    prompt = system + user

    assert "低价值也不得省略" in prompt
    assert "一次性食物" in prompt
    assert "普通面包" in prompt
    assert "TRANSIENT_DETAIL / importance=1 / scope=CHAPTER / suggestedAction=IGNORE" in prompt


def test_polish_runs_a_fact_audit_repair_pass(monkeypatch):
    class FakeProvider:
        is_mock = False

        def __init__(self):
            self.prompts: list[str] = []

        def complete(self, prompt: str, *, system: str | None = None) -> str:
            self.prompts.append((system or "") + "\n" + prompt)
            if len(self.prompts) == 1:
                return '{"polishedContent":"林夜与艾琳在公会完成汇报，午夜去旧钟楼。"}'
            return (
                '{"polishedContent":"林夜与艾琳在冒险者公会大厅完成失踪案汇报；'
                '铁剑和调查委托书仍由林夜保留。线索指向幽影森林。'
                '午夜，两人去旧钟楼见证人。"}'
            )

    provider = FakeProvider()
    monkeypatch.setattr(router, "get_provider", lambda: provider)
    request = PolishChapterRequest(
        content=(
            "林夜和艾琳在冒险者公会完成失踪案线索汇报。林夜仍站在公会大厅。"
            "林夜保留铁剑和调查委托书。林夜确认失踪者最后出现在幽影森林。"
            "林夜听见艾琳提醒他午夜去旧钟楼见证人。"
        ),
        chapterGoal="完成汇报",
        endingIntent="午夜去旧钟楼见证人",
        userInstruction="降低机械重复但保持全部事实",
    )

    response = router.polish_chapter(request)

    assert len(provider.prompts) == 2
    assert "事实审计修复" in provider.prompts[1]
    assert request.content in provider.prompts[1]
    assert "铁剑和调查委托书" in response.polishedContent
    assert "冒险者公会大厅" in response.polishedContent
