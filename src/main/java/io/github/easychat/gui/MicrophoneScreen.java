package io.github.easychat.gui;

import io.github.easychat.EasyChat;
import io.github.easychat.audio.AudioCapture;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.*;

final class MicrophoneScreen extends GuiScreen {
    private final EasyChat mod;
    private final GuiScreen parent;
    private List<AudioCapture.Device> devices;
    private int page, perPage;
    MicrophoneScreen(EasyChat mod, GuiScreen parent) { this.mod = mod; this.parent = parent; }
    @Override public void initGui() {
        devices = AudioCapture.devices();
        perPage = Math.max(1, Math.min(7, (height - 100) / 24));
        page = Math.min(page, (devices.size() - 1) / perPage);
        buttonList.clear();
        int start = page * perPage;
        for (int i = start; i < Math.min(devices.size(), start + perPage); i++) {
            AudioCapture.Device d = devices.get(i);
            String name = (d.id.equals(mod.settings.microphone) ? "[已选择] " : "") + d.name;
            buttonList.add(new GuiButton(100 + i, width / 2 - 145, 45 + (i - start) * 24, 290, 20, fontRendererObj.trimStringToWidth(name, 280)));
        }
        buttonList.add(new GuiButton(1, width / 2 - 145, height - 48, 65, 20, "上一页"));
        buttonList.add(new GuiButton(2, width / 2 - 75, height - 48, 65, 20, "下一页"));
        buttonList.add(new GuiButton(3, width / 2 - 5, height - 48, 70, 20, "刷新设备"));
        buttonList.add(new GuiButton(0, width / 2 + 70, height - 48, 75, 20, "返回"));
        buttonList.get(buttonList.size() - 4).enabled = page > 0;
        buttonList.get(buttonList.size() - 3).enabled = (page + 1) * perPage < devices.size();
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 100) { mod.settings.microphone = devices.get(button.id - 100).id; mc.displayGuiScreen(parent); }
        else if (button.id == 0) mc.displayGuiScreen(parent);
        else { if (button.id == 1) page--; if (button.id == 2) page++; initGui(); }
    }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else super.keyTyped(c, key);
    }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "选择麦克风", width / 2, 16, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "第 " + (page + 1) + " 页 · 选择后可返回测试音量", width / 2, height - 22, 0xCCCCCC);
        super.drawScreen(x, y, partial);
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
