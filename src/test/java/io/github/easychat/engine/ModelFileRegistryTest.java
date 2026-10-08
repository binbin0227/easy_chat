package io.github.easychat.engine;

import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class ModelFileRegistryTest {
    private static void clean(Path root) throws IOException {
        try (java.util.stream.Stream<Path> paths=Files.walk(root)) {
            for (Path path:paths.sorted(Comparator.reverseOrder()).toArray(Path[]::new)) Files.deleteIfExists(path);
        }
    }
    @Test public void tracksRetiredAndUnknownFilesWithoutRemovingCurrentModels() throws Exception {
        Path root=Files.createTempDirectory("easychat-inventory-");
        try {
            Path current=root.resolve("models/paraformer-zh-int8/model.int8.onnx"), retired=root.resolve("text-model/qwen3-1.7b/Qwen3-1.7B-Q8_0.gguf"), unknown=root.resolve("models/my-note.txt");
            Files.createDirectories(current.getParent()); Files.createDirectories(retired.getParent());
            Files.write(current,new byte[]{1}); Files.write(retired,new byte[]{2}); Files.write(unknown,new byte[]{3});
            ModelFileRegistry registry=ModelFileRegistry.at(root);
            List<ModelFileRegistry.Group> groups=registry.leftovers();
            assertEquals(2,groups.size()); assertTrue(Files.exists(current)); assertTrue(Files.exists(unknown));
            registry.deleteLeftover("qwen3-1.7b-q8");
            assertFalse(Files.exists(retired)); assertFalse(Files.exists(retired.getParent()));
            assertTrue(Files.exists(current)); assertTrue(Files.exists(unknown));
            try { registry.deleteLeftover("asr:paraformer-zh-int8"); fail("active model deletion must be rejected"); }
            catch (IOException expected) { assertTrue(Files.exists(current)); }
        } finally { clean(root); }
    }
    @Test public void remembersModelsThatHaveDisappearedFromCatalog() throws Exception {
        Path root=Files.createTempDirectory("easychat-retired-");
        try {
            Path model=root.resolve("text-model/older-model/weights.bin"); Files.createDirectories(model.getParent()); Files.write(model,new byte[]{1});
            String state="{\"schema\":1,\"files\":[{\"modelId\":\"older-model\",\"label\":\"Previous model\",\"revision\":\"r1\",\"path\":\"text-model/older-model/weights.bin\",\"role\":\"MODEL\",\"algorithm\":\"SHA-256\",\"digest\":\"test\"}]}";
            Files.write(root.resolve("model-files.json"),state.getBytes(StandardCharsets.UTF_8));
            ModelFileRegistry registry=ModelFileRegistry.at(root); registry.refresh();
            assertEquals("older-model",registry.leftovers().get(0).key); assertFalse(registry.leftovers().get(0).unknown);
            registry.deleteLeftover("older-model"); assertFalse(Files.exists(model));
        } finally { clean(root); }
    }
    @Test public void clearsKnownOldPartialsButPreservesUnknownAndCompleteFiles() throws Exception {
        Path root=Files.createTempDirectory("easychat-partials-");
        try {
            Path partial=root.resolve("text-model/qwen3-1.7b/Qwen3-1.7B-Q8_0.gguf.part"), complete=root.resolve("models/ggml-small.bin"), unknown=root.resolve("models/manual.part");
            Files.createDirectories(partial.getParent()); Files.createDirectories(complete.getParent());
            Files.write(partial,new byte[]{1,2,3}); Files.write(complete,new byte[]{4}); Files.write(unknown,new byte[]{5});
            assertEquals(3,ModelFileRegistry.at(root).clearTemporaryFiles());
            assertFalse(Files.exists(partial)); assertTrue(Files.exists(complete)); assertTrue(Files.exists(unknown));
        } finally { clean(root); }
    }
    @Test public void rejectsEscapedPathsAndRetainsBadManifest() throws Exception {
        Path root=Files.createTempDirectory("easychat-escape-");
        try {
            Path manifest=root.resolve("model-files.json");
            String state="{\"schema\":1,\"files\":[{\"modelId\":\"bad\",\"path\":\"models/../../outside.bin\",\"role\":\"MODEL\"}]}";
            Files.write(manifest,state.getBytes(StandardCharsets.UTF_8));
            try { ModelFileRegistry.at(root).refresh(); fail("must reject escaped path"); } catch (IOException expected) { }
            assertEquals(state,new String(Files.readAllBytes(manifest),StandardCharsets.UTF_8));
            String insideEscape=state.replace("models/../../outside.bin","models/../settings.properties");
            Files.write(root.resolve("settings.properties"),new byte[]{42});
            Files.write(manifest,insideEscape.getBytes(StandardCharsets.UTF_8));
            try { ModelFileRegistry.at(root).refresh(); fail("must stay inside model directories"); } catch (IOException expected) { }
            assertArrayEquals(new byte[]{42},Files.readAllBytes(root.resolve("settings.properties")));
        } finally { clean(root); }
    }
}
