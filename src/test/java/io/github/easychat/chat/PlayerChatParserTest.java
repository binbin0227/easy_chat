package io.github.easychat.chat;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class PlayerChatParserTest {
    private final Set<String> players = new HashSet<>(Arrays.asList("alex", "bob"));
    @Test public void recognizesDefaultRankScreenshotDuringTabTransfer() {
        PlayerChatParser.Message message = PlayerChatParser.parse("§7StingerC70§7: i like dudes as a dude", Collections.emptySet());
        assertNotNull(message);
        assertEquals("StingerC70", message.sender);
        assertEquals("i like dudes as a dude", message.text);
        assertNotNull(PlayerChatParser.parse("§r§7StingerC70§r§7: hello§r", Collections.emptySet()));
        assertNotNull(PlayerChatParser.parse("§7StingerC70: hello§r", Collections.emptySet()));
        assertNotNull(PlayerChatParser.parse(new net.minecraft.util.ChatComponentText("§7StingerC70§7: hello").getFormattedText(), Collections.emptySet()));
        for(String notice:new String[]{"Unknown: hi", "§7Server§7: Restarting soon", "§7TIP§7: Buy ranks", "§7[NPC] Dave: hello", "§7游戏将在4秒后开始!"})
            assertNull(notice, PlayerChatParser.parse(notice, Collections.emptySet()));
    }
    @Test public void recognizesScreenshotTeamMessageAndEmbeddedLineBreaks() {
        String prefix = "§e§7[6✫]§r §e[黄队] §7PC_Is_Lagging§7: ";
        String body = "Christ_isGod tuffffffffffffffffff name bro";
        PlayerChatParser.Message message = PlayerChatParser.parse(prefix + body, Collections.emptySet());
        assertNotNull(message);
        assertEquals("PC_Is_Lagging", message.sender);
        assertEquals(body, message.text);
        assertEquals(body, PlayerChatParser.parse(prefix + "Christ_isGod\n  tuffffffffffffffffff name bro", Collections.emptySet()).text);
    }
    @Test public void recognizesPublicPartyGuildAndPrivatePlayerMessages() {
        for (String text : new String[] {"Alex: defend the bed", "[MVP+] Bob: help me", "Party > [VIP] Remote: let's go", "Guild > Remote: hello", "From Remote: thanks", "[SHOUT] [RED] Alex: incoming", "[123✫] [MVP++] Bob: defend the bed", "[5⋆] Alex: hello"}) {
            assertNotNull(text, PlayerChatParser.parse(text, players));
        }
        assertEquals("help me", PlayerChatParser.parse("§b[MVP+] §fBob§7: help me", players).text);
    }
    @Test public void excludesNoticesSystemLikeNamesAndChinese() {
        for (String text : new String[] {"BED DESTRUCTION > Your bed was destroyed!", "Server: Restarting in 30 seconds", "TIP: Buy ranks now", "[ANNOUNCEMENT] News: Welcome", "Party > System: Welcome", "To Bob: hello", "Alex: 你好", "Random: server announcement", "Random: Buy [VIP] today"}) {
            assertNull(text, PlayerChatParser.parse(text, players));
        }
        assertNotNull(PlayerChatParser.parse("[VIP] Bob: hi", players));
        assertNotNull(PlayerChatParser.parse("<Alex> hello", players));
    }
    @Test public void recognizesActualProxyConnectionMessageWithoutHostnameOrTabEntry() {
        // Prefix reconstructed from the rejected codepoints in the actual Lunar log.
        String prefix = "[20\u272b] [\u9ec4\u961f] __5H4D0W__";
        PlayerChatParser.Message message = PlayerChatParser.parse(prefix + ": drop iron i buy def", Collections.emptySet());
        assertNotNull(message);
        assertEquals("__5H4D0W__", message.sender);
        assertEquals("drop iron i buy def", message.text);
        for (String notice : new String[] {"[MVP++] jobaso正在广施慷慨!", "+15 Bed Wars XP (Time Played)", "[20✫] Server: Buy ranks now", "Unknown: Buy ranks now", "游戏将在1秒后开始!"})
            assertNull(notice, PlayerChatParser.parse(notice, Collections.emptySet()));
    }
    @Test public void recognizesLocalizedBedWarsTeamAfterLevelButNotServerNotices() {
        Set<String> tab = new HashSet<>(Arrays.asList("yoimiyayyds", "sport4589", "k1ll3r_k4ktuz"));
        String[] messages = {
                "[14✫] [红队] [VIP] YoimiyaYYDS: hello",
                "[11✫] [红队] Sport4589: hey",
                "[361✫] [红队] K1ll3r_K4ktuz: hi",
                "[11✫] [RED] Sport4589: hey",
                "[红队] [11✫] Sport4589: hey",
                "[VIP] [14✫] [红队] YoimiyaYYDS: hello"};
        for (String text : messages) assertNotNull(text, PlayerChatParser.parse(text, tab));
        assertEquals("hello", PlayerChatParser.parse("§7[14✫] §c[红队] §a[VIP] YoimiyaYYDS§f: hello", tab).text);
        for (String notice : new String[] {"床已被破坏 > Blue的床成为了winexp破坏的第#3,146张床！", "Sport4589失足跌入虚空。", "winexp被Dinnerbone5467击杀。", "+15 Bed Wars XP (Time Played)", "[通知] Server: hello", "[红队] Stranger: hello"})
            assertNull(notice, PlayerChatParser.parse(notice, tab));
    }
    @Test public void recognizesScreenshotLobbySendersMissingFromTabAndKeepsNotificationsUntouched() {
        Set<String> empty = Collections.emptySet();
        for(String line:new String[]{"[17✫] tygios: toi invite moi","[4✫] Jacketal: hi","[15✩] [VIP] YoimiyaYYDS: HI","  §7[17✫]§r\u00a0§7tygios§7: allo"})
            assertNotNull(line,PlayerChatParser.parse(line,empty));
        for(String line:new String[]{"[MVP+] Haycko潜入了大厅!"," >>> [MVP++] Salale潜入了大厅! <<<","Click to view the stats of your Bed Wars game!","[17✫] Server: Restarting soon","Unknown: hi","[17✫] Bonus awarded!","[NPC] Dave: hello"})
            assertNull(line,PlayerChatParser.parse(line,empty));
        assertNotNull(PlayerChatParser.parse("[17✫] tygios: hi",empty));
    }
}
