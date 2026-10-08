package io.github.easychat.engine;

import com.google.gson.*;
import io.github.easychat.chat.TextModelCatalog;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent ownership inventory, independent of the current download catalog. */
public final class ModelFileRegistry {
    private static final Map<Path, ModelFileRegistry> INSTANCES = new ConcurrentHashMap<>();
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static ModelFileRegistry at(Path root) {
        Path key = root.toAbsolutePath().normalize();
        return INSTANCES.computeIfAbsent(key, ModelFileRegistry::new);
    }
    public static final class Entry {
        public String modelId, label, revision, path, role, algorithm, digest;
        Entry(String id, String label, String revision, String path, String role, String algorithm, String digest) {
            this.modelId=id; this.label=label; this.revision=revision; this.path=path;
            this.role=role; this.algorithm=algorithm; this.digest=digest;
        }
    }
    private static final class State {
        int schema = 1;
        List<Entry> files = new ArrayList<>();
    }
    public static final class Group {
        public final String key, label;
        public final boolean unknown;
        public final List<Entry> files = new ArrayList<>();
        public long bytes;
        Group(String key, Entry entry) { this.key=key; this.label=entry.label; unknown="UNKNOWN".equals(entry.role); }
    }
    private final Path root, manifest;
    private ModelFileRegistry(Path root) { this.root=root; manifest=root.resolve("model-files.json"); }
    private boolean allowed(String name) {
        return name != null && (name.startsWith("models/") || name.startsWith("text-model/"));
    }
    private Path safe(String name) throws IOException {
        if (!allowed(name)) throw new IOException("登记文件不在模型目录中");
        Path result = root.resolve(name).normalize();
        if (!result.startsWith(root) || result.equals(root)) throw new IOException("模型路径越界");
        if (!result.startsWith(root.resolve("models")) && !result.startsWith(root.resolve("text-model")))
            throw new IOException("登记文件不在模型目录中");
        for (Path part=result; part!=null && part.startsWith(root); part=part.getParent())
            if (Files.isSymbolicLink(part)) throw new IOException("不操作模型目录中的链接");
        if (Files.exists(result) && !result.toRealPath().startsWith(root.toRealPath())) throw new IOException("模型实际路径越界");
        return result;
    }
    private State load() throws IOException {
        if (!Files.exists(manifest)) return new State();
        try (Reader reader=Files.newBufferedReader(manifest,StandardCharsets.UTF_8)) {
            State state=JSON.fromJson(reader,State.class);
            if (state==null || state.schema!=1 || state.files==null) throw new IOException("模型登记表版本不支持");
            for (Entry entry:state.files) {
                if (entry==null || entry.modelId==null || entry.role==null) throw new IOException("模型登记表内容无效");
                safe(entry.path);
            }
            return state;
        } catch (JsonParseException error) { throw new IOException("模型登记表损坏，已保留原文件",error); }
    }
    private void save(State state) throws IOException {
        Files.createDirectories(root);
        Path temporary=manifest.resolveSibling("model-files.json.tmp");
        try {
            try (Writer writer=Files.newBufferedWriter(temporary,StandardCharsets.UTF_8)) { JSON.toJson(state,writer); }
            try { Files.move(temporary,manifest,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary,manifest,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    private void add(Map<String,Entry> result,String id,String label,String revision,String path,String algorithm,String digest) {
        result.put(path,new Entry(id,label,revision,path,"MODEL",algorithm,digest));
        result.put(path+".part",new Entry(id,label,revision,path+".part","TEMP",algorithm,digest));
    }
    private Map<String,Entry> descriptors() throws IOException {
        Map<String,Entry> result=new LinkedHashMap<>();
        for (ModelStore.Model model:ModelStore.Model.values()) {
            String directory="models/"+model.id+"/",id="asr:"+model.id;
            add(result,id,model.label,model.sha256,directory+"model.int8.onnx","SHA-256",model.sha256);
            add(result,id,model.label,model.sha256,directory+"tokens.txt","SHA-256",ModelStore.TOKEN_HASH);
            result.put(directory+"import.part",new Entry(id,model.label,model.sha256,directory+"import.part","TEMP","SHA-256",model.sha256));
            result.put(directory+".removed",new Entry(id,model.label,model.sha256,directory+".removed","CONTROL","",""));
        }
        // Removed engines remain inventoried so updates never create unmanageable orphan weights.
        String[] oldIds={"base","small","tiny","medium","large-v3-q5_0"};
        String[] oldHashes={"465707469ff3a37a2b9b8d8f89f2f99de7299dac","55356645c2b361a969dfd0ef2c5a50d530afd8d5","bd577a113a864445d4c299885e0cb97d4ba92b5f","fd9727b6e1217c2f614f9b698455c4ffd82463b4","e6e2ed78495d403bef4b7cff42ef4aaadcfea8de"};
        for(int i=0;i<oldIds.length;i++) {
            String id="whisper:"+oldIds[i],label="旧 Whisper "+oldIds[i],hash=oldHashes[i];
            add(result,id,label,hash,"models/ggml-"+oldIds[i]+".bin","SHA-1",hash);
            for(String prefix:new String[]{"bundled-","import-"}) {
                String temporary="models/"+prefix+oldIds[i]+".part";
                result.put(temporary,new Entry(id,label,hash,temporary,"TEMP","SHA-1",hash));
            }
            String marker="models/ggml-"+oldIds[i]+".disabled";
            result.put(marker,new Entry(id,label,hash,marker,"CONTROL","",""));
        }
        String qwen="text-model/qwen3-1.7b/",qwenHash="061b54daade076b5d3362dac252678d17da8c68f07560be70818cace6590cb1a";
        add(result,"qwen3-1.7b-q8","旧 Qwen3 1.7B Q8_0","legacy",qwen+"Qwen3-1.7B-Q8_0.gguf","SHA-256",qwenHash);
        result.put(qwen+"import.part",new Entry("qwen3-1.7b-q8","旧 Qwen3","legacy",qwen+"import.part","TEMP","SHA-256",qwenHash));
        // Recognize files left by releases that shipped the retired OPUS direction.
        String retired="text-model/opus-zh-en/";
        String[] names={"encoder_model_quantized.onnx","decoder_model_quantized.onnx","tokenizer.json"};
        String[] hashes={"84d5e171b626bc8b6b220d022ac58696e9528c25deeacca62b5cbf4364547a99","debcc3054b9ff9aaed972bd6e251a459abe1e2f1402da1efa63163dc2f46ef73","b306d0301cf280bfd647d7067b5ade2a97b987e6d678df110703c002433643ff"};
        for (int i=0;i<names.length;i++) add(result,"opus-zh-en","OPUS-MT 中译英","39d480d52a9ea3065a1f117adfe4dbc55de10e6f",retired+names[i],"SHA-256",hashes[i]);
        result.put(retired+".removed",new Entry("opus-zh-en","OPUS-MT 中译英","legacy",retired+".removed","CONTROL","",""));
        for (TextModelCatalog.Model model:TextModelCatalog.all()) {
            String directory=model.directory+"/";
            if (model.bundled()) {
                Properties hashesInJar=new Properties();
                try (InputStream input=ModelFileRegistry.class.getResourceAsStream(model.manifest)) { if (input!=null) hashesInJar.load(input); }
                for (String name:names) add(result,model.id,model.name,hashesInJar.getProperty(name,""),directory+name,"SHA-256",hashesInJar.getProperty(name,""));
            } else {
                add(result,model.id,model.name,model.revision,directory+model.file,"SHA-256",model.sha256);
                result.put(directory+"import.part",new Entry(model.id,model.name,model.revision,directory+"import.part","TEMP","SHA-256",model.sha256));
            }
            result.put(directory+".removed",new Entry(model.id,model.name,model.revision,directory+".removed","CONTROL","",""));
        }
        return result;
    }
    private Set<String> currentPaths(Map<String,Entry> descriptors) {
        Set<String> ids=new HashSet<>(), paths=new HashSet<>();
        for (ModelStore.Model model:ModelStore.Model.values()) ids.add("asr:"+model.id);
        for (TextModelCatalog.Model model:TextModelCatalog.all()) ids.add(model.id);
        for (Entry entry:descriptors.values()) if (ids.contains(entry.modelId)) paths.add(entry.path);
        return paths;
    }
    public synchronized void refresh() throws IOException {
        State old=load(), next=new State();
        Map<String,Entry> known=descriptors(), previous=new HashMap<>();
        for (Entry entry:old.files) previous.put(entry.path,entry);
        for (String directory:new String[]{"models","text-model"}) {
            Path folder=root.resolve(directory);
            if (!Files.isDirectory(folder,LinkOption.NOFOLLOW_LINKS)) continue;
            safe(directory+"/");
            Files.walkFileTree(folder,new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws IOException {
                    if (Files.isSymbolicLink(dir) || !dir.toRealPath().startsWith(root.toRealPath())) return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile() || Files.isSymbolicLink(file)) return FileVisitResult.CONTINUE;
                    String name=root.relativize(file).toString().replace('\\','/'); safe(name);
                    Entry entry=known.get(name);
                    if (entry==null) entry=previous.get(name);
                    if (entry==null) entry=new Entry("unknown:"+name,"未识别文件", "",name,"UNKNOWN","","");
                    next.files.add(entry); return FileVisitResult.CONTINUE;
                }
            });
        }
        save(next);
    }
    public synchronized List<Group> leftovers() throws IOException {
        refresh();
        Set<String> current=currentPaths(descriptors());
        Map<String,Group> groups=new LinkedHashMap<>();
        for (Entry entry:load().files) {
            if (current.contains(entry.path)) continue;
            String key=entry.modelId;
            Group group=groups.computeIfAbsent(key,unused->new Group(key,entry));
            group.files.add(entry); group.bytes+=Files.size(safe(entry.path));
        }
        return new ArrayList<>(groups.values());
    }
    public synchronized long clearTemporaryFiles() throws IOException {
        refresh(); long bytes=0;
        for (Entry entry:load().files) if ("TEMP".equals(entry.role)) {
            Path file=safe(entry.path);
            if (Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) { bytes+=Files.size(file); Files.delete(file); }
        }
        refresh(); return bytes;
    }
    public synchronized void deleteLeftover(String key) throws IOException {
        // Recompute the inventory: never trust stale UI rows or delete active model paths.
        for (Group group:leftovers()) if (group.key.equals(key)) {
            for (Entry entry:group.files) Files.deleteIfExists(safe(entry.path));
            for (Entry entry:group.files) {
                Path directory=safe(entry.path).getParent();
                while (!directory.equals(root.resolve("models")) && !directory.equals(root.resolve("text-model"))) {
                    try { Files.deleteIfExists(directory); }
                    catch (DirectoryNotEmptyException inUse) { break; }
                    directory=directory.getParent();
                }
            }
            refresh(); return;
        }
        throw new IOException("文件列表已变化，请刷新后重试");
    }
}
