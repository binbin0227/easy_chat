package io.github.easychat.chat;
import io.github.easychat.config.ChatSettings;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;
public class TextTranslationServiceTest {
    @Test public void scanMigratesMissingQwenAndNeverSelectsWrongDirection() throws Exception {
        Path root=Files.createTempDirectory("easychat-opus-");ChatSettings settings=new ChatSettings(root.resolve("settings.properties"));settings.incomingTextModel="qwen3-1.7b-q8";settings.speechTextModel="qwen3-1.7b-q8";
        try(TextTranslationService service=new TextTranslationService(root,settings)) {
            service.scan();assertEquals("opus-en-zh",settings.incomingTextModel);assertEquals("opus-zh-en",settings.speechTextModel);assertTrue(settings.translateIncoming);
            service.select("opus-en-zh",true);assertEquals("opus-zh-en",settings.speechTextModel);service.select("qwen3-1.7b-q8",false);assertEquals("opus-en-zh",settings.incomingTextModel);
        } finally {Files.deleteIfExists(root.resolve("settings.properties"));Files.delete(root);}
    }
}
