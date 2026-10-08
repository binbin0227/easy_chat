package io.github.easychat.gui;

import io.github.easychat.EasyChat;
import io.github.easychat.audio.AudioCapture;
import io.github.easychat.config.ChatSettings;
import io.github.easychat.chat.TextTranslationService;
import io.github.easychat.engine.ModelStore;
import net.minecraft.client.gui.*;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import java.io.IOException;

/** Four task-oriented pages. Model selection and resource management are separate. */
public class SettingsTabsScreen extends GuiScreen {
    private final EasyChat mod;
    private int tab;
    private AudioCapture.Session test;
    private int binding;
    private String message = "";
    private int left;
    public SettingsTabsScreen(EasyChat mod, int tab) { this.mod = mod; this.tab = tab; }
    public int selectedTab() { return tab; }
    private void row(int id, int row, String label) { buttonList.add(new GuiButton(id, left + 105, 60 + row * 20, 205, 18, label)); }
    @Override public void initGui() {
        left = width / 2 - 155; buttonList.clear();
        String[] tabs = {"语音发送", "接收翻译", "模型管理", "通用设置"};
        for (int i = 0; i < tabs.length; i++) buttonList.add(new GuiButton(100 + i, left + i * 79, 30, 73, 20, tabs[i]));
        if (tab == 0) {
            row(1, 0, deviceName());
            buttonList.add(new GuiButton(2, left + 105, 80, 95, 18, "音量测试"));
            row(3, 2, ""); row(4, 3, ""); row(5, 4, ""); row(13, 5, "");
            buttonList.add(new GuiButton(9, left, height - 26, 150, 20, "语音测试"));
        } else if (tab == 1) { row(201, 0, ""); row(202, 1, ""); }
        else if (tab == 2) {
            buttonList.add(new GuiButton(6, left, 70, 310, 20, "语音模型管理"));
            buttonList.add(new GuiButton(11, left, 115, 310, 20, "文字模型管理"));
            buttonList.add(new GuiButton(203, left, 160, 150, 20, "清除下载缓存"));
            buttonList.add(new GuiButton(204, left + 160, 160, 150, 20, "旧模型 / 未识别文件"));
        } else {
            row(10, 0, ""); row(14, 1, "");
            buttonList.add(new GuiButton(8, left, height - 26, 150, 20, "恢复默认设置"));
        }
        buttonList.add(new GuiButton(0, left + (tab == 0 || tab == 3 ? 160 : 0), height - 26, tab == 0 || tab == 3 ? 150 : 310, 20, "完成"));
        updateScreen();
    }
    private String trim(String text, int pixels) { return fontRendererObj.trimStringToWidth(text, pixels); }
    private String deviceName() {
        for (AudioCapture.Device device : AudioCapture.devices()) if (device.id.equals(mod.settings.microphone)) return trim(device.name, 195);
        return "麦克风已断开，请选择";
    }
    @Override public void updateScreen() {
        boolean busy = mod.controller.busy() || mod.textTranslation.busy;
        for (GuiButton button : buttonList) {
            button.enabled = button.id == 0 || button.id >= 100 && button.id <= 103 || !busy && binding == 0;
            if (button.id >= 100 && button.id <= 103) { button.enabled = button.id != 100 + tab; continue; }
            if (button.id == 3) { button.enabled = !busy && (binding == 0 || binding == 1); button.displayString = binding == 1 ? "请按新按键（Esc 取消）" : net.minecraft.client.settings.GameSettings.getKeyDisplayString(mod.talkKey.getKeyCode()) + " · 修改"; }
            if (button.id == 14) { button.enabled = !busy && (binding == 0 || binding == 2); button.displayString = binding == 2 ? "请按新按键（Esc 取消）" : net.minecraft.client.settings.GameSettings.getKeyDisplayString(mod.settingsKey.getKeyCode()) + " · 修改"; }
            if (button.id == 4) button.displayString = mod.settings.sendMode == ChatSettings.SendMode.AUTO ? "松键后自动发送" : "填入聊天框，确认发送";
            if (button.id == 5) { ModelStore.Model model = ModelStore.Model.from(mod.settings.model); button.displayString = mod.controller.models.ready(model) ? model.label : "无可用模型 · 请先准备"; }
            if (button.id == 13) button.displayString = TextTranslationService.label(mod.settings.speechTextModel) + " · 选择";
            if (button.id == 201) button.displayString = mod.settings.translateIncoming ? "开启" : "关闭";
            if (button.id == 202) button.displayString = TextTranslationService.label(mod.settings.incomingTextModel) + " · 选择";
            if (button.id == 10) button.displayString = mod.settings.hintMode == ChatSettings.HintMode.TOP_LEFT ? "左上角 · 5 秒后隐藏" : mod.settings.hintMode == ChatSettings.HintMode.CHAT ? "聊天框 · 仅自己可见" : "不显示提示";
            if (button.id == 2) button.displayString = test == null ? "音量测试" : "停止测试";
            if (button.id == 9) { button.enabled = binding == 0; button.displayString = mod.controller.busy() ? "取消语音任务" : "语音测试"; }
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 100 && button.id <= 103) { stopTest(); binding = 0; tab = button.id - 100; message = ""; initGui(); return; }
        switch (button.id) {
            case 0: binding = 0; if (save()) mc.displayGuiScreen(null); break;
            case 1: stopTest(); mc.displayGuiScreen(new MicrophoneScreen(mod, this)); break;
            case 2: if (test != null) stopTest(); else { message = "音量测试不会识别、翻译或发送"; test = new AudioCapture().start(mod.settings.microphone, false); } break;
            case 3: stopTest(); binding = binding == 1 ? 0 : 1; break;
            case 14: binding = binding == 2 ? 0 : 2; break;
            case 4: mod.settings.sendMode = mod.settings.sendMode == ChatSettings.SendMode.AUTO ? ChatSettings.SendMode.DRAFT : ChatSettings.SendMode.AUTO; break;
            case 5: stopTest(); mc.displayGuiScreen(new ModelSelectionScreen(mod, this, ModelSelectionScreen.Use.VOICE)); break;
            case 13: mc.displayGuiScreen(new ModelSelectionScreen(mod, this, ModelSelectionScreen.Use.SPEECH_TEXT)); break;
            case 202: mc.displayGuiScreen(new ModelSelectionScreen(mod, this, ModelSelectionScreen.Use.INCOMING_TEXT)); break;
            case 201: mod.settings.translateIncoming = !mod.settings.translateIncoming; break;
            case 6: mc.displayGuiScreen(new ModelManagerScreen(mod, this)); break;
            case 11: mc.displayGuiScreen(new TextModelManagerScreen(mod, this)); break;
            case 203: mod.controller.clearDownloadCache(); break;
            case 204: mc.displayGuiScreen(new ModelFilesScreen(mod, this)); break;
            case 8: mc.displayGuiScreen(new GuiYesNo((confirmed, id) -> {
                if (confirmed) { mod.resetSettings(); mod.controller.modelChanged();  }
                mc.displayGuiScreen(this);
            }, "恢复所有默认设置？", "已下载的模型不会删除。", 8)); break;
            case 9: stopTest(); if (mod.controller.busy()) mod.controller.cancel(); else mc.displayGuiScreen(new TranslationTestScreen(mod, this)); break;
            case 10: ChatSettings.HintMode[] modes = ChatSettings.HintMode.values(); mod.settings.hintMode = modes[(mod.settings.hintMode.ordinal() + 1) % modes.length]; mod.controller.hintModeChanged(); break;
        }
        save(); updateScreen();
    }
    private void stopTest() { if (test != null) { test.stop(); test = null; } }
    private boolean save() { try { mod.saveSettings(); return true; } catch (IOException error) { message = "保存失败：" + error.getMessage(); return false; } }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (binding != 0) { if (key != Keyboard.KEY_ESCAPE) bindKey(key); binding = 0; updateScreen(); }
        else if (key == Keyboard.KEY_ESCAPE) { if (save()) mc.displayGuiScreen(null); }
        else super.keyTyped(c, key);
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        if (binding != 0) { bindKey(button - 100); binding = 0; updateScreen(); } else super.mouseClicked(x, y, button);
    }
    private void bindKey(int key) {
        KeyBinding target = binding == 2 ? mod.settingsKey : mod.talkKey;
        target.setKeyCode(key); KeyBinding.resetKeyBindingArrayAndHash(); message = "";
        for (KeyBinding other : mc.gameSettings.keyBindings) if (other != target && other.getKeyCode() == key) { message = "提醒：该按键也被其他操作使用"; break; }
        save();
    }
    private void labels(String... labels) { for (int i = 0; i < labels.length; i++) drawString(fontRendererObj, labels[i], left, 65 + i * 20, 0xEEEEEE); }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground(); drawCenteredString(fontRendererObj, "Easy Chat 设置 · 0.1.0", width / 2, 12, 0xFFFFFF);
        if (tab == 0) {
            labels("麦克风", "音量测试", "说话按键", "发送方式", "中文识别模型", "中文 → 英文");
            drawString(fontRendererObj, "Paraformer 识别中文 → OPUS 翻译英文", left, 190, 0xBBBBBB);
            drawRect(left + 210, 84, left + 309, 96, 0xFF333333);
            if (test != null) { drawRect(left + 211, 85, left + 211 + (int)(97 * Math.min(1, test.level * 5)), 95, 0xFF55CC77); message = test.finished.isCompletedExceptionally() ? test.status : test.level >= .003 ? "检测到声音" : "未检测到声音"; }
        } else if (tab == 1) {
            labels("翻译玩家消息", "英文 → 中文模型");
            fontRendererObj.drawSplitString("英文立即显示，译完在原消息下一行补中文。仅自己可见，服务器通知和自己的消息保持原样。", left, 108, 310, 0xBBBBBB);
            fontRendererObj.drawSplitString("OPUS 英译中已内置，无需配置 API。", left, 144, 310, 0xBBBBBB);
        } else if (tab == 2) {
            drawString(fontRendererObj, "Paraformer：中文语音识别，CPU 推理", left, 99, 0xBBBBBB);
            drawString(fontRendererObj, "OPUS：中译英 / 英译中，两个模型均内置", left, 144, 0xBBBBBB);
        } else {
            labels("使用提示位置", "打开设置按键");
            drawString(fontRendererObj, trim(mod.textTranslation.backendStatus(), 310), left, 127, 0x99DDFF);
        }
        String status = !message.isEmpty() ? message : tab == 1 ? mod.incoming.status : tab == 2 && mod.textTranslation.busy ? mod.textTranslation.status : mod.controller.status;
        drawString(fontRendererObj, trim(status, 310), left, height - 42, 0x99DDFF);
        super.drawScreen(x, y, partial);
    }
    @Override public void onGuiClosed() { stopTest(); binding = 0; save(); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
