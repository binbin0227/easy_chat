package io.github.easychat.chat;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Curated model metadata. Engine compatibility is explicit, not inferred from file extension. */
public final class TextModelCatalog {
    public static final class Model {
        public String id, name, directory, engine, direction, resource, manifest, file, sha256, repository, revision, license, profile;
        public long bytes;
        public boolean supports(boolean toEnglish) { return "both".equals(direction) || (toEnglish ? "zh-en" : "en-zh").equals(direction); }
        public boolean bundled() { return resource != null; }
        public boolean gpu() { return "gguf".equals(engine); }
        public String directionLabel() { return "both".equals(direction) ? "中英双向" : "zh-en".equals(direction) ? "中文 → 英文" : "英文 → 中文"; }
        public String sizeLabel() { return bytes / 1048576 + " MiB"; }
    }
    private static final List<Model> MODELS = read();
    private static List<Model> read() {
        try (InputStream input = TextModelCatalog.class.getResourceAsStream("/easychat/text-model-catalog.json")) {
            if (input == null) throw new IOException("缺少文字模型目录");
            Model[] models = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Model[].class);
            Set<String> ids = new HashSet<>();
            for (Model model : models) {
                if (!model.id.matches("[a-z0-9.-]+") || !ids.add(model.id) || !model.directory.matches("text-model/[a-z0-9.-]+")) throw new IOException("模型目录无效");
                if (!"opus".equals(model.engine) || !model.bundled()) throw new IOException("未支持的推理引擎");
            }
            return Collections.unmodifiableList(Arrays.asList(models));
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    public static List<Model> all() { return MODELS; }
    public static Model find(String id) { for (Model model : MODELS) if (model.id.equals(id)) return model; return null; }
    public static Model resolve(String id, boolean toEnglish) { return "opus".equals(id) ? find(toEnglish ? "opus-zh-en" : "opus-en-zh") : find(id); }
    private TextModelCatalog() { }
}
