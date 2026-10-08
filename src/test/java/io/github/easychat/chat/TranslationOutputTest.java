package io.github.easychat.chat;

import org.junit.Test;
import static org.junit.Assert.*;

public class TranslationOutputTest {
    @Test public void removesActualScreenshotSubtitleOverrides() {
        String style = "{\\fn黑体\\fs22\\bord1\\shad0\\3aHBE\\4aH00\\fscx67\\fscy66\\2cHFFFFFF\\3cH808080}";
        assertEquals("这... 我...", TranslationOutput.clean(style + "这... " + style + "我..."));
        assertEquals("你好 世界", TranslationOutput.clean("{\\i1}你好{\\i0}\\N世界"));
    }
    @Test public void suppressesRowsWithOnlyStylesWhitespaceOrPunctuation() {
        for (String text : new String[]{null, "", "{\\fn黑体}", "{\\fs22", "{\\i1}...", "§7\n\u200b"})
            assertFalse(TranslationOutput.hasContent(TranslationOutput.clean(text)));
    }
    @Test public void preservesNormalTextAndLiteralBraces() {
        assertEquals("大个子", TranslationOutput.clean("大个子"));
        assertEquals("哟", TranslationOutput.clean("哟"));
        assertEquals("拿 {red} 队的床", TranslationOutput.clean("拿 {red} 队的床"));
        assertTrue(TranslationOutput.hasContent("GG"));
        assertTrue(TranslationOutput.hasContent("2"));
    }
}
