package io.github.easychat.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class ChatSettings {
    public enum SendMode { AUTO, DRAFT }
    public enum HintMode { TOP_LEFT, CHAT, HIDDEN }
    public volatile String speechTextModel = "opus-zh-en";
    public volatile String incomingTextModel = "opus-en-zh";
    public volatile io.github.easychat.download.DownloadSources.Source voiceDownloadSource = io.github.easychat.download.DownloadSources.Source.OFFICIAL;
    public volatile io.github.easychat.download.DownloadSources.Source textDownloadSource = io.github.easychat.download.DownloadSources.Source.OFFICIAL;
    public volatile String microphone = "";
    public volatile String model = "paraformer-zh-int8";
    public volatile SendMode sendMode = SendMode.AUTO;
    public volatile HintMode hintMode = HintMode.TOP_LEFT;
    public volatile boolean translateIncoming = true;
    public int talkKey = 47; // LWJGL V
    public int settingsKey = 25; // LWJGL P
    private final Path path;

    public ChatSettings(Path path) { this.path = path; }

    public void load() throws IOException {
        if (!Files.exists(path)) return;
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, java.nio.charset.StandardCharsets.UTF_8)) { p.load(reader); }
        voiceDownloadSource = io.github.easychat.download.DownloadSources.Source.parse(p.getProperty("voiceDownloadSource", "OFFICIAL"));
        textDownloadSource = io.github.easychat.download.DownloadSources.Source.parse(p.getProperty("textDownloadSource", "OFFICIAL"));
        microphone = p.getProperty("microphone", "");
        String selected = p.getProperty("model", "paraformer-zh-int8");
        model = selected.isEmpty() ? "" : io.github.easychat.engine.ModelStore.Model.from(selected).id;
        sendMode = "DRAFT".equals(p.getProperty("sendMode")) ? SendMode.DRAFT : SendMode.AUTO;
        try { hintMode = HintMode.valueOf(p.getProperty("hintMode", "TOP_LEFT")); }
        catch (IllegalArgumentException invalid) { hintMode = HintMode.TOP_LEFT; }
        translateIncoming = !"false".equals(p.getProperty("translateIncoming", "true"));
        String legacyTextModel = p.getProperty("textModel", "opus");
        speechTextModel = directionModel(p.getProperty("speechTextModel", "opus-zh-en"), true);
        incomingTextModel = directionModel(p.getProperty("incomingTextModel", legacyTextModel), false);
        talkKey = key(p.getProperty("talkKey"), 47);
        settingsKey = key(p.getProperty("settingsKey"), 25);
    }

    private int key(String value, int fallback) {
        try { int k = Integer.parseInt(value); return k >= -100 && k < 256 ? k : fallback; }
        catch (Exception e) { return fallback; }
    }
    private String directionModel(String value, boolean speech) {
        if (value.isEmpty()) return value;
        io.github.easychat.chat.TextModelCatalog.Model selected = io.github.easychat.chat.TextModelCatalog.resolve(value, speech);
        return selected != null && selected.supports(speech) ? selected.id : speech ? "opus-zh-en" : "opus-en-zh";
    }

    public synchronized void save() throws IOException {
        Files.createDirectories(path.getParent());
        Properties p = new Properties();
        p.setProperty("voiceDownloadSource", voiceDownloadSource.name());
        p.setProperty("textDownloadSource", textDownloadSource.name());
        p.setProperty("microphone", microphone);
        p.setProperty("model", model);
        p.setProperty("sendMode", sendMode.name());
        p.setProperty("hintMode", hintMode.name());
        p.setProperty("translateIncoming", Boolean.toString(translateIncoming));
        p.setProperty("speechTextModel", speechTextModel);
        p.setProperty("incomingTextModel", incomingTextModel);
        p.setProperty("talkKey", Integer.toString(talkKey));
        p.setProperty("settingsKey", Integer.toString(settingsKey));
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, java.nio.charset.StandardCharsets.UTF_8)) { p.store(writer, "Easy Chat"); }
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }
}
