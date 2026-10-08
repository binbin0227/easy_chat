package io.github.easychat.chat;
import org.junit.Test;
import static org.junit.Assert.*;
public class TextModelCatalogTest {
    @Test public void onlyOpusDirectionsRemainAndAliasResolvesByDirection() {
        assertEquals(2,TextModelCatalog.all().size());
        assertEquals("opus-zh-en",TextModelCatalog.resolve("opus",true).id);assertEquals("opus-en-zh",TextModelCatalog.resolve("opus",false).id);
        assertTrue(TextModelCatalog.find("opus-zh-en").supports(true));assertFalse(TextModelCatalog.find("opus-zh-en").supports(false));
        assertTrue(TextModelCatalog.find("opus-en-zh").supports(false));assertNull(TextModelCatalog.find("qwen3-1.7b-q8"));
        for(TextModelCatalog.Model model:TextModelCatalog.all()){assertTrue(model.bundled());assertFalse(model.gpu());}
    }
}
