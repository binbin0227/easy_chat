package io.github.easychat.gui;

import io.github.easychat.EasyChat;
import io.github.easychat.audio.AudioCapture;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

final class TranslationTestScreen extends GuiScreen {
    private final EasyChat mod;
    private final GuiScreen parent;
    private AudioCapture.Session recording;
    private boolean stopping;
    private String error = "";
    TranslationTestScreen(EasyChat mod, GuiScreen parent) { this.mod = mod; this.parent = parent; }
    @Override public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 - 145, height - 48, 140, 20, "开始录音"));
        buttonList.add(new GuiButton(0, width / 2 + 5, height - 48, 140, 20, "返回"));
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 0) { mc.displayGuiScreen(parent); return; }
        if (recording == null) {
            error = "";
            recording = new AudioCapture().start(mod.settings.microphone, true);
            stopping = false;
        } else { recording.stop(); stopping = true; }
    }
    @Override public void updateScreen() {
        if (recording != null && recording.finished.isDone()) {
            try { mod.controller.translateTest(recording.finished.get()); }
            catch (Exception e) {
                Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                error = cause.getMessage() == null ? "录音失败" : cause.getMessage();
            }
            recording = null; stopping = false;
        }
        GuiButton b = buttonList.get(0);
        b.enabled = !mod.controller.busy() && !stopping;
        b.displayString = recording == null ? "开始录音" : stopping ? "正在结束录音" : "结束并翻译";
    }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else super.keyTyped(c, key);
    }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "本地翻译测试", width / 2, 18, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "显示中文识别和英文译文，不会发到服务器", width / 2, 38, 0xCCCCCC);
        String status = !error.isEmpty() ? error : recording != null ? "录音中 · 最长 15 秒" : mod.controller.status;
        fontRendererObj.drawSplitString(status, width / 2 - 145, 66, 290, 0x99DDFF);
        if (recording != null) {
            drawRect(width / 2 - 145, 96, width / 2 + 145, 108, 0xFF333333);
            drawRect(width / 2 - 144, 97, width / 2 - 144 + (int) (288 * Math.min(1, recording.level * 5)), 107, 0xFF55CC77);
        } else {
            fontRendererObj.drawSplitString("Paraformer 识别的中文：" + mod.controller.lastTranscript, width / 2 - 145, 96, 290, 0xDDDDDD);
            fontRendererObj.drawSplitString("译出的英文：" + mod.controller.lastTranslation, width / 2 - 145, 142, 290, 0xFFFFFF);
        }
        super.drawScreen(x, y, partial);
    }
    @Override public void onGuiClosed() {
        if (recording != null) { recording.stop(); recording = null; }
        if (mod.controller.busy()) mod.controller.cancel();
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
