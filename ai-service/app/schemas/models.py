"""Pydantic schemas for the AI Service <-> Spring Boot contract.

These are the structured JSON shapes exchanged over HTTP. Java must never
regex-parse natural language; it validates these models instead.
"""
from __future__ import annotations

from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, Field


class SuggestedAction(str, Enum):
    AUTO = "AUTO"
    REVIEW = "REVIEW"
    IGNORE = "IGNORE"


class ConstraintItem(BaseModel):
    type: str
    content: str


class StateItem(BaseModel):
    category: str
    subject: str
    field: Optional[str] = None
    value: str


class MemoryItem(BaseModel):
    type: str
    subject: Optional[str] = None
    description: str


class RelationshipItem(BaseModel):
    subjectA: str
    subjectB: str
    description: str


# --------------------------------------------------------------------------
# Plan Stage
# --------------------------------------------------------------------------
class PlanStageRequest(BaseModel):
    coreIdea: str
    constraints: List[ConstraintItem] = Field(default_factory=list)
    stageDirection: str
    currentState: List[StateItem] = Field(default_factory=list)
    storyMemories: List[MemoryItem] = Field(default_factory=list)
    recentContext: str = ""
    targetChapterCount: Optional[int] = None


class ChapterPlanItem(BaseModel):
    order: int
    goal: str
    expectedProgress: str


class PlanStageResponse(BaseModel):
    suggestedChapterCount: int
    chapterPlans: List[ChapterPlanItem]


# --------------------------------------------------------------------------
# Generate Chapter
# --------------------------------------------------------------------------
class GenerateChapterRequest(BaseModel):
    coreIdea: str
    constraints: List[ConstraintItem] = Field(default_factory=list)
    stageDirection: str
    chapterGoal: str
    chapterOrder: int
    currentState: List[StateItem] = Field(default_factory=list)
    storyMemories: List[MemoryItem] = Field(default_factory=list)
    relationshipState: List[RelationshipItem] = Field(default_factory=list)
    recentContext: str = ""


class GenerateChapterResponse(BaseModel):
    title: str
    content: str
    summary: str


# --------------------------------------------------------------------------
# Extract Memory
# --------------------------------------------------------------------------
class ExtractMemoryRequest(BaseModel):
    chapterContent: str
    chapterSummary: str
    chapterOrder: int
    existingState: List[StateItem] = Field(default_factory=list)
    constraints: List[ConstraintItem] = Field(default_factory=list)


class MemoryCandidate(BaseModel):
    type: str
    subject: str
    field: Optional[str] = None
    value: str
    suggestedAction: SuggestedAction
    evidence: str


class ExtractMemoryResponse(BaseModel):
    candidates: List[MemoryCandidate]


# --------------------------------------------------------------------------
# Suggest Directions
# --------------------------------------------------------------------------
class SuggestDirectionsRequest(BaseModel):
    coreIdea: str
    constraints: List[ConstraintItem] = Field(default_factory=list)
    currentState: List[StateItem] = Field(default_factory=list)
    storyMemories: List[MemoryItem] = Field(default_factory=list)
    relationshipState: List[RelationshipItem] = Field(default_factory=list)
    recentContext: str = ""


class DirectionItem(BaseModel):
    title: str
    description: str


class SuggestDirectionsResponse(BaseModel):
    directions: List[DirectionItem]


# --------------------------------------------------------------------------
# Story Query
# --------------------------------------------------------------------------
class StoryQueryRequest(BaseModel):
    question: str
    currentState: List[StateItem] = Field(default_factory=list)
    storyMemories: List[MemoryItem] = Field(default_factory=list)
    relationshipState: List[RelationshipItem] = Field(default_factory=list)
    recentContext: str = ""
    sourceEvidence: List[MemoryItem] = Field(default_factory=list)


class StoryQueryResponse(BaseModel):
    answer: str
