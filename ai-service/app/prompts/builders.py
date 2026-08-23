"""Prompt construction for each AI capability.

Each builder returns a (system, user) prompt tuple. The system prompt pins the
role and the strict JSON output contract; the user prompt serializes the
structured inputs so the LLM has everything it needs to produce a valid output.
"""
from __future__ import annotations

from typing import Tuple

from app.schemas.models import (
    ExtractMemoryRequest,
    GenerateChapterRequest,
    PlanStageRequest,
    StoryQueryRequest,
    SuggestDirectionsRequest,
    SummarizeChapterRequest,
)
from app.services.length_policy import length_bounds

import logging

_request_logger = logging.getLogger("ai.requests")


def log_request_shape(name: str, req) -> None:
    """DEBUG-only observability (TASK-103).

    Logs which context fields are present vs empty so wiring gaps
    (e.g. empty currentState / storyMemories) are visible without a
    debugger. Request DTOs carry no API keys, so no secret is logged.
    """
    if not _request_logger.isEnabledFor(logging.DEBUG):
        return

    def _len(v):
        if v is None:
            return 0
        if isinstance(v, (list, str)):
            return len(v)
        return 1

    shape = {
        "coreIdea": _len(getattr(req, "coreIdea", None)),
        "constraints": _len(getattr(req, "constraints", None)),
        "stageDirection": _len(getattr(req, "stageDirection", None)),
        "chapterGoal": _len(getattr(req, "chapterGoal", None)),
        "currentState": _len(getattr(req, "currentState", None)),
        "storyMemories": _len(getattr(req, "storyMemories", None)),
        "relationshipState": _len(getattr(req, "relationshipState", None)),
        "recentContext": _len(getattr(req, "recentContext", None)),
        "targetChapterCount": getattr(req, "targetChapterCount", None),
        "continuationAnchor": _len(getattr(req, "continuationAnchor", None)),
        "completedStageSummaries": _len(getattr(req, "completedStageSummaries", None)),
        "recentChapterSummaries": _len(getattr(req, "recentChapterSummaries", None)),
        "currentChapterNumber": getattr(req, "currentChapterNumber", None),
        "longFormPosition": _len(getattr(getattr(req, "longFormPosition", None), "model_dump", lambda: None)() or {}),
    }
    _request_logger.debug("[%s] received request shape: %s", name, shape)


def _fmt_constraints(items) -> str:
    return "\n".join(f"- [{i.type}] {i.content}" for i in items) or "(无)"


def _fmt_state(items) -> str:
    return (
        "\n".join(
            f"- [{i.category}] {i.subject}{('/' + i.field) if i.field else ''}: {i.value}"
            for i in items
        )
        or "(无)"
    )


def _fmt_memories(items) -> str:
    return "\n".join(f"- [{i.type}] {i.subject or ''}: {i.description}" for i in items) or "(无)"


def _fmt_relationships(items) -> str:
    return "\n".join(f"- {i.subjectA} <-> {i.subjectB}: {i.description}" for i in items) or "(无)"


PLAN_OUTPUT_HINT = (
    '{"suggestedChapterCount": int, '
    '"chapterPlans": [{"order": int, "goal": str, "expectedProgress": str, '
    '"targetCharacters": int, "mustAdvance": [str], "mustNotDo": [str], '
    '"storyBeats": [str], "endingIntent": str}]}'
)


def build_plan_prompt(req: PlanStageRequest) -> Tuple[str, str]:
    system = (
        "你是一名小说分章策划 AI。根据作者的'阶段导演指令'、故事约束、当前状态与记忆，"
        "输出一个分阶段章节计划。必须只返回严格 JSON，格式为：\n" + PLAN_OUTPUT_HINT
    )
    # TASK-109: continuation framing. If the story already has chapters, the
    # Planner is CONTINUING an existing story and must not restart it.
    anchor_block = _fmt_continuation(req)
    user = f"""核心创意：
{req.coreIdea}

阶段导演指令：
{req.stageDirection}

约束：
{_fmt_constraints(req.constraints)}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

{anchor_block}

{_fmt_long_form_position(req)}

近期上下文：
{req.recentContext or '(无)'}

已完成阶段概要：
{_fmt_lines(req.completedStageSummaries)}

近期章节摘要：
{_fmt_lines(req.recentChapterSummaries)}

目标章节数：{req.targetChapterCount or '由你决定'}

每一章计划都必须包含完整的 ChapterSpec：
- targetCharacters：本章目标字数（建议 2500–3500，长篇请接近上限）
- mustAdvance：本章必须推进的要点列表
- mustNotDo：本章禁止做的事列表（例如不得重复已完成阶段、不得重复开场）
- storyBeats：本章关键剧情节拍列表
- endingIntent：本章结尾意图（为下一章留接口）

请只输出 JSON。"""
    return system, user


