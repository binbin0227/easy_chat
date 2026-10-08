package io.github.easychat.gui;
import io.github.easychat.EasyChat;
import io.github.easychat.engine.ModelStore;
import io.github.easychat.chat.TextTranslationService;
import io.github.easychat.chat.TextModelCatalog;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.*;

/** Explicit list of validated, available models only. */
final class ModelSelectionScreen extends GuiScreen {
    enum Use { VOICE, SPEECH_TEXT, INCOMING_TEXT }
    private final EasyChat mod; private final GuiScreen parent; private final Use use;
    private final List<ModelStore.Model> voice = new ArrayList<>(); private final List<String> text = new ArrayList<>();
    ModelSelectionScreen(EasyChat mod, GuiScreen parent, Use use) { this.mod = mod; this.parent = parent; this.use = use; }
    @Override public void initGui() {
        buttonList.clear(); voice.clear(); text.clear(); int left = width / 2 - 155;
        if (use == Use.VOICE) {
            voice.addAll(mod.controller.models.available());
            for (int i = 0; i < voice.size(); i++) { ModelStore.Model model = voice.get(i); buttonList.add(new GuiButton(100+i,left,55+i*24,310,20,(model.id.equals(mod.settings.model)?"[当前] ":"")+model.label+" · "+model.mib+" MiB")); }
        } else {
            boolean speech = use == Use.SPEECH_TEXT;
            for (TextModelCatalog.Model model : TextModelCatalog.all()) if (model.supports(speech) && mod.textTranslation.ready(model.id)) text.add(model.id);
            TextModelCatalog.Model current = TextModelCatalog.resolve(speech ? mod.settings.speechTextModel : mod.settings.incomingTextModel, speech);
            String selected = current == null ? "" : current.id;
            for (int i=0;i<text.size();i++) buttonList.add(new GuiButton(100+i,left,55+i*24,310,20,(text.get(i).equals(selected)?"[当前] ":"")+TextTranslationService.label(text.get(i))));
        }
        buttonList.add(new GuiButton(0,left,height-27,310,20,"返回"));
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 100 && !mod.controller.busy() && !mod.textTranslation.busy) {
            if(use==Use.VOICE) mod.controller.selectModel(voice.get(button.id-100)); else mod.textTranslation.select(text.get(button.id-100),use==Use.SPEECH_TEXT);
            mod.saveSettings();
        }
        mc.displayGuiScreen(parent);
    }
    @Override protected void keyTyped(char c,int key) throws IOException { if(key==Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); else super.keyTyped(c,key); }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground(); drawCenteredString(fontRendererObj,use==Use.VOICE?"选择语音模型":use==Use.SPEECH_TEXT?"选择中文 → 英文模型":"选择英文 → 中文模型",width/2,18,0xFFFFFF);
        drawCenteredString(fontRendererObj,"只列已准备的模型，下载请前往模型管理",width/2,36,0xBBBBBB);super.drawScreen(x,y,partial);
    }
    @Override public boolean doesGuiPauseGame(){return false;}
}
