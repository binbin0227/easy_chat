package io.github.easychat.chat;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import java.util.List;
import java.util.ArrayList;

/** Replace a stored line by ID, even when a chat mod copied the component. */
public final class ChatLineUpdater {
    public static boolean replace(List<ChatLine> lines, int id, IChatComponent message) {
        for (int i = 0; i < lines.size(); i++) {
            ChatLine old = lines.get(i);
            if (old.getChatLineID() == id) {
                lines.set(i, new ChatLine(old.getUpdatedCounter(), message, id));
                return true;
            }
        }
        return false;
    }
    public static void update(GuiNewChat chat, int id, IChatComponent message) {
        List<ChatLine> lines = history(chat);
        if (!replace(lines, id, message)) return; // Removed/cleared messages must not reappear.
        refresh(chat, lines);
    }
    public static List<ChatLine> history(GuiNewChat chat) {
        return ReflectionHelper.getPrivateValue(GuiNewChat.class, chat, "chatLines", "field_146252_h");
    }
    public static boolean replace(List<ChatLine> lines, ChatLine original, IChatComponent message) {
        // Most received messages use ID zero. Matching that ID would overwrite an
        // unrelated message; track the full history entry instead of rendered wraps.
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i) == original) {
                lines.set(i, new ChatLine(original.getUpdatedCounter(), message, original.getChatLineID()));
                return true;
            }
        }
        return false;
    }
    public static boolean update(GuiNewChat chat, ChatLine original, IChatComponent message) {
        List<ChatLine> lines = history(chat);
        if (!replace(lines, original, message)) return false;
        refresh(chat, lines);
        return true;
    }
    private static void refresh(GuiNewChat chat, List<ChatLine> lines) {
        int scroll = ReflectionHelper.getPrivateValue(GuiNewChat.class, chat, "scrollPos", "field_146250_j");
        // 1.8.9 refreshChat calls deleteChatLine for nonzero IDs while rebuilding.
        // Restore the source history after it creates the rendered lines.
        List<ChatLine> snapshot = new ArrayList<>(lines);
        try { chat.refreshChat(); }
        finally { lines.clear(); lines.addAll(snapshot); }
        if (scroll > 0) chat.scroll(scroll);
    }
    private ChatLineUpdater() { }
}