def _fmt_lines(items) -> str:
    if not items:
        return "(无)"
    return "\n".join(f"- {line}" for line in items)


def _fmt_continuation(req: PlanStageRequest) -> str:
    """TASK-107/108/109: render the continuation context block.

    When the story has prior chapters, pins the 'you are continuing' framing
    and surfaces the structured anchor so the Planner does not repeat
    already-resolved plot (crossing / first meeting / already-owned identity).
    """
    if req.currentChapterNumber is None:
        return "续写状态：这是故事的第一阶段，没有前情，可以自由开篇。"
    a = req.continuationAnchor
    parts = [
        "续写状态：这是一部正在连载的故事，你必须坚持已有剧情，不得重新开篇。",
        f"当前最新章节编号：第 {req.currentChapterNumber} 章",
    ]
    if a is not None:
        if a.lastChapterSummary:
            parts.append(f"上一章摘要：{a.lastChapterSummary}")
        if a.lastChapterEnding:
            parts.append(f"上一章结尾（节选）：{a.lastChapterEnding}")
        if a.currentLocation:
            parts.append(f"当前地点：{a.currentLocation}")
        if a.activeCharacters:
            parts.append("当前活跃人物：" + "、".join(a.activeCharacters))
        if a.currentImmediateGoal:
            parts.append(f"当前直接目标：{a.currentImmediateGoal}")
    parts.append(
        "禁止：再次描写穿越、初次遇见已经认识的人物、重新获得已经拥有的身份或住所、"
        "重复已经完成过的阶段。新计划必须从上述续写状态继续。"
    )
    return "\n".join(parts)


def _fmt_long_form_position(req: PlanStageRequest) -> str:
    """TASK-154/155: render the long-form position block + pace guard rules.

    The guard is expressed as PROPORTIONAL RULES over the position numbers, never
    as a hardcoded plot-word blacklist: what counts as "endgame content" is
    defined structurally (belongs to the final ~10% of a long-form book) so it
    generalises to any story.
    """
    pos = req.longFormPosition
    if pos is None:
        return ""
    lines = ["长篇定位："]
    if pos.targetChapterCount is not None:
        lines.append(f"- 本书目标总章数：约 {pos.targetChapterCount} 章")
    if pos.currentChapterNumber is not None:
        lines.append(f"- 当前进度：第 {pos.currentChapterNumber} 章")
    if pos.arcTitle is not None or pos.arcStartChapter is not None:
        rng = ""
        if pos.arcStartChapter is not None and pos.arcEndChapter is not None:
            rng = f"（第 {pos.arcStartChapter}–{pos.arcEndChapter} 章）"
        title = pos.arcTitle or "(未命名卷)"
        lines.append(f"- 当前卷：《{title}》{rng}")
        if pos.arcGoal:
            lines.append(f"- 当前卷目标：{pos.arcGoal}")

    target = pos.targetChapterCount
    current = pos.currentChapterNumber
    if target and current and target > 0:
        remaining_ratio = max(0.0, (target - current) / target)
        if remaining_ratio > 0.10:
            lines += [
                "",
                "长篇节奏守则（必须遵守）：",
                f"- 当前仅完成全书的约 {round((current / target) * 100)}%，剩余约 "
                f"{round(remaining_ratio * 100)}%。你规划的只是接下来一个阶段的局部剧情。",
                "- 终局性内容（全书核心真相的完整揭示、与最终敌人的决胜之战、主角回到原点、"
                "全书主矛盾的彻底解决等收束性剧情）只允许出现在全书最后 10% 的章节里。",
                "- 本阶段的每一章都必须服务于「当前卷」的目标，用新的事件、人物或阻力"
                "小步推进，而不是向结局冲刺。",
                "- 禁止在本阶段计划中出现任何终局性、收束性或总结性的章节计划。",
            ]
    return "\n".join(lines)


GENERATE_OUTPUT_HINT = '{"title": str, "content": str, "summary": str}'


# ---- v0.1.1 Phase 8 (TASK-167/168): polish prompt with fact preservation ----
POLISH_OUTPUT_HINT = '{"polishedContent": str}'


# ---- RH-01 / HB-001: current-body-only summary refresh ----
SUMMARY_OUTPUT_HINT = '{"summary": str}'


def build_summary_prompt(req: SummarizeChapterRequest) -> Tuple[str, str]:
    """Summarize only the supplied current body; never reuse stale context."""
    system = (
        "你是一名小说章节摘要 AI。只根据本次提供的【当前正文】生成简洁摘要，"
        "不得沿用旧摘要，不得补充正文中没有的事实。摘要应优先记录：本章已经完成的"
        "事件、人物或物品状态变化、尚未完成的动作，以及结尾所在位置或悬念。"
        "普通装饰细节不要强化为主线。必须只返回严格 JSON，格式为：\n"
        + SUMMARY_OUTPUT_HINT
    )
    user = f"""当前正文（唯一事实来源）：
{req.content}

请生成 1–3 句摘要，只输出 JSON。"""
    return system, user


