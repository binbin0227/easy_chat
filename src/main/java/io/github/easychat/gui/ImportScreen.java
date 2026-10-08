package io.github.easychat.gui;

import io.github.easychat.EasyChat;
import io.github.easychat.engine.ModelStore;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.nio.file.Paths;

final class ImportScreen extends GuiScreen {
    private final EasyChat mod;
    private final GuiScreen parent;
    private GuiTextField path;
    private String error = "";
    private final ModelStore.Model model;
    ImportScreen(EasyChat mod, GuiScreen parent, ModelStore.Model model) { this.mod = mod; this.parent = parent; this.model = model; }
    @Override public void initGui() {
        String existing = path == null ? "" : path.getText();
        path = new GuiTextField(1, fontRendererObj, width / 2 - 145, height / 2 - 10, 290, 20);
        path.setMaxStringLength(2048); path.setText(existing); path.setFocused(true);
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 - 145, height / 2 + 25, 140, 20, "校验并导入"));
        buttonList.add(new GuiButton(0, width / 2 + 5, height / 2 + 25, 140, 20, "返回"));
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 0) { mc.displayGuiScreen(parent); return; }
        String value = path.getText().trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) value = value.substring(1, value.length() - 1);
        try {
            java.nio.file.Path source = Paths.get(value);
            if (!java.nio.file.Files.isRegularFile(source)) { error = "找不到文件，请粘贴模型文件的完整路径"; return; }
            mod.controller.importModel(model, source);
            mc.displayGuiScreen(parent);
        } catch (RuntimeException e) { error = "文件路径无效"; }
    }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else path.textboxKeyTyped(c, key);
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button); path.mouseClicked(x, y, button);
    }
    @Override public void updateScreen() { path.updateCursorCounter(); }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "导入 Paraformer model.int8.onnx", width / 2, height / 2 - 52, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "粘贴模型文件路径（Windows：右键文件 → 复制文件地址）", width / 2, height / 2 - 30, 0xCCCCCC);
        path.drawTextBox();
        drawCenteredString(fontRendererObj, error, width / 2, height / 2 + 55, 0xFF8888);
        super.drawScreen(x, y, partial);
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
