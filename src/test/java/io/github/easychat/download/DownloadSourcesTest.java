package io.github.easychat.download;

import org.junit.Test;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.*;

public class DownloadSourcesTest {
    @Test public void savesChoicesAndMigratesOldAutomaticSettings() throws Exception {
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("easychat-download-settings-");
        java.nio.file.Path file=root.resolve("settings.properties");
        try {
            java.nio.file.Files.write(file,"voiceDownloadSource=AUTO\ntextDownloadSource=MIRROR\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            io.github.easychat.config.ChatSettings settings=new io.github.easychat.config.ChatSettings(file); settings.load();
            assertEquals(DownloadSources.Source.OFFICIAL,settings.voiceDownloadSource);
            assertEquals(DownloadSources.Source.MIRROR,settings.textDownloadSource);
            settings.textDownloadSource=DownloadSources.Source.MODELSCOPE; settings.save();
            io.github.easychat.config.ChatSettings loaded=new io.github.easychat.config.ChatSettings(file); loaded.load();
            assertEquals(DownloadSources.Source.OFFICIAL,loaded.voiceDownloadSource);
            assertEquals(DownloadSources.Source.MODELSCOPE,loaded.textDownloadSource);
        } finally { java.nio.file.Files.deleteIfExists(file); java.nio.file.Files.delete(root); }
    }
    @Test public void oldAutomaticPreferenceMigratesToOfficialAndExplicitBackupPersists() {
        assertEquals(DownloadSources.Source.OFFICIAL,DownloadSources.Source.parse("AUTO"));
        assertEquals(DownloadSources.Source.OFFICIAL,DownloadSources.Source.parse("invalid"));
        assertEquals(DownloadSources.Source.MIRROR,DownloadSources.Source.parse("MIRROR"));
        assertEquals(DownloadSources.Source.MODELSCOPE,DownloadSources.Source.parse("MODELSCOPE"));
    }
    @Test public void selectingOneSourceNeverSilentlyFallsBack() throws Exception {
        List<DownloadSources.Link> links=DownloadSources.links("paraformer-zh-int8",null,null,null);
        assertEquals(2,links.size());
        assertEquals(1,DownloadSources.selected(links,DownloadSources.Source.OFFICIAL).size());
        assertTrue(DownloadSources.selected(links,DownloadSources.Source.MIRROR).get(0).url.startsWith("https://hf-mirror.com/"));
        try { DownloadSources.selected(links,DownloadSources.Source.MODELSCOPE); fail("unsupported source must be rejected"); }
        catch (IOException expected) { }
    }
}
