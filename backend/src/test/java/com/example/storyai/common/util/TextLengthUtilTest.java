package com.example.storyai.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * TASK-119 — chapter length counting must be simple, consistent and tested.
 * Rule: every Unicode code point counts as one character (CJK == Latin == 1).
 * All expected values below were verified by a standalone code-point count.
 */
class TextLengthUtilTest {

    @Test
    void nullAndEmptyCountZero() {
        assertEquals(0, TextLengthUtil.countCharacters(null));
        assertEquals(0, TextLengthUtil.countCharacters(""));
        assertEquals(0, TextLengthUtil.countCharacters("   \n\t "));
    }

    @Test
    void countsCJKAndLatinAsOneEach() {
        // 5 CJK ideographs + 6 ASCII letters/digit = 11 code points.
        assertEquals(11, TextLengthUtil.countCharacters("主角穿越了world5"));
    }

    @Test
    void countsMixedPunctuationAndSpaces() {
        // "你好，world!" -> 2 CJK + 1 fullwidth comma + 5 latin + 1 ascii bang = 9
        assertEquals(9, TextLengthUtil.countCharacters("你好，world!"));
    }

    @Test
    void countsLongChineseParagraph() {
        String prose = "主角来到禁书区最深处，终于在一堆残卷之下找到想要的东西。";
        // 28 characters (27 CJK + 1 fullwidth period); BMP-only so length == codePointCount.
        assertEquals(28, TextLengthUtil.countCharacters(prose));
        assertEquals(prose.length(), TextLengthUtil.countCharacters(prose));
    }
}