def build_polish_prompt(req) -> Tuple[str, str]:
    """TASK-168 — polish the prose WITHOUT changing what happened.

    Allowed: sentence flow, dialogue texture, scene detail, removing repetition
    and mechanical summary tone. Forbidden: new plot elements, changed core
    events, changed character/state facts, a different ending.
    """
    system = (
        "你是一名小说文字润色 AI。你的任务是在【完全不改变事实】的前提下提升文笔。\n"
        "\n"
        "允许：\n"
        "- 调整句式与节奏，让叙述更自然流畅；\n"
        "- 优化对话的语气与个性化表达；\n"
        "- 充实场景感官细节（在已有设定范围内）；\n"
        "- 删除重复表述、机械总结式的段落收尾。\n"
        "禁止：\n"
        "- 增加任何新设定、新人物、新物品、新事件；\n"
        "- 改变核心事件及其因果顺序；\n"
        "- 改变人物的状态、持有物或关系事实；\n"
        "- 改变章节结局的走向与意图。\n"
        "必须只返回严格 JSON，格式为：\n" + POLISH_OUTPUT_HINT
    )
    style = req.writingStyle or "(未指定)"
    instruction = req.userInstruction or "(无)"
    state_block = _fmt_state(req.currentState)
    constraint_block = _fmt_constraints(req.constraints)
    ending = req.endingIntent or "(未指定)"
    user = f"""写作风格要求：
{style}

润色指令（作者）：
{instruction}

本章目标（事实基准）：
{req.chapterGoal}

结尾意图（必须保持）：
{ending}

约束：
{constraint_block}

当前状态（事实基准——润色后这些事实必须原样成立）：
{state_block}

原文：
{req.content}

请只输出 JSON。"""
    return system, user


def build_generate_prompt(req: GenerateChapterRequest) -> Tuple[str, str]:
    system = (
        "你是一名小说章节写作 AI。根据阶段导演指令、本章目标与上下文，写出一章连贯的叙事正文，"
        "并附标题与摘要。必须只返回严格 JSON，格式为：\n" + GENERATE_OUTPUT_HINT
    )
    # TASK-118: Goal Lock — enforce priority order and Must-Not compliance.
    spec_block = _fmt_writer_spec(req)
    style_block = getattr(req, "writingStyle", None) or "(未指定)"
    user = f"""核心创意：
{req.coreIdea}

阶段导演指令：
{req.stageDirection}

写作风格（作者要求）：
{style_block}

{spec_block}

本章目标（第 {req.chapterOrder} 章）：
{req.chapterGoal}

约束：
{_fmt_constraints(req.constraints)}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

近期上下文：
{req.recentContext or '(无)'}

【执行优先级，必须自上而下遵守】
1. 硬性约束（约束列表）高于一切；
2. 本章目标（ChapterSpec）是本章必须执行的主线；
3. 必须推进（mustAdvance）必须发生；禁止事项（mustNotDo）绝不可违反；
4. 故事记忆用于丰富细节，不得用它替换本章主线；
5. 与本章无关的旧细节不要重复铺陈；
6. 本章结尾意图（endingIntent）应被满足，为下一章留接口。

请只输出 JSON。"""
    return system, user


def _fmt_writer_spec(req: GenerateChapterRequest) -> str:
    """TASK-117/118: render the full ChapterSpec the Writer must execute.

    TASK-121 length hardening: the target character count is a HARD contract.
    qwen3-8b (and similar compact models) tend to write ~1000-char vignettes
    unless explicitly pushed, so we state the floor/ceiling, forbid under-length
    output, and give concrete structural guidance (expand each beat into a
    paragraph-level scene with dialogue, action, and interiority).
    """
    lines = ["本章执行规格（ChapterSpec）："]
    if req.targetCharacters is not None:
        lo, hi = length_bounds(req.targetCharacters)
        lines.append(
            f"- 目标字数：必须达到约 {req.targetCharacters} 字（硬性要求，可接受范围 "
            f"{lo}–{hi} 字）。严禁明显偏短：若正文不足 {lo} 字，视为未完成本章，必须补充 "
            f"场景细节、对话与描写直到达标。"
        )
        lines.append(
            "  达成方法：把每个剧情节拍展开为完整段落——包含环境描写、人物对话、动作与"
            "心理活动；不要只用一两句话带过任何一个节拍。"
        )
    if req.mustAdvance:
        lines.append(f"- 必须推进：{req.mustAdvance}")
    if req.mustNotDo:
        lines.append(f"- 禁止事项：{req.mustNotDo}（绝不可违反）")
    if req.storyBeats:
        lines.append(f"- 剧情节拍（每个都要充分展开）：{req.storyBeats}")
    if req.endingIntent:
        lines.append(f"- 结尾意图：{req.endingIntent}")
    if len(lines) == 1:
        lines.append("- （无显式规格，按本章目标自由发挥）")
    return "\n".join(lines)


