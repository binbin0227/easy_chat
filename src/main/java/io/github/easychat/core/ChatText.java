package io.github.easychat.core;

public final class ChatText {
    private ChatText() { }
    public static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{Cntrl}§]", " ").replaceAll("\\s+", " ").trim();
    }
    public static void validate(String text) {
        if (text.isEmpty()) throw new IllegalArgumentException("没有识别到可发送的内容");
        if (text.startsWith("/")) throw new IllegalArgumentException("结果以 / 开头，已阻止作为命令发送");
        if (text.length() > 100) throw new IllegalArgumentException("译文超过 1.8.9 聊天的 100 字符限制，请缩短句子");
    }
}
