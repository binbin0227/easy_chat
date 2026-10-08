package io.github.easychat.gui;
import io.github.easychat.EasyChat;
import io.github.easychat.chat.TextModelCatalog;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import java.io.IOException;

/** Catalog overview; choosing an entry only opens its resource controls. */
final class TextModelManagerScreen extends GuiScreen {
    private final EasyChat mod; private final GuiScreen parent;
    private int page;
    TextModelManagerScreen(EasyChat mod,GuiScreen parent){this.mod=mod;this.parent=parent;}
    @Override public void initGui(){int left=width/2-155;buttonList.clear();
        for(int i=page*3;i<Math.min(page*3+3,TextModelCatalog.all().size());i++)buttonList.add(new GuiButton(100+i,left,51+(i%3)*43,310,20,TextModelCatalog.all().get(i).name+" · 管理"));
        if(TextModelCatalog.all().size()>3){buttonList.add(new GuiButton(4,left,31,45,18,"上一页"));buttonList.add(new GuiButton(5,left+265,31,45,18,"下一页"));}
        buttonList.add(new GuiButton(3,left,height-27,150,20,"清除下载缓存"));buttonList.add(new GuiButton(0,left+160,height-27,150,20,"返回"));
    }
    @Override public void updateScreen(){for(GuiButton button:buttonList)if(button.id==3)button.enabled=!mod.textTranslation.busy&&!mod.controller.busy();}
    @Override protected void actionPerformed(GuiButton button)throws IOException{
        if(button.id>=100)mc.displayGuiScreen(new TextModelDetailsScreen(mod,this,TextModelCatalog.all().get(button.id-100)));
        else if(button.id==3)mod.controller.clearDownloadCache();else if(button.id==4||button.id==5){page=(page+(button.id==4?-1:1)+(TextModelCatalog.all().size()+2)/3)%((TextModelCatalog.all().size()+2)/3);initGui();}else mc.displayGuiScreen(parent);
    }
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);else super.keyTyped(c,key);}
    @Override public void drawScreen(int x,int y,float partial){drawDefaultBackground();int left=width/2-155;
        drawCenteredString(fontRendererObj,"文字模型管理",width/2,15,0xFFFFFF);
        drawCenteredString(fontRendererObj,"下载后到对应翻译页选择 · "+(page+1)+"/"+((TextModelCatalog.all().size()+2)/3),width/2,34,0xBBBBBB);
        for(int i=page*3;i<Math.min(page*3+3,TextModelCatalog.all().size());i++){TextModelCatalog.Model model=TextModelCatalog.all().get(i);
            drawString(fontRendererObj,model.directionLabel()+" · "+(model.bundled()?"JAR 内置":"按需下载")+" · "+model.sizeLabel(),left,74+(i%3)*43,0xCCCCCC);
            drawString(fontRendererObj,fontRendererObj.trimStringToWidth(mod.textTranslation.modelState(model.id),310),left,84+(i%3)*43,0x99DDFF);
        }
        drawString(fontRendererObj,fontRendererObj.trimStringToWidth(mod.textTranslation.status,310),left,height-43,0x99DDFF);super.drawScreen(x,y,partial);
    }
    @Override public boolean doesGuiPauseGame(){return false;}
}
