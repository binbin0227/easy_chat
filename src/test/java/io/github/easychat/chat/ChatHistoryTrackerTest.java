package io.github.easychat.chat;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.ChatComponentText;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ChatHistoryTrackerTest {
    @Test public void observesMessagesOnceInArrivalOrderIncludingSameTextAndZeroIds() {
        ChatHistoryTracker tracker = new ChatHistoryTracker();
        ChatLine first = new ChatLine(10, new ChatComponentText("Alex: hello"), 0);
        ChatLine second = new ChatLine(11, new ChatComponentText("Alex: hello"), 0);
        assertEquals(Arrays.asList(first, second), tracker.observe(Arrays.asList(second, first)));
        assertTrue(tracker.observe(Arrays.asList(second, first)).isEmpty());
        ChatLine third = new ChatLine(12, new ChatComponentText("Bob: hi"), 0);
        assertEquals(Collections.singletonList(third), tracker.observe(Arrays.asList(third, second, first)));
        assertTrue(tracker.observe(Collections.emptyList()).isEmpty());
        ChatLine afterClear = new ChatLine(13, new ChatComponentText("Alex: hello"), 0);
        assertEquals(Collections.singletonList(afterClear), tracker.observe(Collections.singletonList(afterClear)));
    }
}