EXTRACT_OUTPUT_HINT = (
    '{"candidates": [{"type": str, "subject": str, "field": str|null, '
    '"value": str, "suggestedAction": "AUTO"|"REVIEW"|"IGNORE", "evidence": str, '
    '"importance": int, "scope": str}]}'
)


def build_extract_prompt(req: ExtractMemoryRequest) -> Tuple[str, str]:
    """TASK-160 — Memory v2 extractor prompt.

    Core discipline: EXTRACT, DON'T INVENT. Every candidate must be grounded in
    the chapter text; the five questions decide type/importance/scope/action.
    """
    system = (
        "你是一名故事记忆抽取 AI。你的纪律是：只抽取，不发明（Extract, don't invent）——"
        "每个候选都必须能在章节正文中找到证据原文，禁止推测、脑补或总结出正文没有的事实。\n"
        "\n"
        "对每个候选依次回答五个问题：\n"
        "1. 这是当前状态吗？（位置/伤势/情绪/当前目标 → CURRENT_STATE；"
        "两人关系变化 → RELATIONSHIP。有长期剧情意义的持有物 → CURRENT_STATE 且 field 写为 "
        "item:物品名。注意：一次性食物/消耗品/纯过场物件不属于状态，除非它对后续剧情有明确持续影响）\n"
        "2. 五章之后作者还需要它吗？不需要 → TRANSIENT_DETAIL（如一次性道具、"
        "过场对话细节），建议 IGNORE 或低重要度\n"
        "3. 它是剧情资产还是瞬时细节？推动主线/埋设伏笔/世界规则 → "
        "PLOT_FACT / PLOT_THREAD / FORESHADOWING / WORLD_RULE\n"
        "4. 重要度 importance 1–5：5=必须始终让写作者知道的核心事实；"
        "1=可有可无的细节\n"
        "5. 范围 scope：CHAPTER=仅本章有意义 / STAGE=本阶段内有效 / "
        "ARC=本卷内有效 / STORY=全书长期有效\n"
        "\n"
        "type 只允许：CURRENT_STATE, RELATIONSHIP, PLOT_FACT, PLOT_THREAD, "
        "FORESHADOWING, WORLD_RULE, TRANSIENT_DETAIL。"
        "suggestedAction 建议：核心事实与状态→AUTO；伏笔与世界规则→REVIEW；"
        "低价值细节→IGNORE。\n"
        "每个候选给出 subject、可选 field、value、evidence 证据原文。"
        "必须只返回严格 JSON，格式为：\n" + EXTRACT_OUTPUT_HINT
    )
    user = f"""章节摘要：
{req.chapterSummary}

章节正文：
{req.chapterContent}

已有状态：
{_fmt_state(req.existingState)}

约束：
{_fmt_constraints(req.constraints)}

注意：每个 candidate 都必须包含 importance（1–5 整数）和 scope（CHAPTER/STAGE/ARC/STORY 之一）字段，缺一不可。

请只输出 JSON。"""
    return system, user


SUGGEST_OUTPUT_HINT = '{"directions": [{"title": str, "description": str}]}'


def build_suggest_prompt(req: SuggestDirectionsRequest) -> Tuple[str, str]:
    system = (
        "你是一名创作辅助 AI。基于当前故事状态与约束，给出若干条值得作者考虑的下一步创作方向，"
        "例如冲突型、成长型、悬疑型。必须只返回严格 JSON，格式为：\n" + SUGGEST_OUTPUT_HINT
    )
    user = f"""核心创意：
{req.coreIdea}

约束：
{_fmt_constraints(req.constraints)}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

近期上下文：
{req.recentContext or '(无)'}

请只输出 JSON。"""
    return system, user


QUERY_OUTPUT_HINT = '{"answer": str}'


def build_query_prompt(req: StoryQueryRequest) -> Tuple[str, str]:
    system = (
        "你是一名故事问答 AI。基于当前状态、故事记忆与人物关系，回答作者关于'故事现在是什么情况'的问题。"
        "若信息不足，明确说明'当前故事中没有确定这一信息（未知）'。必须只返回严格 JSON，格式为：\n"
        + QUERY_OUTPUT_HINT
    )
    user = f"""问题：
{req.question}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

近期上下文：
{req.recentContext or '(无)'}

请只输出 JSON。"""
    return system, user
