package io.github.easychat.chat;
import io.github.easychat.EasyChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class IncomingChatTranslator {
    private final EasyChat mod;
    private final Path logFile;
    private final ChatHistoryTracker historyTracker = new ChatHistoryTracker();
    private int rejectedSamples;
    private boolean historyFailure;
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16), r -> {
        Thread t = new Thread(r, "easychat-incoming-chat"); t.setDaemon(true); return t;
    });
    private volatile long generation;
    private Object world, connection;
    private volatile boolean enabled;
    private String selectedModel;
    private static final class Pending {
        final IChatComponent combined;
        final ChatLine original;
        final long ticket, expires = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        volatile boolean active = true;
        Pending(IChatComponent combined, ChatLine original, long ticket) {
            this.combined = combined; this.original = original; this.ticket = ticket;
        }
    }
    private static final class Completed {
        final Pending item; final String text;
        Completed(Pending item, String text) { this.item = item; this.text = text; }
    }
    private final ConcurrentLinkedQueue<Completed> completed = new ConcurrentLinkedQueue<>();
    private final List<Pending> pending = new ArrayList<>();
    private final Map<String, String> cache = new LinkedHashMap<String, String>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) { return size() > 128; }
    };
    public volatile String status = "收到玩家英文消息时自动翻译";
    public IncomingChatTranslator(EasyChat mod, Path root) { this.mod = mod; logFile = root.resolve("text-translation.log"); enabled = mod.settings.translateIncoming; }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        updateContext(mc);
        try {
            List<ChatLine> fresh = historyTracker.observe(new ArrayList<>(ChatLineUpdater.history(mc.ingameGUI.getChatGUI())));
            for (ChatLine line : fresh) received(mc, line);
        } catch (RuntimeException error) {
            if (!historyFailure) { log(error); historyFailure = true; }
            status = "无法读取聊天记录，请查看 easychat/text-translation.log";
        }
        Completed result;
        while ((result = completed.poll()) != null) {
            if (result.item.active && result.item.ticket == generation) {
                if (System.nanoTime() >= result.item.expires) timeout(mc, result.item);
                else finish(mc, result.item, result.text);
            }
        }
        for (Pending item : new ArrayList<>(pending)) {
            if (System.nanoTime() >= item.expires) {
                timeout(mc, item);
            }
        }
    }
    private void timeout(Minecraft mc, Pending item) {
        finish(mc, item, null);
        status = "聊天翻译超时，请查看 easychat/text-translation.log";
        log(new TimeoutException("Incoming translation exceeded 30 seconds; stage=" + mod.textTranslation.phase()));
    }
    private void finish(Minecraft mc, Pending item, String text) {
        item.active = false; pending.remove(item);
        text = TranslationOutput.clean(text);
        if (!TranslationOutput.hasContent(text)) return; // No empty/punctuation-only translation rows.
        item.combined.appendSibling(new ChatComponentText("\n  [译] " + text).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY)));
        try {
            boolean updated = ChatLineUpdater.update(mc.ingameGUI.getChatGUI(), item.original, item.combined);
            trace(updated ? "delivered" : "discarded: original left chat history");
        }
        catch (RuntimeException error) {
            log(error);
        }
    }
    private synchronized void trace(String message) {
        try {
            Files.createDirectories(logFile.getParent());
            if (Files.exists(logFile) && Files.size(logFile) > 512 * 1024) Files.delete(logFile);
            Files.write(logFile, (new java.util.Date() + " " + message + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) { }
    }
    private synchronized void log(Throwable error) {
        try {
            Files.createDirectories(logFile.getParent());
            if (Files.exists(logFile) && Files.size(logFile) > 512 * 1024) Files.delete(logFile);
            String text = new java.util.Date() + " " + error.getClass().getName() + ": " + error.getMessage() + "\n";
            for (StackTraceElement frame : error.getStackTrace()) text += "  at " + frame + "\n";
            Files.write(logFile, text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) { }
    }
    private void updateContext(Minecraft mc) {
        if (world != mc.theWorld || connection != mc.getNetHandler() || enabled != mod.settings.translateIncoming || !Objects.equals(selectedModel, mod.settings.incomingTextModel)) {
            for (Pending item : new ArrayList<>(pending)) {
                if (world == mc.theWorld && mc.thePlayer != null) finish(mc, item, null);
                else item.active = false;
            }
            pending.clear();
            completed.clear();
            world = mc.theWorld; connection = mc.getNetHandler(); enabled = mod.settings.translateIncoming;
            selectedModel = mod.settings.incomingTextModel;
            generation++;
            rejectedSamples = 0; historyFailure = false;
            trace("context enabled=" + enabled + "; model=" + selectedModel + "; recognition=player-format");
            worker.getQueue().clear();
        }
    }
    private void received(Minecraft mc, ChatLine line) {
        if (!mod.settings.translateIncoming || mc.thePlayer == null || mc.getNetHandler() == null) return;
        IChatComponent componentMessage = line.getChatComponent();
        PlayerChatParser.Message message = null;
        if (componentMessage instanceof ChatComponentTranslation) {
            ChatComponentTranslation component = (ChatComponentTranslation) componentMessage;
            Object[] args = component.getFormatArgs();
            if ("chat.type.text".equals(component.getKey()) && args.length == 2)
                message = PlayerChatParser.eligible(plain(args[0]), plain(args[1]));
        }
        // Hypixel / client localization may use ChatComponentTranslation with a
        // non-vanilla key. Recognize its displayed player format as a fallback.
        if (message == null) {
            Set<String> players = new HashSet<>();
            for (NetworkPlayerInfo player : mc.getNetHandler().getPlayerInfoMap()) players.add(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
            message = PlayerChatParser.parse(componentMessage.getFormattedText(), players);
        }
        if (message == null) {
            String plainText = componentMessage.getUnformattedText();
            int colon = plainText.indexOf(": ");
            if (colon > 0 && colon < 120 && rejectedSamples++ < 20) {
                // Prefix only: diagnose formats without saving players' message bodies.
                StringBuilder codes = new StringBuilder();
                for (char c : plainText.substring(0, colon).toCharArray()) codes.append(String.format("%04x ", (int)c));
                trace("unrecognized prefix codepoints=" + codes);
            }
            return;
        }
        if (message.sender.equalsIgnoreCase(mc.thePlayer.getName())) return;
        final String source = message.text;
        final String textModel = mod.settings.incomingTextModel;
        final long ticket = generation;
        final IChatComponent original = componentMessage.createCopy();
        final IChatComponent combined = new ChatComponentText("").appendSibling(original);
        final Pending item = new Pending(combined, line, ticket);
        try {
            worker.execute(() -> {
                if (ticket != generation || !item.active || System.nanoTime() >= item.expires) return;
                String result;
                try {
                    status = "正在本地翻译玩家聊天";
                    String cacheKey = textModel + ":" + source;
                    result = cache.get(cacheKey);
                    if (result == null) {
                        result = TranslationOutput.clean(mod.textTranslation.translate(source, false, textModel));
                        if (!TranslationOutput.hasContent(result) || result.length() > 500) throw new IllegalArgumentException("译文无有效内容或过长");
                        cache.put(cacheKey, result);
                    }
                    status = "玩家聊天翻译完成";
                } catch (Exception | LinkageError | OutOfMemoryError error) {
                    status = "聊天翻译失败，可在设置中关闭或重试";
                    System.err.println("[Easy Chat] Text translation failed: " + error.getClass().getSimpleName());
                    log(error);
                    result = null;
                }
                if (ticket == generation && item.active) completed.add(new Completed(item, result));
            });
            pending.add(item);
            trace("queued sender=" + message.sender + "; characters=" + source.length());
        } catch (RejectedExecutionException full) { status = "聊天过于频繁，暂时保留部分原文"; }
    }
    private static String plain(Object value) { return value instanceof IChatComponent ? ((IChatComponent) value).getUnformattedText() : String.valueOf(value); }
    public long clearDownloadCache() throws java.io.IOException { return mod.textTranslation.clearDownloadCache(); }
    public void shutdown() {
        generation++; worker.getQueue().clear();
        worker.shutdownNow();
    }
}
