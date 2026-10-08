package io.github.easychat.engine;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;
public class ModelStoreTest {
    @Test public void wrongImportPreservesExistingWeightAndSource() throws Exception {
        Path root=Files.createTempDirectory("easychat-import-");ModelStore store=new ModelStore(root);ModelStore.Model model=ModelStore.Model.PARAFORMER;Path target=store.file(model),source=root.resolve("wrong.onnx");Files.createDirectories(target.getParent());Files.write(target,new byte[]{9});Files.write(source,new byte[]{1});
        try {try{store.importFile(model,source,s->{});fail("wrong model accepted");}catch(java.io.IOException expected){}
            assertArrayEquals(new byte[]{9},Files.readAllBytes(target));assertArrayEquals(new byte[]{1},Files.readAllBytes(source));assertFalse(Files.exists(target.resolveSibling("import.part")));
        } finally {try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new))Files.delete(p);}}
    }
    @Test public void cleanupPreservesCompleteAndUnrecognizedFiles() throws Exception {
        Path root=Files.createTempDirectory("easychat-cache-");ModelStore store=new ModelStore(root);Path folder=store.file(ModelStore.Model.PARAFORMER).getParent();Files.createDirectories(folder);
        Path partial=folder.resolve("model.int8.onnx.part"),unknown=folder.resolve("manual.part"),complete=store.file(ModelStore.Model.PARAFORMER);Files.write(partial,new byte[]{1,2});Files.write(unknown,new byte[]{3});Files.write(complete,new byte[]{4});
        try {assertEquals(2,store.clearDownloadCache());assertFalse(Files.exists(partial));assertTrue(Files.exists(unknown));assertTrue(Files.exists(complete));assertEquals(ModelStore.Model.PARAFORMER,ModelStore.Model.from("base"));}
        finally {try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new))Files.delete(p);}}
    }
}
