package io.github.easychat.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class StatusHintTest {
    @Test public void expiresAtFiveSecondsAndNewEventsHaveTheirOwnLifetime() {
        StatusHint first = new StatusHint("录音中", 1000, true);
        assertTrue(first.visible(5999));
        assertFalse(first.visible(6000));
        StatusHint next = new StatusHint("已发送", 7000, true);
        assertTrue(next.visible(7000));
        assertFalse(next.visible(12000));
        assertFalse(first.visible(7000));
    }
}
