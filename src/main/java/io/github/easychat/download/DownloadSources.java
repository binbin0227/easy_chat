package io.github.easychat.download;

import java.net.*;
import java.util.*;

/** Fixed, verified source mappings. Every source uses the existing model digest. */
public final class DownloadSources {
    public enum Source {
        OFFICIAL("Hugging Face 官方"), MIRROR("HF-Mirror 镜像"), MODELSCOPE("ModelScope 魔搭");
        public final String label;
        Source(String label) { this.label = label; }
        public static Source parse(String value) { try { return valueOf(value); } catch (Exception invalid) { return OFFICIAL; } }
    }
    public static final class Link {
        public final Source source; public final String url;
        Link(Source source, String url) { this.source = source; this.url = url; }
    }
    public static List<Link> links(String voiceId, String repository, String revision, String file) {
        String path = voiceId != null ? "/csukuangfj/sherpa-onnx-paraformer-zh-2024-03-09/resolve/main/model.int8.onnx" : "/" + repository + "/resolve/" + revision + "/" + file;
        List<Link> result = new ArrayList<>();
        result.add(new Link(Source.OFFICIAL, "https://huggingface.co" + path));
        result.add(new Link(Source.MIRROR, "https://hf-mirror.com" + path));
        return Collections.unmodifiableList(result);
    }
    public static List<Link> selected(List<Link> links, Source source) throws java.io.IOException {
        for (Link link : links) if (link.source == source) return Collections.singletonList(link);
        throw new java.io.IOException("此模型不支持所选下载源，请重新选择");
    }
    public static String speed(long bytes, long started) {
        double seconds = Math.max(.001, (System.nanoTime() - started) / 1e9);
        return String.format(Locale.ROOT, "%.2f MiB/s", bytes / 1048576.0 / seconds);
    }
    private DownloadSources() { }
}
