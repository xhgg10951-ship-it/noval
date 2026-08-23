from app.prompts.builders import build_generate_prompt
from app.schemas.models import CurrentArc, GenerateChapterRequest, LongFormPosition


def test_writer_prompt_renders_frozen_context_in_priority_order():
    request = GenerateChapterRequest(
        coreIdea="长篇成长故事",
        constraints=[{"type": "硬约束", "content": "不得离开王都"}],
        longFormPosition=LongFormPosition(
            targetChapterCount=600,
            currentChapterNumber=1,
        ),
        currentArc=CurrentArc(
            title="生存融入卷",
            goal="在王都站稳脚跟",
            targetStartChapter=1,
            targetEndChapter=60,
        ),
        stageDirection="先解决住处和身份",
        chapterGoal="办理临时身份登记",
        expectedProgress="完成身份登记并确定临时住处",
        chapterOrder=1,
        currentState=[],
        storyMemories=[],
        relationshipState=[],
        recentContext="上一章结尾仍在城门外",
        targetCharacters=3000,
    )

    _, prompt = build_generate_prompt(request)

    expected_in_order = [
        "硬性约束：",
        "长篇定位：",
        "当前卷：生存融入卷",
        "阶段导演指令：",
        "本章执行规格（ChapterSpec）：",
        "预期进度：完成身份登记并确定临时住处",
        "当前状态：",
        "故事记忆：",
        "近期上下文：",
    ]
    positions = [prompt.index(fragment) for fragment in expected_in_order]
    assert positions == sorted(positions)
    assert "全书目标约 600 章" in prompt
    assert "第 1–60 章" in prompt

