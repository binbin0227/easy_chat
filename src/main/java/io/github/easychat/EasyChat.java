package io.github.easychat;

import io.github.easychat.config.ChatSettings;
import io.github.easychat.core.ChatController;
import io.github.easychat.gui.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.lwjgl.input.Keyboard;
import java.nio.file.Path;

@Mod(modid = EasyChat.MOD_ID, name = "Easy Chat", version = "0.1.0", clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]", acceptableRemoteVersions = "*")
public final class EasyChat {
    public static final String MOD_ID = "easychat";
    public static EasyChat instance;
    public ChatSettings settings;
    public io.github.easychat.engine.ModelFileRegistry modelFiles;
    public ChatController controller;
    public io.github.easychat.chat.IncomingChatTranslator incoming;
    public io.github.easychat.chat.TextTranslationService textTranslation;
    public KeyBinding talkKey, settingsKey;

    @Mod.EventHandler public void initialize(FMLInitializationEvent event) {
        instance = this;
        Path root = Minecraft.getMinecraft().mcDataDir.toPath().resolve("easychat");
        modelFiles = io.github.easychat.engine.ModelFileRegistry.at(root);
        settings = new ChatSettings(root.resolve("settings.properties"));
        try { settings.load(); } catch (Exception error) { System.err.println("[Easy Chat] 设置读取失败：" + error.getMessage()); }
        talkKey = new KeyBinding("key.easychat.talk", settings.talkKey, "key.categories.easychat");
        settingsKey = new KeyBinding("key.easychat.settings", settings.settingsKey, "key.categories.easychat");
        ClientRegistry.registerKeyBinding(talkKey);
        ClientRegistry.registerKeyBinding(settingsKey);
        textTranslation = new io.github.easychat.chat.TextTranslationService(root, settings);
        controller = new ChatController(this, root);
        incoming = new io.github.easychat.chat.IncomingChatTranslator(this, root);
        MinecraftForge.EVENT_BUS.register(incoming);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(incoming);
        MinecraftForge.EVENT_BUS.register(controller);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(controller);
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override public String getCommandName() { return "easychat"; }
            @Override public String getCommandUsage(ICommandSender sender) { return "/easychat"; }
            @Override public int getRequiredPermissionLevel() { return 0; }
            @Override public void processCommand(ICommandSender sender, String[] args) {
                // The command is executed from GuiChat; open on the next client tick.
                controller.requestSettings();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { controller.shutdown(); incoming.shutdown(); textTranslation.close(); }, "easychat-shutdown"));
    }

    public void openSettings() { Minecraft.getMinecraft().displayGuiScreen(new SettingsScreen(this)); }
    public void saveSettings() throws java.io.IOException {
        settings.talkKey = talkKey.getKeyCode();
        settings.settingsKey = settingsKey.getKeyCode();
        settings.save();
        Minecraft.getMinecraft().gameSettings.saveOptions();
    }
    public void setTalkKey(int key) {
        talkKey.setKeyCode(key);
        KeyBinding.resetKeyBindingArrayAndHash();
    }
    public void resetSettings() {
        settings.microphone = "";
        settings.model = "paraformer-zh-int8";
        settings.sendMode = ChatSettings.SendMode.AUTO;
        settings.hintMode = ChatSettings.HintMode.TOP_LEFT;
        settings.translateIncoming = true;
        settings.speechTextModel = "opus-zh-en";
        settings.incomingTextModel = "opus-en-zh";
        settings.voiceDownloadSource = io.github.easychat.download.DownloadSources.Source.OFFICIAL;
        settings.textDownloadSource = io.github.easychat.download.DownloadSources.Source.OFFICIAL;
        setTalkKey(Keyboard.KEY_V);
    }
}
