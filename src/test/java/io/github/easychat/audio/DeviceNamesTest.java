package io.github.easychat.audio;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceNamesTest {
    @Test public void recoversMixedLatin1AndUtf8WindowsName() {
        assertEquals("麦克风 (USB Audio)", DeviceNames.display("Âó¿˷ç (USB Audio"));
    }
    @Test public void preservesValidChineseName() { assertEquals("麦克风 (USB Audio)", DeviceNames.display("麦克风 (USB Audio)")); }
    @Test public void preservesAsciiAndLatinNames() {
        assertEquals("USB Camera", DeviceNames.display("USB Camera"));
        assertEquals("Café", DeviceNames.display("Café"));
    }
}
