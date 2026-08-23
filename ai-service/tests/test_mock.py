"""Smoke tests for the AI Service in MOCK mode (no LLM_API_KEY required).

Run with:  pytest tests/  (from ai-service/, with deps installed)
"""
from __future__ import annotations

import os

# Ensure mock mode regardless of ambient env.
os.environ.pop("LLM_API_KEY", None)
os.environ.pop("API_KEY", None)

from fastapi.testclient import TestClient  # noqa: E402

from app.main import app  # noqa: E402

client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert body["version"] == "0.1.1"
    assert body["mock_llm"] is True


def test_plan_stage():
    r = client.post(
        "/ai/plan-stage",
        json={
            "coreIdea": "穿越者异世界求生",
            "stageDirection": "主角初到异世界，在小镇站稳脚跟",
            "targetChapterCount": 3,
        },
    )
    assert r.status_code == 200
    data = r.json()
    assert data["suggestedChapterCount"] == 3
    assert len(data["chapterPlans"]) == 3
    assert data["chapterPlans"][0]["order"] == 1


def test_replan_stage():
    r = client.post(
        "/ai/replan-stage",
        json={
            "coreIdea": "穿越者异世界求生",
            "stageDirection": "调整后的阶段目标：主角加入公会",
            "targetChapterCount": 4,
        },
    )
    assert r.status_code == 200
    assert r.json()["suggestedChapterCount"] == 4


def test_generate_chapter():
    r = client.post(
        "/ai/generate-chapter",
        json={
            "coreIdea": "穿越者异世界求生",
            "stageDirection": "主角初到异世界",
            "chapterGoal": "在酒馆获得第一个线索",
            "chapterOrder": 1,
        },
    )
    assert r.status_code == 200
    data = r.json()
    assert data["title"] and data["content"] and data["summary"]


def test_summarize_chapter_uses_current_body():
    r = client.post(
        "/ai/summarize-chapter",
        json={"content": "主角检查空仓库后，空手离开。"},
    )
    assert r.status_code == 200
    summary = r.json()["summary"]
    assert summary
    assert "空手离开" in summary
    assert "铁剑" not in summary


def test_extract_memory():
    r = client.post(
        "/ai/extract-memory",
        json={
            "chapterContent": "主角来到风息镇，获得了一把铁剑。艾琳在一旁观察，仍存戒备。",
            "chapterSummary": "主角抵达风息镇并获铁剑。",
            "chapterOrder": 1,
        },
    )
    assert r.status_code == 200
    data = r.json()
    assert isinstance(data["candidates"], list)
    assert len(data["candidates"]) >= 1


def test_suggest_directions():
    r = client.post(
        "/ai/suggest-directions",
        json={"coreIdea": "穿越者异世界求生"},
    )
    assert r.status_code == 200
    data = r.json()
    assert len(data["directions"]) == 3


def test_story_query():
    r = client.post(
        "/ai/story-query",
        json={
            "question": "主角现在在哪里？",
            "currentState": [
                {"category": "CURRENT_STATE", "subject": "主角", "field": "location", "value": "风息镇"}
            ],
        },
    )
    assert r.status_code == 200
    assert "风息镇" in r.json()["answer"]
