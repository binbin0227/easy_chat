package io.github.easychat.config;
import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class ChatSettingsTest {
    @Test public void migratesRemovedEnginesButPreservesPlayerPreferences() throws Exception {
        Path root=Files.createTempDirectory("easychat-migrate-");Path file=root.resolve("settings.properties");
        try {
            Files.write(file,("model=large-v3-q5_0\nspeechMode=DIRECT\nspeechTextModel=qwen3-1.7b-q8\nincomingTextModel=qwen3-1.7b-q8\ngpuAcceleration=true\nmicrophone=USB麦克风\nsendMode=DRAFT\ntalkKey=-97\nhintMode=CHAT\ntranslateIncoming=false\n").getBytes(StandardCharsets.UTF_8));
            ChatSettings settings=new ChatSettings(file);settings.load();settings.save();
            assertEquals("paraformer-zh-int8",settings.model);assertEquals("opus-zh-en",settings.speechTextModel);assertEquals("opus-en-zh",settings.incomingTextModel);
            assertEquals("USB麦克风",settings.microphone);assertEquals(-97,settings.talkKey);assertEquals(ChatSettings.SendMode.DRAFT,settings.sendMode);assertFalse(settings.translateIncoming);
            String saved=new String(Files.readAllBytes(file),StandardCharsets.UTF_8);assertFalse(saved.contains("speechMode"));assertFalse(saved.contains("gpuAcceleration"));assertFalse(saved.contains("qwen"));
        } finally {Files.deleteIfExists(file);Files.delete(root);}
    }
    @Test public void defaultsAndWrongDirectionAreSafe() throws Exception {
        Path root=Files.createTempDirectory("easychat-settings-");Path file=root.resolve("settings.properties");
        try {
            ChatSettings settings=new ChatSettings(file);assertTrue(settings.translateIncoming);assertEquals("opus-zh-en",settings.speechTextModel);
            Files.write(file,"model=unknown\nspeechTextModel=opus-en-zh\nincomingTextModel=opus-zh-en\ntalkKey=999\n".getBytes(StandardCharsets.UTF_8));settings.load();
            assertEquals("paraformer-zh-int8",settings.model);assertEquals("opus-zh-en",settings.speechTextModel);assertEquals("opus-en-zh",settings.incomingTextModel);assertEquals(47,settings.talkKey);
        } finally {Files.deleteIfExists(file);Files.delete(root);}
    }
    @Test public void concurrentSavesRemainAtomic() throws Exception {
        Path root=Files.createTempDirectory("easychat-save-");Path file=root.resolve("settings.properties");ChatSettings settings=new ChatSettings(file);
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            java.util.List<java.util.concurrent.Future<?>> futures=new java.util.ArrayList<>();
            for(int i=0;i<4;i++)futures.add(pool.submit(()->{for(int j=0;j<5;j++)settings.save();return null;}));
            for(java.util.concurrent.Future<?> f:futures)f.get();ChatSettings loaded=new ChatSettings(file);loaded.load();assertEquals("opus-en-zh",loaded.incomingTextModel);assertFalse(Files.exists(root.resolve("settings.properties.tmp")));
        } finally {pool.shutdownNow();Files.deleteIfExists(file);Files.delete(root);}
    }
}
