package io.github.easychat.gui;
import io.github.easychat.EasyChat;
import io.github.easychat.chat.TextModelCatalog;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.nio.file.Paths;

final class TextModelDetailsScreen extends GuiScreen {
    private final EasyChat mod;private final GuiScreen parent;private final TextModelCatalog.Model model;
    private GuiTextField path;private String error="";
    TextModelDetailsScreen(EasyChat mod,GuiScreen parent,TextModelCatalog.Model model){this.mod=mod;this.parent=parent;this.model=model;}
    @Override public void initGui(){int left=width/2-155;buttonList.clear();
        buttonList.add(new GuiButton(1,left,102,150,20,model.bundled()?"准备 / 恢复内置模型":"下载此模型"));
        buttonList.add(new GuiButton(2,left+160,102,150,20,"删除本地副本"));
        if(!model.bundled()){path=new GuiTextField(4,fontRendererObj,left,146,310,18);path.setMaxStringLength(1024);buttonList.add(new GuiButton(4,left,173,310,20,"导入此型号的官方文件"));}
        buttonList.add(new GuiButton(0,left,height-27,310,20,"返回模型列表"));Keyboard.enableRepeatEvents(true);
    }
    @Override public void updateScreen(){if(path!=null)path.updateCursorCounter();boolean busy=mod.textTranslation.busy||mod.controller.busy();
        for(GuiButton button:buttonList){button.enabled=button.id==0||!busy;
            if(button.id==1)button.displayString=model.bundled()?"准备 / 恢复内置模型":"下载此模型";
            if(button.id==1&&mod.textTranslation.ready(model.id)&&mod.textTranslation.exists(model.id)){button.displayString=model.bundled()?"模型已准备好":"下载源 / 链接";button.enabled=!model.bundled()&&!busy;}
            if(button.id==2)button.enabled&=mod.textTranslation.exists(model.id);
        }
    }
    @Override protected void actionPerformed(GuiButton button)throws IOException{
        error="";if(button.id==0)mc.displayGuiScreen(parent);
        else if(button.id==1){if(model.bundled())mod.textTranslation.prepare(model.id);else mc.displayGuiScreen(new ModelDownloadScreen(mod,this,null,model));}
        else if(button.id==2)mc.displayGuiScreen(new GuiYesNo((confirmed,id)->{if(confirmed)mod.textTranslation.delete(model.id);mc.displayGuiScreen(this);},"删除 "+model.name+"？",model.bundled()?"仅删除本地副本；需手动恢复。当前选择将回退。":"当前选择将回退；没有可用模型时停用。",2));
        else if(button.id==4)try{mod.textTranslation.importModel(model.id,Paths.get(path.getText().trim().replace("\"","")));}catch(RuntimeException invalid){error="请输入有效文件路径";}
    }
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{super.mouseClicked(x,y,button);if(path!=null)path.mouseClicked(x,y,button);}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);else if(path==null||!path.textboxKeyTyped(c,key))super.keyTyped(c,key);}
    @Override public void drawScreen(int x,int y,float partial){drawDefaultBackground();int left=width/2-155;
        drawCenteredString(fontRendererObj,model.name,width/2,15,0xFFFFFF);
        drawString(fontRendererObj,model.directionLabel()+" · "+model.sizeLabel()+" · "+model.license,left,36,0xBBBBBB);
        drawString(fontRendererObj,(model.bundled()?"JAR 内置 · 无需联网下载":"按需下载 / 离线导入 · 权重不在 JAR 中"),left,49,0xBBBBBB);
        drawString(fontRendererObj,model.gpu()?"CPU / 显卡 · 引擎内置，无需 Ollama":"CPU · 首次使用自动释放到游戏目录",left,62,0xBBBBBB);
        drawString(fontRendererObj,fontRendererObj.trimStringToWidth(mod.textTranslation.modelState(model.id),310),left,82,0x99DDFF);
        if(path!=null){drawString(fontRendererObj,"离线导入："+model.file,left,132,0xCCCCCC);path.drawTextBox();}
        else fontRendererObj.drawSplitString("更新模组不会删除有效缓存。删除副本后不会自动恢复；恢复按钮从 JAR 重新释放，不改变使用选择。",left,139,310,0xCCCCCC);
        drawString(fontRendererObj,fontRendererObj.trimStringToWidth(error.isEmpty()?mod.textTranslation.status:error,310),left,height-43,0x99DDFF);super.drawScreen(x,y,partial);
    }
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
