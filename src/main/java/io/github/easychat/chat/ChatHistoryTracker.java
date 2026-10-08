package io.github.easychat.chat;

import net.minecraft.client.gui.ChatLine;
import java.util.*;

/** Observe full messages after the client has applied its chat modifications. */
final class ChatHistoryTracker {
    private Set<ChatLine> seen = Collections.newSetFromMap(new IdentityHashMap<ChatLine, Boolean>());
    List<ChatLine> observe(List<ChatLine> newestFirst) {
        List<ChatLine> fresh = new ArrayList<>();
        Set<ChatLine> current = Collections.newSetFromMap(new IdentityHashMap<ChatLine, Boolean>());
        for (ChatLine line : newestFirst) {
            current.add(line);
            if (!seen.contains(line)) fresh.add(line);
        }
        seen = current; // Bounded by Minecraft's chat history, including after clear.
        Collections.reverse(fresh);
        return fresh;
    }
}
