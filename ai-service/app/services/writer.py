"""Chapter writing service: /ai/generate-chapter.

TASK-121 length guard: compact models (e.g. qwen3-8b) tend to emit ~1000-char
vignettes even when a 3000-char target is requested. To make the ChapterSpec
target a real contract rather than a suggestion, we apply a bounded
expand-and-reconcile step: if the first draft falls below the floor, we send it
back ONCE with an explicit "expand to N chars, preserve every established fact"
instruction. This is deterministic engineering (no new infra, no MQ) and
directly addresses the frozen root cause "章节过短".
"""
from __future__ import annotations

from app.llm.provider import get_provider, parse_json_response
from app.prompts.builders import build_generate_prompt, log_request_shape
from app.schemas.models import GenerateChapterRequest, GenerateChapterResponse
from app.services.mock_builders import mock_generate

# Below this many characters we treat the draft as under-target and trigger one
# expand pass. 2250 is the AC-103 lower band; we guard at the same floor.
_LENGTH_FLOOR = 2250
_MAX_EXPAND_PASSES = 1


def _count_chars(text: str) -> int:
    # Mirrors backend TextLengthUtil: code points, CJK == Latin == 1.
    return len(text) if text else 0


def _expand_system() -> str:
    return (
        "你是一名小说扩写 AI。你会收到一章已写好的正文与其字数目标。你的任务是在"
        "【不改动任何已有事实、人物、对话与情节走向】的前提下，把它扩充到目标字数："
        "补充环境描写、人物心理、动作细节与必要的过渡，使节奏更饱满。禁止推翻或矛盾"
        "已有内容，禁止新增与本章目标无关的重大事件。只返回严格 JSON，格式为："
        '{"title": str, "content": str, "summary": str}'
    )


def _expand_user(title: str, content: str, target: int, summary: str) -> str:
    return f"""当前章节标题：{title}

当前正文字数：{_count_chars(content)} 字（目标是约 {target} 字，明显偏少，需要扩充）

当前正文：
{content}

当前摘要：
{summary}

请在保持以上所有事实、人物、对话、情节走向完全不变的前提下，将正文扩充到约 {target} 字：
- 为每个已有场景补充环境、心理、动作与过渡细节；
- 不得删除或改写已有句子所确立的事实；
- 不得引入与本章目标无关的新重大事件；
- 结尾保持原意，仅做必要的丰满。

请只输出 JSON。"""


def generate_chapter(req: GenerateChapterRequest) -> GenerateChapterResponse:
    log_request_shape("generate-chapter", req)
    provider = get_provider()
    if provider.is_mock:
        return mock_generate(req)

    system, user = build_generate_prompt(req)
    raw = provider.complete(user, system=system)
    resp = parse_json_response(GenerateChapterResponse, raw)

    target = req.targetCharacters
    # TASK-121: bounded expand-and-reconcile when the draft is under target.
    if target and _count_chars(resp.content) < _LENGTH_FLOOR:
        for _ in range(_MAX_EXPAND_PASSES):
            exp_raw = provider.complete(
                _expand_user(resp.title, resp.content, target, resp.summary),
                system=_expand_system(),
            )
            try:
                expanded = parse_json_response(GenerateChapterResponse, exp_raw)
            except ValueError:
                break  # keep the original draft if the expand pass is unparseable
            if _count_chars(expanded.content) > _count_chars(resp.content):
                resp = expanded
            if _count_chars(resp.content) >= _LENGTH_FLOOR:
                break
    return resp
