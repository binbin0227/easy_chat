package io.github.easychat.chat;
import java.util.*;
import java.util.regex.*;

/** Conservative Hypixel format recognition. Unknown formats are left untouched. */
public final class PlayerChatParser {
    public static final class Message {
        public final String sender, text;
        public Message(String sender, String text) { this.sender = sender; this.text = text; }
    }
    private static final String NAME = "([A-Za-z0-9_]{1,16})";
    private static final String RANK = "(?:(?<rank>\\[(?:VIP\\+?|MVP\\+{0,2}|YOUTUBE|ADMIN|GM|MOD|HELPER|PIG\\+{0,3})\\])\\s*)?";
    private static final String TEAM = "\\[(?:RED|BLUE|GREEN|YELLOW|AQUA|WHITE|PINK|GRAY|R|B|G|Y|A|W|P|S|红队|蓝队|绿队|黄队|青队|白队|粉队|灰队|红|蓝|绿|黄|青|白|粉|灰)\\]\\s*";
    private static final String LEVEL = "\\[\\d{1,5}[✫✪✯✰✵✶✷✸✹✺✻✼★☆⋆⚝✩✧✦✥❂❃❁]\\]\\s*";
    private static final Pattern LEVEL_MARKER = Pattern.compile(LEVEL);
    private static final String PREFIX = "(?:" + TEAM + "|" + LEVEL + ")";
    private static final Pattern HYPIXEL = Pattern.compile("^(?:(?<channel>(?:Party|Guild|Officer|Co-op|Team|队伍|组队|公会|官员|协作) > |(?:From|来自) |\\[(?:TEAM|SHOUT|队伍|喊话)\\]\\s*))?" + PREFIX + "{0,3}" + RANK + PREFIX + "{0,2}(?<sender>[A-Za-z0-9_]{1,16})(?:\\s*\\[[A-Za-z0-9_ +*-]{1,16}\\])?\\s*: (?<body>.+)$");
    private static final Pattern VANILLA = Pattern.compile("^<" + NAME + "> (.+)$");
    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList("server", "system", "notice", "announcement", "tip", "info", "warning"));
    // Proxies and alternate connection addresses do not identify the game server.
    // Recognize the complete player format, never gate on the connection hostname.
    public static Message parse(String formatted, Set<String> players) {
        String text = formatted.replaceAll("§.", "").replace('\u00a0', ' ').replace('\u202f', ' ').replaceAll("[\\r\\n]+\\s*", " ").trim();
        Matcher vanilla = VANILLA.matcher(text);
        if (vanilla.matches() && players.contains(vanilla.group(1).toLowerCase(Locale.ROOT))) return eligible(vanilla.group(1), vanilla.group(2));
        Matcher match = HYPIXEL.matcher(text);
        if (!match.matches()) return null;
        String sender = match.group("sender");
        boolean channel = match.group("channel") != null;
        boolean rank = match.group("rank") != null;
        // Hypixel lobby tabs can omit senders. A recognized Bed Wars level is also
        // player-format evidence; notices still have to match the full chat grammar.
        boolean level = LEVEL_MARKER.matcher(text.substring(0, match.start("sender"))).find();
        if (!channel && !rank && !level && !players.contains(sender.toLowerCase(Locale.ROOT))) return null;
        return eligible(sender, match.group("body"));
    }
    public static Message eligible(String sender, String body) {
        String text = body.replaceAll("§.", "").trim();
        if (RESERVED.contains(sender.toLowerCase(Locale.ROOT)) || text.length() > 256 || !text.matches(".*[A-Za-z].*") || text.matches(".*[\\p{IsHan}].*")) return null;
        return new Message(sender, text);
    }
}
