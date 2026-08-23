"""Shared deterministic chapter-length bounds for prompt and expand guard."""


def length_bounds(target_characters: int) -> tuple[int, int]:
    """Return the frozen v0.1.1 acceptable range for one ChapterSpec target."""
    floor = max(300, round(target_characters * 0.75))
    ceiling = round(target_characters * 1.25)
    return floor, ceiling
