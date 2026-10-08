package io.github.easychat.gui;

import io.github.easychat.EasyChat;
import io.github.easychat.engine.ModelFileRegistry;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.*;

/** Older supported files and unknown files remain visible even after catalog changes. */
final class ModelFilesScreen extends GuiScreen {
    private final EasyChat mod;
    private final GuiScreen parent;
    private List<ModelFileRegistry.Group> groups=new ArrayList<>();
    private int page;
    private String status="";
    ModelFilesScreen(EasyChat mod,GuiScreen parent) { this.mod=mod; this.parent=parent; }
    private boolean busy() { return mod.controller.busy() || mod.textTranslation.busy; }
    @Override public void initGui() {
        buttonList.clear(); int left=width/2-155;
        if (!busy()) try { groups=mod.modelFiles.leftovers(); status=groups.isEmpty()?"没有旧版模型或未识别文件":"仅列旧版与未识别文件；当前模型在原管理页"; }
        catch (IOException error) { groups=new ArrayList<>(); status=error.getMessage(); }
        page=Math.min(page,Math.max(0,(groups.size()-1)/2));
        for (int i=page*2;i<Math.min(page*2+2,groups.size());i++) buttonList.add(new GuiButton(100+i,left+230,50+(i%2)*45,80,20,"删除"));
        buttonList.add(new GuiButton(4,left,height-76,95,20,"上一页"));
        buttonList.add(new GuiButton(5,left+215,height-76,95,20,"下一页"));
        buttonList.add(new GuiButton(1,left,height-51,310,20,"刷新文件列表"));
        buttonList.add(new GuiButton(0,left,height-26,310,20,"返回"));
        updateScreen();
    }
    @Override public void updateScreen() {
        for (GuiButton button:buttonList) {
            button.enabled=button.id==0 || !busy();
            if (button.id==4) button.enabled &= page>0;
            if (button.id==5) button.enabled &= (page+1)*2<groups.size();
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id==0) { mc.displayGuiScreen(parent); return; }
        if (busy()) return;
        if (button.id==1) initGui();
        else if (button.id==4 || button.id==5) { page+=button.id==4?-1:1; initGui(); }
        else if (button.id>=100) {
            ModelFileRegistry.Group group=groups.get(button.id-100);
            mc.displayGuiScreen(new GuiYesNo((confirmed,id)-> {
                String result="";
                if (confirmed && !busy()) try { mod.modelFiles.deleteLeftover(group.key); result="本地文件已删除"; }
                catch (IOException error) { result="未完成："+error.getMessage(); }
                mc.displayGuiScreen(this); if (!result.isEmpty()) status=result;
            },group.unknown?"删除这个未识别文件？":"删除旧版模型 "+group.label+"？",group.files.size()+" 个本地文件，删除后无法撤销。",0));
        }
    }
    @Override protected void keyTyped(char c,int key) throws IOException { if (key==Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); else super.keyTyped(c,key); }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground(); int left=width/2-155;
        drawCenteredString(fontRendererObj,"旧版模型与未识别文件",width/2,15,0xFFFFFF);
        drawCenteredString(fontRendererObj,"有效模型不会自动删除；只管理游戏目录内文件",width/2,32,0xBBBBBB);
        for (int i=page*2;i<Math.min(page*2+2,groups.size());i++) {
            ModelFileRegistry.Group group=groups.get(i); int row=50+(i%2)*45;
            drawString(fontRendererObj,fontRendererObj.trimStringToWidth(group.unknown?"未识别文件":group.label,220),left,row+3,0xEEEEEE);
            drawString(fontRendererObj,fontRendererObj.trimStringToWidth(group.files.get(0).path,310),left,row+24,0xBBBBBB);
            drawString(fontRendererObj,group.files.size()+" 个文件 · "+group.bytes/1048576+" MiB",left,row+13,0xBBBBBB);
        }
        drawString(fontRendererObj,fontRendererObj.trimStringToWidth(status,310),left,height-92,0x99DDFF);
        drawCenteredString(fontRendererObj,(page+1)+" / "+Math.max(1,(groups.size()+1)/2),width/2,height-70,0xFFFFFF);
        super.drawScreen(x,y,partial);
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
