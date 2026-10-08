package io.github.easychat.chat;
import ai.onnxruntime.*;
import io.github.easychat.engine.ModelStore;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Entirely local CPU translation; owned by one bounded chat worker. */
public final class LocalTextTranslator implements AutoCloseable {
    private final Path root;
    private final String modelDirectory, resourceDirectory, manifestName;
    private OrtSession encoder, decoder;
    private OrtEnvironment environment;
    private UnigramTokenizer tokenizer;
    private boolean inventoried;
    private final boolean toEnglish;
    public volatile String phase = "等待消息";
    public LocalTextTranslator(Path root) { this(root, false); }
    public LocalTextTranslator(Path root, boolean toEnglish) {
        this(root, TextModelCatalog.resolve("opus", toEnglish));
    }
    public LocalTextTranslator(Path root, TextModelCatalog.Model model) {
        if (model == null || !"opus".equals(model.engine)) throw new IllegalArgumentException("需要 OPUS 模型");
        toEnglish = "zh-en".equals(model.direction);
        this.root = root;
        modelDirectory = model.directory + "/";
        resourceDirectory = model.resource;
        manifestName = model.manifest;
    }
    public synchronized void prepare() throws IOException { for (String name : new String[]{"encoder_model_quantized.onnx", "decoder_model_quantized.onnx", "tokenizer.json"}) resource(name); }
    public boolean cacheExists() { return Files.isRegularFile(root.resolve(modelDirectory + "tokenizer.json")); }
    public boolean cacheValid() throws IOException {
        Properties hashes = new Properties();
        try (InputStream input = getClass().getResourceAsStream(manifestName)) { if(input == null) return false; hashes.load(input); }
        for (String name : hashes.stringPropertyNames()) { Path file=root.resolve(modelDirectory+name); if(!Files.isRegularFile(file)||!hashes.getProperty(name).equals(ModelStore.digest(file,"SHA-256"))) return false; }
        return hashes.size() == 3;
    }
    private synchronized Path resource(String name) throws IOException {
        Path destination = root.resolve(modelDirectory + name);
        Properties hashes = new Properties();
        try (InputStream manifest = getClass().getResourceAsStream(manifestName)) {
            if (manifest == null) throw new IOException("缺少聊天翻译模型，请使用完整发行 JAR");
            hashes.load(manifest);
        }
        String expected = hashes.getProperty(name);
        if (expected == null) throw new IOException("模型校验清单不完整");
        if (Files.isRegularFile(destination) && expected.equals(ModelStore.digest(destination, "SHA-256"))) return destination;
        Files.createDirectories(destination.getParent());
        Path temporary = destination.resolveSibling(name + ".part");
        try (InputStream input = getClass().getResourceAsStream(resourceDirectory + name)) {
            if (input == null) throw new IOException("缺少聊天翻译模型文件");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("翻译已取消");
                    output.write(buffer, 0, count);
                }
            }
            if (!expected.equals(ModelStore.digest(temporary, "SHA-256"))) throw new IOException("聊天翻译模型校验失败");
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } finally { Files.deleteIfExists(temporary); }
    }
    public synchronized long clearDownloadCache() throws IOException {
        long bytes = 0;
        for (String name : new String[] {"encoder_model_quantized.onnx", "decoder_model_quantized.onnx", "tokenizer.json"}) {
            Path part = root.resolve(modelDirectory + name + ".part");
            if (Files.isRegularFile(part, LinkOption.NOFOLLOW_LINKS)) { bytes += Files.size(part); Files.delete(part); }
        }
        return bytes;
    }
    public synchronized void deleteCachedModel() throws IOException {
        close();
        for (String name : new String[] {"encoder_model_quantized.onnx", "decoder_model_quantized.onnx", "tokenizer.json"}) {
            Files.deleteIfExists(root.resolve(modelDirectory + name));
            Files.deleteIfExists(root.resolve(modelDirectory + name + ".part"));
        }
        tokenizer = null;
    }
    private void load() throws Exception {
        if (encoder != null) return;
        phase = "释放并校验模型";
        Path enc = resource("encoder_model_quantized.onnx"), dec = resource("decoder_model_quantized.onnx"), tokens = resource("tokenizer.json");
        phase = "加载分词器"; tokenizer = new UnigramTokenizer(tokens);
        phase = "加载 ONNX 原生引擎";
        environment = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(2); options.setInterOpNumThreads(1);
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            encoder = environment.createSession(enc.toAbsolutePath().toString(), options);
            try { decoder = environment.createSession(dec.toAbsolutePath().toString(), options); }
            catch (Exception failure) { close(); throw failure; }
        }
    }
    public String translate(String source) throws Exception {
        String greeting = source.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+$", "");
        if (!toEnglish && (greeting.equals("hello") || greeting.equals("hey") || greeting.equals("hi"))) return "你好";
        load();
        if (!inventoried) { io.github.easychat.engine.ModelFileRegistry.at(root).refresh(); inventoried=true; }
        phase = "生成译文";
        long[] encoded = tokenizer.encode(source.replaceAll("(?i)\\bu\\b", "you").replaceAll("(?i)\\bur\\b", "your").replaceAll("(?i)\\b(?:pls|plz)\\b", "please").replaceAll("(?i)\\bthx\\b", "thanks")), attention = new long[encoded.length]; Arrays.fill(attention, 1);
        Map<String, OnnxTensor> inputs = new HashMap<>();
        try (OnnxTensor ids = OnnxTensor.createTensor(environment, new long[][] {encoded});
             OnnxTensor mask = OnnxTensor.createTensor(environment, new long[][] {attention})) {
            inputs.put("input_ids", ids); inputs.put("attention_mask", mask);
            try (OrtSession.Result state = encoder.run(inputs);
                 OnnxTensor hidden = OnnxTensor.createTensor(environment, state.get(0).getValue())) {
                String translated = TranslationOutput.clean(tokenizer.decode(beamSearch(hidden, mask)));
                if (!TranslationOutput.hasContent(translated)) throw new IOException("模型没有生成有效译文");
                return translated;
            }
        }
    }
    private static final class Beam {
        final List<Long> tokens; final double score;
        Beam(List<Long> tokens, double score) { this.tokens = tokens; this.score = score; }
        double normalized() { return score / Math.max(1, tokens.size() - 1); }
    }
    /** Follow the model's four-beam generation setting, rather than greedy decoding. */
    private List<Long> beamSearch(OnnxTensor hidden, OnnxTensor mask) throws Exception {
        List<Beam> active = new ArrayList<>(), finished = new ArrayList<>();
        active.add(new Beam(Collections.singletonList(65000L), 0));
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        for (int step = 0; step < 96 && !active.isEmpty(); step++) {
            List<Beam> candidates = new ArrayList<>();
            for (Beam beam : active) {
                if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline) throw new InterruptedIOException("聊天翻译已取消或超时");
                long[] prefix = new long[beam.tokens.size()]; for (int i = 0; i < prefix.length; i++) prefix[i] = beam.tokens.get(i);
                try (OnnxTensor decoderIds = OnnxTensor.createTensor(environment, new long[][] {prefix})) {
                    Map<String, OnnxTensor> arguments = new HashMap<>();
                    arguments.put("input_ids", decoderIds); arguments.put("encoder_hidden_states", hidden); arguments.put("encoder_attention_mask", mask);
                    try (OrtSession.Result prediction = decoder.run(arguments)) {
                        float[][][] logits = (float[][][]) prediction.get("logits").get().getValue();
                        float[] scores = logits[0][logits[0].length - 1];
                        float max = Float.NEGATIVE_INFINITY;
                        for (float value : scores) max = Math.max(max, value);
                        double sum = 0; for (float value : scores) sum += Math.exp(value - max);
                        double normalizer = max + Math.log(sum);
                        int[] top = new int[8]; Arrays.fill(top, -1);
                        for (int id = 0; id < scores.length - 1; id++) {
                            if (id == 1) continue;
                            for (int rank = 0; rank < top.length; rank++) if (top[rank] < 0 || scores[id] > scores[top[rank]]) {
                                for (int tail = top.length - 1; tail > rank; tail--) top[tail] = top[tail - 1];
                                top[rank] = id; break;
                            }
                        }
                        for (int id : top) if (id >= 0) {
                            List<Long> tokens = new ArrayList<>(beam.tokens); tokens.add((long) id);
                            candidates.add(new Beam(tokens, beam.score + scores[id] - normalizer));
                        }
                    }
                }
            }
            candidates.sort((a, b) -> Double.compare(b.score, a.score));
            active = new ArrayList<>();
            for (int rank = 0; rank < candidates.size(); rank++) {
                Beam candidate = candidates.get(rank);
                if (candidate.tokens.get(candidate.tokens.size() - 1) == 0) {
                    if (rank < 4 && candidate.tokens.size() > 2) finished.add(candidate);
                } else if (active.size() < 4) active.add(candidate);
                if (active.size() == 4 && rank >= 7) break;
            }
            finished.sort((a, b) -> Double.compare(b.normalized(), a.normalized()));
            if (finished.size() > 4) finished.subList(4, finished.size()).clear();
            if (finished.size() == 4 && (active.isEmpty() || active.get(0).score / Math.max(1, step + 1) <= finished.get(3).normalized())) break;
        }
        List<Beam> choices = finished.isEmpty() ? active : finished;
        choices.sort((a, b) -> Double.compare(b.normalized(), a.normalized()));
        if (choices.isEmpty()) throw new IOException("模型没有生成译文");
        return choices.get(0).tokens;
    }
    @Override public void close() {
        try { if (decoder != null) decoder.close(); } catch (OrtException ignored) { }
        try { if (encoder != null) encoder.close(); } catch (OrtException ignored) { }
        encoder = null; decoder = null;
    }
}
