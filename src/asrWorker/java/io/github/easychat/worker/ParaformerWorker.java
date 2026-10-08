package io.github.easychat.worker;

import com.sun.jna.*;
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
        System.setProperty("jna.boot.library.path", runtime.toAbsolutePath().toString());
        System.setProperty("jna.nosys", "true");
        System.setProperty("jna.nounpack", "true");
        Files.deleteIfExists(runtime.resolve("sherpa-onnx-jni.dll")); // Owned retired TTS-enabled runtime.
        // Load our exact DLLs from a stable, owned runtime directory, never global PATH.
        for (String name : new String[]{"jnidispatch.dll", "onnxruntime_providers_shared.dll", "onnxruntime.dll", "sherpa-onnx-c-api.dll"}) {
            Path file = runtime.resolve(name);
            String resource = name.equals("jnidispatch.dll") ? "/com/sun/jna/win32-x86-64/jnidispatch.dll" : "/easychat/native/win-x64/" + name;
            try (InputStream input = ParaformerWorker.class.getResourceAsStream(resource)) {
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
            if (!name.equals("jnidispatch.dll")) NativeLibrary.getInstance(file.toAbsolutePath().toString());
        }
        SherpaAsr.Api api = Native.load(runtime.resolve("sherpa-onnx-c-api.dll").toAbsolutePath().toString(),
                SherpaAsr.Api.class, java.util.Collections.singletonMap(Library.OPTION_STRING_ENCODING, "UTF-8"));
        System.err.println("EasyChat ASR-only: sherpa " + api.SherpaOnnxGetVersionStr()
                + ", ONNX Runtime " + api.SherpaOnnxGetOnnxruntimeVersionStr());
        SherpaAsr.SherpaOnnxOfflineRecognizerConfig config = new SherpaAsr.SherpaOnnxOfflineRecognizerConfig();
        config.feat_config.sample_rate = 16000; config.feat_config.feature_dim = 80;
        config.model_config.paraformer.model = args[0]; config.model_config.tokens = args[1];
        config.model_config.num_threads = 4; config.model_config.provider = "cpu";
        config.model_config.model_type = "paraformer"; config.decoding_method = "greedy_search";
        config.max_active_paths = 4; config.hotwords_score = 1.5f;
        Pointer recognizer = api.SherpaOnnxCreateOfflineRecognizer(config);
        if (recognizer == null) throw new IOException("Paraformer 模型初始化失败");
        try {
            protocol.writeInt(READY); protocol.flush();
            DataInputStream input = new DataInputStream(System.in);
            for (;;) {
                int size;
                try { size = input.readInt(); } catch (EOFException done) { break; }
                if (size <= 0 || size > 2 * 1024 * 1024) throw new IOException("音频大小无效");
                byte[] wav = new byte[size]; input.readFully(wav);
                try { reply(protocol, 0, decode(api, recognizer, wav)); }
                catch (Exception error) { reply(protocol, 1, error.getMessage() == null ? "语音识别失败" : error.getMessage()); }
            }
        } finally { api.SherpaOnnxDestroyOfflineRecognizer(recognizer); }
    }
    private static String decode(SherpaAsr.Api api, Pointer recognizer, byte[] wav) throws Exception {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            AudioFormat format = audio.getFormat();
            if (format.getChannels() != 1 || format.getSampleSizeInBits() != 16 || format.isBigEndian()
                    || format.getSampleRate() != 16000 || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding()))
                throw new IOException("识别需要 16kHz 单声道 PCM 音频");
            ByteArrayOutputStream pcm = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
            while ((n = audio.read(buffer)) != -1) pcm.write(buffer, 0, n);
            byte[] bytes = pcm.toByteArray(); float[] samples = new float[bytes.length / 2];
            for (int i = 0; i < samples.length; i++) samples[i] = (short)((bytes[i * 2] & 255) | (bytes[i * 2 + 1] << 8)) / 32768f;
            Pointer stream = api.SherpaOnnxCreateOfflineStream(recognizer);
            if (stream == null) throw new IOException("无法创建识别输入流");
            try {
                api.SherpaOnnxAcceptWaveformOffline(stream, 16000, samples, samples.length);
                api.SherpaOnnxDecodeOfflineStream(recognizer, stream);
                Pointer result = api.SherpaOnnxGetOfflineStreamResult(stream);
                if (result == null) throw new IOException("识别结果为空");
                try {
                    Pointer text = result.getPointer(0); // First member is const char *text.
                    return text == null ? "" : text.getString(0, "UTF-8").trim();
                } finally { api.SherpaOnnxDestroyOfflineRecognizerResult(result); }
            } finally { api.SherpaOnnxDestroyOfflineStream(stream); }
        }
    }
    private static void reply(DataOutputStream output, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        output.writeInt(status); output.writeInt(bytes.length); output.write(bytes); output.flush();
    }
}
