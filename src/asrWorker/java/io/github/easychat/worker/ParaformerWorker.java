package io.github.easychat.worker;

import com.k2fsa.sherpa.onnx.*;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;

/** Isolated Java 8 CPU worker: native failures and ORT versions cannot affect Minecraft. */
public final class ParaformerWorker {
    public static final int READY = 0x50415241;
    public static void main(String[] args) throws Exception {
        DataOutputStream protocol = new DataOutputStream(System.out);
        System.setOut(new PrintStream(System.err, true, "UTF-8"));
        Path runtime = Paths.get(args[2]);
        Files.createDirectories(runtime);
        // Load our exact DLLs from a stable, owned runtime directory, never global PATH.
        for (String name : new String[]{"onnxruntime.dll", "sherpa-onnx-jni.dll"}) {
            Path file = runtime.resolve(name);
            try (InputStream input = ParaformerWorker.class.getResourceAsStream("/sherpa-onnx/native/win-x64/" + name)) {
                if (input == null) throw new IOException("Paraformer 缺少 " + name);
                ByteArrayOutputStream content = new ByteArrayOutputStream(); byte[] buffer = new byte[65536]; int n;
                while ((n = input.read(buffer)) != -1) content.write(buffer, 0, n);
                byte[] expected = content.toByteArray();
                if (!Files.isRegularFile(file) || !java.util.Arrays.equals(expected, Files.readAllBytes(file))) {
                    Path temporary = file.resolveSibling(name + ".part");
                    try { Files.write(temporary, expected); Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
                    finally { Files.deleteIfExists(temporary); }
                }
            }
            System.load(file.toAbsolutePath().toString());
        }
        LibraryLoader.setAutoLoadEnabled(false);
        OfflineModelConfig models = OfflineModelConfig.builder()
                .setParaformer(OfflineParaformerModelConfig.builder().setModel(args[0]).build())
                .setTokens(args[1]).setNumThreads(4).setProvider("cpu").build();
        OfflineRecognizer recognizer = new OfflineRecognizer(OfflineRecognizerConfig.builder()
                .setOfflineModelConfig(models)
                .setFeatureConfig(FeatureConfig.builder().setSampleRate(16000).setFeatureDim(80).build())
                .setDecodingMethod("greedy_search").build());
        try {
            protocol.writeInt(READY); protocol.flush();
            DataInputStream input = new DataInputStream(System.in);
            for (;;) {
                int size;
                try { size = input.readInt(); } catch (EOFException done) { break; }
                if (size <= 0 || size > 2 * 1024 * 1024) throw new IOException("音频大小无效");
                byte[] wav = new byte[size]; input.readFully(wav);
                try { reply(protocol, 0, decode(recognizer, wav)); }
                catch (Exception error) { reply(protocol, 1, error.getMessage() == null ? "语音识别失败" : error.getMessage()); }
            }
        } finally { recognizer.release(); }
    }
    private static String decode(OfflineRecognizer recognizer, byte[] wav) throws Exception {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            AudioFormat format = audio.getFormat();
            if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.isBigEndian()
                    || format.getSampleRate() != 16000 || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding()))
                throw new IOException("识别需要 16kHz 单声道 PCM 音频");
            ByteArrayOutputStream pcm = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
            while ((n = audio.read(buffer)) != -1) pcm.write(buffer, 0, n);
            byte[] bytes = pcm.toByteArray(); float[] samples = new float[bytes.length / 2];
            for (int i = 0; i < samples.length; i++) samples[i] = (short)((bytes[i * 2] & 255) | (bytes[i * 2 + 1] << 8)) / 32768f;
            OfflineStream stream = recognizer.createStream();
            try { stream.acceptWaveform(samples, 16000); recognizer.decode(stream); return recognizer.getResult(stream).getText(); }
            finally { stream.release(); }
        }
    }
    private static void reply(DataOutputStream output, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        output.writeInt(status); output.writeInt(bytes.length); output.write(bytes); output.flush();
    }
}
