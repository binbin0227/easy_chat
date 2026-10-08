package io.github.easychat.audio;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Java 8's Windows audio backend can expose ANSI names as modified UTF-8. */
public final class DeviceNames {
    private DeviceNames() { }
    public static String display(String raw) {
        if (hasHan(raw)) return raw;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c <= 255) bytes.write(c);
            else {
                byte[] encoded = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                bytes.write(encoded, 0, encoded.length);
            }
        }
        String decoded = new String(bytes.toByteArray(), Charset.forName("GBK"));
        if (!hasHan(decoded)) return raw;
        decoded = decoded.replace("\uFFFD", "");
        if (decoded.startsWith("主声音捕获")) return "默认输入设备";
        if (decoded.contains("(") && !decoded.contains(")")) decoded += ")";
        return decoded;
    }
    private static boolean hasHan(String text) {
        for (int i = 0; i < text.length(); i++)
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) return true;
        return false;
    }
}
