package io.github.easychat.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class AudioCapture {
    public static final int MAX_SECONDS = 15;
    public static final class Device {
        public final String id, name;
        private final Mixer.Info info;
        Device(String id, String name, Mixer.Info info) { this.id = id; this.name = name; this.info = info; }
    }
    public static final class Clip {
        public final byte[] wav;
        public final double seconds, rms;
        Clip(byte[] wav, double seconds, double rms) { this.wav = wav; this.seconds = seconds; this.rms = rms; }
    }
    public static List<Device> devices() {
        List<Device> result = new ArrayList<>();
        result.add(new Device("", "系统默认麦克风", null));
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                for (Line.Info line : mixer.getTargetLineInfo()) {
                    if (TargetDataLine.class.isAssignableFrom(line.getLineClass())) {
                        String name = info.getName();
                        String nativeEncoding = System.getProperty("sun.jnu.encoding", java.nio.charset.Charset.defaultCharset().name());
                        if (System.getProperty("os.name").startsWith("Windows") && System.getProperty("java.version").startsWith("1.8")
                                && nativeEncoding.matches("(?i)GBK|MS936|GB18030"))
                            name = DeviceNames.display(name);
                        result.add(new Device(info.getName() + "|" + info.getVendor() + "|" + info.getDescription(), name, info));
                        break;
                    }
                }
            } catch (Exception ignored) { }
        }
        return result;
    }
    public static final class Session {
        public final CompletableFuture<Clip> finished = new CompletableFuture<>();
        public volatile float level;
        public volatile String status = "正在打开麦克风";
        private volatile boolean stopping;
        public void stop() { stopping = true; }
    }
    public Session start(String deviceId, boolean keepAudio) {
        Session session = new Session();
        Thread thread = new Thread(() -> capture(session, deviceId, keepAudio), "easychat-microphone");
        thread.setDaemon(true);
        thread.start();
        return session;
    }
    private void capture(Session session, String deviceId, boolean keepAudio) {
        TargetDataLine line = null;
        try {
            Device selected = null;
            for (Device d : devices()) if (d.id.equals(deviceId)) { selected = d; break; }
            if (selected == null) throw new IOException("所选麦克风已断开，请重新选择");
            AudioFormat format = null;
            for (int rate : new int[] {16000, 48000, 44100}) {
                for (int channels : new int[] {1, 2}) {
                    AudioFormat candidate = new AudioFormat(rate, 16, channels, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, candidate);
                    try {
                        line = (TargetDataLine) (selected.info == null ? AudioSystem.getLine(info) : AudioSystem.getMixer(selected.info).getLine(info));
                        line.open(candidate);
                        format = candidate;
                        break;
                    } catch (Exception unavailable) {
                        if (line != null) line.close();
                        line = null;
                    }
                }
                if (line != null) break;
            }
            if (line == null || format == null) throw new IOException("麦克风不可用，请检查设备、占用情况和系统麦克风权限");
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesPerFrame = format.getFrameSize();
            long maxBytes = (long) format.getFrameRate() * bytesPerFrame * MAX_SECONDS;
            long received = 0;
            line.start();
            session.status = "正在采集声音";
            while (!session.stopping && (!keepAudio || received < maxBytes)) {
                int n = Math.min(buffer.length, line.available());
                if (keepAudio) n = (int) Math.min(n, maxBytes - received);
                n -= n % bytesPerFrame;
                if (n == 0) { Thread.sleep(10); continue; }
                n = line.read(buffer, 0, n);
                if (n <= 0) continue;
                double energy = 0;
                for (int i = 0; i + 1 < n; i += 2) {
                    double sample = (short) ((buffer[i] & 255) | (buffer[i + 1] << 8)) / 32768.0;
                    energy += sample * sample;
                }
                session.level = (float) Math.sqrt(energy / (n / 2));
                if (keepAudio) pcm.write(buffer, 0, n);
                received += n;
            }
            line.stop();
            session.level = 0;
            session.finished.complete(keepAudio ? convert(pcm.toByteArray(), format) : new Clip(new byte[0], 0, 0));
        } catch (Exception error) {
            session.status = error.getMessage() == null ? "麦克风测试失败" : error.getMessage();
            session.finished.completeExceptionally(error);
        } finally { if (line != null) line.close(); }
    }
    public static Clip convert(byte[] input, AudioFormat format) throws IOException {
        int channels = format.getChannels();
        int frames = input.length / (2 * channels);
        int samples = (int) (frames * 16000.0 / format.getSampleRate());
        byte[] pcm = new byte[samples * 2];
        double energy = 0;
        for (int i = 0; i < samples; i++) {
            double position = i * format.getSampleRate() / 16000.0;
            int a = Math.min((int) position, frames - 1), b = Math.min(a + 1, frames - 1);
            double fraction = position - a, value = 0;
            for (int ch = 0; ch < channels; ch++) {
                int ia = (a * channels + ch) * 2, ib = (b * channels + ch) * 2;
                short sa = (short) ((input[ia] & 255) | (input[ia + 1] << 8));
                short sb = (short) ((input[ib] & 255) | (input[ib + 1] << 8));
                value += sa + (sb - sa) * fraction;
            }
            short sample = (short) Math.round(value / channels);
            pcm[i * 2] = (byte) sample;
            pcm[i * 2 + 1] = (byte) (sample >> 8);
            energy += (sample / 32768.0) * (sample / 32768.0);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AudioFormat target = new AudioFormat(16000, 16, 1, true, false);
        try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(pcm), target, samples)) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, out);
        }
        return new Clip(out.toByteArray(), samples / 16000.0, samples == 0 ? 0 : Math.sqrt(energy / samples));
    }
}
