package io.github.easychat.chat;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ChatLineUpdaterTest {
    @Test public void targetsExactZeroIdMessageAndDoesNotResurrectClearedMessages() {
        ChatLine older = new ChatLine(10, new ChatComponentText("Alex: hello"), 0);
        ChatLine newer = new ChatLine(20, new ChatComponentText("Bob: hello"), 0);
        List<ChatLine> lines = new ArrayList<>(Arrays.asList(newer, older));
        assertTrue(ChatLineUpdater.replace(lines, older, new ChatComponentText("Alex: hello\n  [译] 你好")));
        assertSame(newer, lines.get(0));
        assertEquals(10, lines.get(1).getUpdatedCounter());
        assertEquals(0, lines.get(1).getChatLineID());
        assertFalse(ChatLineUpdater.replace(lines, older, new ChatComponentText("duplicate")));
        lines.clear();
        assertFalse(ChatLineUpdater.replace(lines, newer, new ChatComponentText("removed")));
    }
    @Test public void replacesCopiedComponentWithoutMovingMessageOrChangingItsAge() {
        IChatComponent original = new ChatComponentText("Alex: hello\n正在翻译");
        List<ChatLine> lines = new ArrayList<>(Arrays.asList(
                new ChatLine(50, new ChatComponentText("newer"), 7),
                new ChatLine(40, original.createCopy(), 42),
                new ChatLine(30, new ChatComponentText("older"), 8)));
        IChatComponent completed = new ChatComponentText("Alex: hello\n你好");
        assertTrue(ChatLineUpdater.replace(lines, 42, completed));
        assertSame(completed, lines.get(1).getChatComponent());
        assertEquals(40, lines.get(1).getUpdatedCounter());
        assertEquals("newer", lines.get(0).getChatComponent().getUnformattedText());
        assertFalse(ChatLineUpdater.replace(lines, 100, completed));
        assertEquals(3, lines.size());
    }
}
