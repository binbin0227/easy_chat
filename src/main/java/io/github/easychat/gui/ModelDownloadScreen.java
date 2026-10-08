package io.github.easychat.gui;

import io.github.easychat.EasyChat;
import io.github.easychat.chat.TextModelCatalog;
import io.github.easychat.download.DownloadSources;
import io.github.easychat.engine.ModelStore;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/** Explicit source selection plus browser-download links for the exact supported file. */
final class ModelDownloadScreen extends GuiScreen {
    private final EasyChat mod;
    private final GuiScreen parent;
    private final ModelStore.Model voice;
    private final TextModelCatalog.Model text;
    private final List<DownloadSources.Link> links;
    private final List<DownloadSources.Source> choices = new ArrayList<>();
    private String message = "";
    ModelDownloadScreen(EasyChat mod, GuiScreen parent, ModelStore.Model voice, TextModelCatalog.Model text) {
        this.mod = mod; this.parent = parent; this.voice = voice; this.text = text;
        links = DownloadSources.links(voice == null ? null : voice.id, text == null ? null : text.repository, text == null ? null : text.revision, text == null ? null : text.file);
        for (DownloadSources.Link link : links) if (link.source != DownloadSources.Source.OFFICIAL) choices.add(link.source);
    }
    private DownloadSources.Source selected() { return voice != null ? mod.settings.voiceDownloadSource : mod.settings.textDownloadSource; }
    private DownloadSources.Link selectedLink() {
        for (DownloadSources.Link link : links) if (link.source == selected()) return link;
        return links.get(0);
    }
    private boolean busy() { return mod.controller.busy() || mod.textTranslation.busy; }
    private boolean ready() { return voice != null ? mod.controller.models.ready(voice) : mod.textTranslation.ready(text.id); }
    @Override public void initGui() {
        buttonList.clear(); int left = width / 2 - 155;
        buttonList.add(new GuiButton(100, left, 48, 150, 20, ""));
        buttonList.add(new GuiButton(101, left + 160, 48, 150, 20, ""));
        if (selected() != DownloadSources.Source.OFFICIAL && choices.size() > 1) buttonList.add(new GuiButton(102, left, 75, 310, 20, ""));
        buttonList.add(new GuiButton(2, left, height - 76, 150, 20, "复制下载链接"));
        buttonList.add(new GuiButton(3, left + 160, height - 76, 150, 20, "在浏览器打开"));
        buttonList.add(new GuiButton(1, left, height - 51, 310, 20, "开始下载"));
        buttonList.add(new GuiButton(0, left, height - 26, 310, 20, "返回"));
        updateScreen();
    }
    @Override public void updateScreen() {
        for (GuiButton button : buttonList) {
            button.enabled = button.id == 0 || !busy();
            if (button.id == 100) button.displayString = (selected() == DownloadSources.Source.OFFICIAL ? "[当前] " : "") + "官方源";
            if (button.id == 101) button.displayString = (selected() != DownloadSources.Source.OFFICIAL ? "[当前] " : "") + "备用源";
            if (button.id == 102) button.displayString = "备用来源：" + selected().label + " · 切换";
            if (button.id == 1) { button.enabled &= !ready(); button.displayString = ready() ? "模型已准备好" : voice != null && mod.controller.models.bundled(voice) ? "准备内置模型 · 无需下载" : "开始下载"; }
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { mc.displayGuiScreen(parent); return; }
        if (button.id >= 100) {
            DownloadSources.Source source = button.id == 100 ? DownloadSources.Source.OFFICIAL : button.id == 101 ? selected() == DownloadSources.Source.OFFICIAL ? choices.get(0) : selected() : choices.get((choices.indexOf(selected()) + 1) % choices.size());
            if (voice != null) mod.settings.voiceDownloadSource = source; else mod.settings.textDownloadSource = source;
            mod.saveSettings(); message = "下载源已保存"; initGui();
        } else if (button.id == 1) {
            if (voice != null) mod.controller.downloadModel(voice); else mod.textTranslation.prepare(text.id);
            mc.displayGuiScreen(parent);
        } else if (button.id == 2) {
            setClipboardString(selectedLink().url); message = "已复制 " + selectedLink().source.label + "链接，可下载后离线导入";
        } else if (button.id == 3) {
            try { java.awt.Desktop.getDesktop().browse(new URI(selectedLink().url)); message = "已打开浏览器，可下载后离线导入"; }
            catch (Exception unavailable) { setClipboardString(selectedLink().url); message = "浏览器无法打开，链接已复制"; }
        }
    }
    @Override protected void keyTyped(char c, int key) throws IOException { if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); else super.keyTyped(c, key); }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground(); int left = width / 2 - 155;
        drawCenteredString(fontRendererObj, "下载源 · " + (voice != null ? voice.label : text.name), width / 2, 14, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "选择官方或备用源；失败后由你切换", width / 2, 30, 0xBBBBBB);
        String status = message.isEmpty() ? "链接：" + selectedLink().source.label + " · 下载后可离线导入" : message;
        drawString(fontRendererObj, fontRendererObj.trimStringToWidth(status, 310), left, height - 91, 0x99DDFF);
        super.drawScreen(x, y, partial);
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
