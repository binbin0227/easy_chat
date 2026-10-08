package io.github.easychat.core;

import io.github.easychat.EasyChat;
import io.github.easychat.audio.AudioCapture;
import io.github.easychat.config.ChatSettings;
import io.github.easychat.engine.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class ChatController {
    private final EasyChat mod;
    public final ModelStore models;
    private final ParaformerEngine engine;
    private final AudioCapture capture = new AudioCapture();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "easychat-worker"); t.setDaemon(true); return t;
    });
    private final AtomicLong generation = new AtomicLong();
    public volatile String status = "请打开设置下载模型";
    public volatile String lastTranslation = "";
    public volatile String lastTranscript = "";
    public volatile long lastMillis;
    private volatile boolean busy;
    private volatile Future<?> job;
    private AudioCapture.Session recording;
    private World recordingWorld;
    private Object recordingConnection;
    private boolean keyWasDown, openRequested;
    private volatile boolean speechJob;
    private long lastKeySave;
    private volatile StatusHint hint;
    private StatusHint deliveredHint, chatHint;
    private static final int HINT_CHAT_ID = 0x454143;
    private void setStatus(String text) { setStatus(text, true); }
    private void setStatus(String text, boolean chat) {
        status = text;
        hint = new StatusHint(text, System.currentTimeMillis(), chat);
    }
    public void hintModeChanged() { setStatus("使用提示设置已更新"); }
    private void reconcileSelection() throws java.io.IOException {
        if (mod.settings.model.isEmpty() || !models.ready(ModelStore.Model.from(mod.settings.model))) {
            java.util.List<ModelStore.Model> available = models.available();
            mod.settings.model = available.isEmpty() ? "" : available.get(0).id;
            mod.settings.save();
        }
    }
    public void selectModel(ModelStore.Model model) {
        if (busy() || !models.ready(model)) return;
        mod.settings.model = model.id;
        engine.close();
        setStatus("识别模型已切换为" + model.label);
    }
    private void updateChatHint(Minecraft mc) {
        if (mc.thePlayer == null) { chatHint = null; return; }
        long now = System.currentTimeMillis();
        if (chatHint != null && (mod.settings.hintMode != ChatSettings.HintMode.CHAT || !chatHint.visible(now))) {
            mc.ingameGUI.getChatGUI().deleteChatLine(HINT_CHAT_ID);
            chatHint = null;
        }
        StatusHint latest = hint;
        if (latest == null || latest == deliveredHint) return;
        deliveredHint = latest;
        if (mod.settings.hintMode == ChatSettings.HintMode.CHAT && latest.chat && latest.visible(now)) {
            mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText("[Easy Chat] " + latest.text), HINT_CHAT_ID);
            chatHint = latest;
        }
    }

    public ChatController(EasyChat mod, Path root) {
        this.mod = mod;
        models = new ModelStore(root);
        engine = new ParaformerEngine(root, models);
        resourceTask(ModelStore.Model.PARAFORMER, (model, ticket) -> {
            mod.modelFiles.refresh();
            models.scan();
            mod.textTranslation.scan();
            if (models.autoPrepare(ModelStore.Model.PARAFORMER)) models.prepare(ModelStore.Model.PARAFORMER, s -> update(ticket, s));
        }, "模型检查完成");
    }
    public boolean busy() { return busy || recording != null; }
    public float level() { return recording == null ? 0 : recording.level; }
    public void requestSettings() { openRequested = true; }
    private boolean keyDown() {
        int key = mod.talkKey.getKeyCode();
        if (key < 0) { int button = key + 100; return button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button); }
        return key > 0 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(key);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        updateChatHint(mc);
        if ((mod.settings.talkKey != mod.talkKey.getKeyCode() || mod.settings.settingsKey != mod.settingsKey.getKeyCode())
                && System.currentTimeMillis() - lastKeySave > 5000) {
            lastKeySave = System.currentTimeMillis();
            try { mod.saveSettings(); } catch (java.io.IOException error) { setStatus("按键设置保存失败：" + error.getMessage()); }
        }
        if (openRequested || mod.settingsKey.isPressed()) {
            openRequested = false;
            if (recording != null || speechJob) cancel();
            mod.openSettings();
        }
        boolean down = keyDown();
        if ((recording != null || speechJob) && (mc.theWorld != recordingWorld || mc.getNetHandler() != recordingConnection)) cancel();
        if (mc.thePlayer == null || mc.currentScreen != null || !mc.inGameHasFocus) {
            if (recording != null) cancel();
            keyWasDown = down;
            return;
        }
        if (down && !keyWasDown && !busy()) startRecording();
        if (recording != null && (!down || recording.finished.isDone())) finishRecording();
        keyWasDown = down;
    }
    private void startRecording() {
        ModelStore.Model model = ModelStore.Model.from(mod.settings.model);
        if (!models.ready(model)) { setStatus("没有可用语音识别模型，请打开模型管理"); return; }
        Minecraft mc = Minecraft.getMinecraft();
        recordingWorld = mc.theWorld;
        recordingConnection = mc.getNetHandler();
        recording = capture.start(mod.settings.microphone, true);
        setStatus("录音中 · 松键发送 · 最长 " + AudioCapture.MAX_SECONDS + " 秒");
    }
    private void finishRecording() {
        AudioCapture.Session session = recording;
        recording = null;
        session.stop();
        final World world = recordingWorld;
        final Object connection = recordingConnection;
        final ChatSettings.SendMode mode = mod.settings.sendMode;
        final ModelStore.Model model = ModelStore.Model.from(mod.settings.model);
        final String textModel = mod.settings.speechTextModel;
        final long ticket = generation.incrementAndGet();
        busy = true; speechJob = true;
        setStatus("正在准备音频");
        job = worker.submit(() -> {
            try {
                AudioCapture.Clip clip = session.finished.get(10, TimeUnit.SECONDS);
                if (clip.seconds < 0.35 || clip.rms < 0.003) throw new IllegalArgumentException("录音太短或没有检测到声音，请重试");
                long started = System.nanoTime();
                engine.ensureStarted(model, s -> update(ticket, s));
                update(ticket, "正在翻译，请稍候");
                String transcript = engine.transcribe(clip.wav).trim();
                if (transcript.isEmpty()) throw new IllegalArgumentException("没有识别到中文，请重试");
                if (ticket != generation.get()) return;
                lastTranscript = transcript;
                update(ticket, "OPUS 正在把中文译成英文");
                String text = ChatText.normalize(mod.textTranslation.translate(transcript, true, textModel));
                ChatText.validate(text);
                long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Minecraft mc = Minecraft.getMinecraft();
                    if (ticket != generation.get()) return;
                    if (mc.theWorld != world || mc.getNetHandler() != connection || mc.thePlayer == null) {
                        busy = false; speechJob = false; setStatus("聊天环境已变化，未发送旧译文"); return;
                    }
                    lastTranslation = text; lastMillis = elapsed;
                    if (mode == ChatSettings.SendMode.AUTO) {
                        mc.thePlayer.sendChatMessage(text);
                        setStatus("已发送 · " + elapsed + " ms");
                    } else if (mc.currentScreen == null && mc.inGameHasFocus) {
                        mc.displayGuiScreen(new GuiChat(text));
                        setStatus("译文已填入聊天框");
                    } else {
                        mc.thePlayer.addChatMessage(new ChatComponentText("[Easy Chat] 译文：" + text));
                        setStatus("界面正在打开，译文已显示在本地聊天");
                    }
                    busy = false; speechJob = false;
                });
            } catch (Exception error) { fail(ticket, error); }
        });
    }
    private void update(long ticket, String text) { if (generation.get() == ticket) setStatus(text, false); }
    private void fail(long ticket, Exception error) {
        Throwable cause = error instanceof ExecutionException && error.getCause() != null ? error.getCause() : error;
        if (generation.get() == ticket) {
            setStatus(cause.getMessage() == null ? "操作失败，请重试或查看日志" : cause.getMessage());
            busy = false; speechJob = false;
        }
    }
    public void downloadModel(ModelStore.Model model) { resourceTask(model, (target, ticket) -> models.download(target, s -> update(ticket, s), mod.settings.voiceDownloadSource), model.label + "模型已准备好"); }
    public void importModel(ModelStore.Model model, Path path) { resourceTask(model, (target, ticket) -> models.importFile(target, path, s -> update(ticket, s)), model.label + "模型已导入"); }
    public void deleteModel(ModelStore.Model model) { resourceTask(model, (target, ticket) -> models.deleteModel(target), model.label + "模型已删除"); }
    public void clearDownloadCache() { resourceTask(ModelStore.Model.PARAFORMER, (target, ticket) -> {
        long bytes = models.clearDownloadCache() + mod.incoming.clearDownloadCache() + mod.modelFiles.clearTemporaryFiles();
        update(ticket, "清理完成，释放 " + bytes / (1024 * 1024) + " MiB");
    }, null); }
    private interface ResourceTask { void run(ModelStore.Model model, long ticket) throws Exception; }
    private void resourceTask(ModelStore.Model model, ResourceTask task, String success) {
        if (busy()) return;
        long ticket = generation.incrementAndGet();
        busy = true; speechJob = false;
        setStatus("正在准备模型");
        job = worker.submit(() -> {
            try {
                engine.close();
                try { task.run(model, ticket); } finally { mod.modelFiles.refresh(); }
                if (generation.get() == ticket) {
                    reconcileSelection();
                    if (success != null) setStatus(success);
                    busy = false;
                }
            } catch (Exception error) { fail(ticket, error); }
        });
    }
    public void modelChanged() {
        engine.close();
        try { reconcileSelection(); setStatus(models.available().isEmpty() ? "请在模型管理中准备模型" : "就绪"); }
        catch (java.io.IOException error) { setStatus("模型选择保存失败：" + error.getMessage()); }
    }
    public void translateTest(AudioCapture.Clip clip) {
        if (busy()) return;
        if (!models.ready(ModelStore.Model.from(mod.settings.model))) { setStatus("请先在模型管理中准备模型"); return; }
        long ticket = generation.incrementAndGet();
        ModelStore.Model model = ModelStore.Model.from(mod.settings.model);
        final String textModel = mod.settings.speechTextModel;
        busy = true; speechJob = false;
        lastTranslation = "";
        lastTranscript = "";
        job = worker.submit(() -> {
            try {
                if (clip.seconds < 0.35 || clip.rms < 0.003) throw new IllegalArgumentException("录音太短或没有检测到声音，请重试");
                long started = System.nanoTime();
                engine.ensureStarted(model, s -> update(ticket, s));
                update(ticket, "正在翻译测试录音，不会发送聊天");
                String transcript = engine.transcribe(clip.wav).trim();
                if (transcript.isEmpty()) throw new IllegalArgumentException("没有识别到内容，请重试");
                if (ticket != generation.get()) return;
                lastTranscript = transcript;
                String text = ChatText.normalize(mod.textTranslation.translate(transcript, true, textModel));
                if (text.isEmpty()) throw new IllegalArgumentException("没有识别到内容，请重试");
                if (generation.get() == ticket) {
                    lastTranslation = text;
                    lastMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    setStatus("测试完成 · " + lastMillis + " ms · 未发送聊天");
                    busy = false;
                }
            } catch (Exception error) { fail(ticket, error); }
        });
    }
    public void cancel() {
        generation.incrementAndGet();
        if (recording != null) { recording.stop(); recording = null; }
        Future<?> task = job; if (task != null) task.cancel(true);
        models.cancel();
        engine.close();
        mod.textTranslation.cancelInference();
        busy = false; speechJob = false;
        setStatus("已取消");
    }
    public void shutdown() { cancel(); worker.shutdownNow(); engine.shutdown(); }
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        StatusHint latest = hint;
        if (mc.thePlayer == null || mc.currentScreen != null || mod.settings.hintMode != ChatSettings.HintMode.TOP_LEFT
                || latest == null || !latest.visible(System.currentTimeMillis())) return;
        mc.fontRendererObj.drawStringWithShadow("Easy Chat: " + latest.text, 6, 6, 0xEEEEEE);
    }
}
