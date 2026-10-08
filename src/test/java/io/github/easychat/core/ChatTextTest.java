package io.github.easychat.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChatTextTest {
    @Test public void speechIsOneChatLine() {
        assertEquals("Anyone want to team up?", ChatText.normalize("  Anyone\nwant\r\nto team up?  "));
    }
    @Test(expected = IllegalArgumentException.class) public void neverSendsCommand() { ChatText.validate(ChatText.normalize("\n /op someone")); }
    @Test(expected = IllegalArgumentException.class) public void neverSilentlyTruncatesLongMessage() {
        ChatText.validate(new String(new char[101]).replace('\0', 'a'));
    }
    @Test(expected = IllegalArgumentException.class) public void neverSendsEmptyMessage() { ChatText.validate(ChatText.normalize("\r\n")); }
    @Test public void acceptsVanillaMaximumLength() { ChatText.validate(new String(new char[100]).replace('\0', 'a')); }
}
