package io.github.easychat.engine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Persistent, isolated Java CPU recognizer with bounded reads and hard cancellation. */
public final class ParaformerEngine implements AutoCloseable {
    private final Path root;
    private final ModelStore models;
    private volatile Process process;
    private DataInputStream input;
    private DataOutputStream output;
    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "easychat-asr-protocol"); t.setDaemon(true); return t;
    });
    public ParaformerEngine(Path root, ModelStore models) { this.root = root; this.models = models; }
    private Path prepareRuntime() throws IOException {
        if (!System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win") || !System.getProperty("os.arch").contains("64"))
            throw new IOException("本地语音识别支持 Windows 64 位");
        Path runtime = root.resolve("runtime/paraformer-1.13.8"); Files.createDirectories(runtime);
        Path jar = runtime.resolve("paraformer-worker.jar");
        try (InputStream stream = getClass().getResourceAsStream("/easychat/paraformer-worker.jar")) {
            if (stream == null) throw new IOException("模组缺少 Paraformer 引擎，请使用完整 JAR");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[65536]; int n;
            while ((n = stream.read(buffer)) != -1) bytes.write(buffer, 0, n);
            byte[] expected = bytes.toByteArray();
            if (!Files.isRegularFile(jar) || !Arrays.equals(expected, Files.readAllBytes(jar))) {
                Path temporary = jar.resolveSibling("paraformer-worker.jar.part");
                try { Files.write(temporary, expected); Files.move(temporary, jar, StandardCopyOption.REPLACE_EXISTING); }
                finally { Files.deleteIfExists(temporary); }
            }
        }
        return jar;
    }
    public void ensureStarted(ModelStore.Model model, Consumer<String> progress) throws IOException {
        Process current = process; if (current != null && current.isAlive()) return;
        close(); models.prepare(model, progress);
        Path jar = prepareRuntime();
        Path java = Paths.get(System.getProperty("java.home"), "bin", "java.exe");
        if (!Files.isRegularFile(java)) throw new IOException("找不到当前游戏的 Java 运行时");
        progress.accept("正在加载 Paraformer 中文识别模型");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-Xms32m", "-Xmx256m", "-jar", jar.toAbsolutePath().toString(),
                models.file(model).toAbsolutePath().toString(), models.tokensFile(model).toAbsolutePath().toString(), jar.getParent().resolve("native").toAbsolutePath().toString());
        builder.redirectError(ProcessBuilder.Redirect.appendTo(root.resolve("paraformer-engine.log").toFile()));
        process = builder.start(); input = new DataInputStream(process.getInputStream()); output = new DataOutputStream(process.getOutputStream());
        final DataInputStream channel = input;
        try { if (await(reader.submit(channel::readInt), 90) != 0x50415241) throw new IOException("语音引擎启动响应无效"); }
        catch (IOException failure) { close(); throw failure; }
    }
    public String transcribe(byte[] wav) throws IOException {
        Process current = process;
        if (current == null || !current.isAlive()) throw new IOException("语音引擎未启动，请重试");
        final DataInputStream channel = input;
        try {
            output.writeInt(wav.length); output.write(wav); output.flush();
            return await(reader.submit(() -> {
                int status = channel.readInt(), length = channel.readInt();
                if (length < 0 || length > 262144) throw new IOException("语音引擎响应大小异常");
                byte[] bytes = new byte[length]; channel.readFully(bytes);
                String text = new String(bytes, StandardCharsets.UTF_8);
                if (status != 0) throw new IOException(text); return text;
            }), 30);
        } catch (IOException failure) { close(); throw failure; }
    }
    private <T> T await(Future<T> task, int seconds) throws IOException {
        try { return task.get(seconds, TimeUnit.SECONDS); }
        catch (InterruptedException cancelled) { task.cancel(true); Thread.currentThread().interrupt(); throw new InterruptedIOException("识别已取消"); }
        catch (TimeoutException timeout) { task.cancel(true); throw new IOException("Paraformer 识别超时，请重试"); }
        catch (ExecutionException failure) { throw new IOException("Paraformer 引擎失败：" + failure.getCause().getMessage() + "（详见 paraformer-engine.log）", failure.getCause()); }
    }
    @Override public void close() {
        Process current = process; process = null;
        if (current != null) { current.destroy(); if (current.isAlive()) current.destroyForcibly(); }
    }
    public void shutdown() { close(); reader.shutdownNow(); }
}
