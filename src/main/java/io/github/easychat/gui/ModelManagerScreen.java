package io.github.easychat.gui;
import io.github.easychat.EasyChat;
import io.github.easychat.engine.ModelStore;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;

final class ModelManagerScreen extends GuiScreen implements GuiYesNoCallback {
    private final EasyChat mod;
    private final GuiScreen parent;
    private int left, top;
    private int page;
    ModelManagerScreen(EasyChat mod, GuiScreen parent) { this.mod = mod; this.parent = parent; }
    @Override public void initGui() {
        left = width / 2 - 145; top = 35;
        buttonList.clear();
        for (int i = page * 2; i < Math.min(page * 2 + 2, ModelStore.Model.values().length); i++) {
            int y = top + (i % 2) * 50 + 16;
            buttonList.add(new GuiButton(10 + i, left, y, 94, 20, ""));
            buttonList.add(new GuiButton(30 + i, left + 98, y, 94, 20, "离线导入"));
            buttonList.add(new GuiButton(20 + i, left + 196, y, 94, 20, "删除模型"));
        }
        buttonList.add(new GuiButton(4, left, height - 91, 94, 20, "上一页"));
        buttonList.add(new GuiButton(5, left + 196, height - 91, 94, 20, "下一页"));
        buttonList.add(new GuiButton(2, left, height - 52, 142, 20, "清除下载缓存"));
        buttonList.add(new GuiButton(3, left + 148, height - 52, 142, 20, "取消任务"));
        buttonList.add(new GuiButton(0, left, height - 27, 290, 20, "返回设置"));
        updateScreen();
    }
    @Override public void updateScreen() {
        for (GuiButton b : buttonList) {
            b.enabled = b.id == 0 || (b.id == 3 ? mod.controller.busy() : !mod.controller.busy());
            if (b.id == 4) b.enabled = page > 0;
            if (b.id == 5) b.enabled = page < (ModelStore.Model.values().length - 1) / 2;
            if (b.id >= 10 && b.id < 10 + ModelStore.Model.values().length) {
                ModelStore.Model model = ModelStore.Model.values()[b.id - 10];
                b.displayString = mod.controller.models.ready(model) ? "下载源 / 链接" : mod.controller.models.bundled(model) ? "准备内置模型" : "下载模型";
            }
            if (b.id >= 20 && b.id < 20 + ModelStore.Model.values().length) b.enabled &= mod.controller.models.exists(ModelStore.Model.values()[b.id - 20]);
        }
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 0) mc.displayGuiScreen(parent);
        else if (b.id == 2) mod.controller.clearDownloadCache();
        else if (b.id == 3) mod.controller.cancel();
        else if (b.id == 4 || b.id == 5) { page += b.id == 4 ? -1 : 1; initGui(); }
        else if (b.id >= 10 && b.id < 10 + ModelStore.Model.values().length) mc.displayGuiScreen(new ModelDownloadScreen(mod, this, ModelStore.Model.values()[b.id - 10], null));
        else if (b.id >= 20 && b.id < 20 + ModelStore.Model.values().length) {
            ModelStore.Model model = ModelStore.Model.values()[b.id - 20];
            mc.displayGuiScreen(new GuiYesNo(this, "删除" + model.label + "模型？", "将删除本地模型文件。需要时可重新准备或下载。", b.id - 20));
        } else if (b.id >= 30 && b.id < 30 + ModelStore.Model.values().length) mc.displayGuiScreen(new ImportScreen(mod, this, ModelStore.Model.values()[b.id - 30]));
    }
    @Override public void confirmClicked(boolean yes, int id) {
        if (yes) mod.controller.deleteModel(ModelStore.Model.values()[id]);
        mc.displayGuiScreen(this);
    }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); else super.keyTyped(c, key);
    }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "语音模型管理 · Paraformer", width / 2, top - 24, 0xFFFFFF);
        for (int i = page * 2; i < Math.min(page * 2 + 2, ModelStore.Model.values().length); i++) {
            ModelStore.Model model = ModelStore.Model.values()[i];
            String state = mod.controller.models.ready(model) ? "已准备" : mod.controller.models.exists(model) ? "未通过校验" : "未安装";
            drawString(fontRendererObj, model.label + " · " + model.mib + " MiB · " + state, left, top + (i % 2) * 50 + 2, 0xEEEEEE);
        }
        drawString(fontRendererObj, "清理保留有效模型；删除只移除本地副本。", left, top + 96, 0xCCCCCC);
        drawCenteredString(fontRendererObj, (page + 1) + " / " + ((ModelStore.Model.values().length + 1) / 2), width / 2, height - 85, 0xFFFFFF);
        drawString(fontRendererObj, fontRendererObj.trimStringToWidth(mod.controller.status, 290), left, height - 67, 0x99DDFF);
        super.drawScreen(x, y, partial);
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
