package io.github.easychat.audio;

import org.junit.Test;
import javax.sound.sampled.*;
import java.io.*;
import static org.junit.Assert.*;

public class AudioCaptureTest {
    @Test public void convertsStereo48kToMono16kWav() throws Exception {
        byte[] pcm = new byte[48000 * 4];
        for (int i = 0; i < 48000; i++) {
            short left = (short) (12000 * Math.sin(2 * Math.PI * 440 * i / 48000));
            short right = (short) (6000 * Math.sin(2 * Math.PI * 440 * i / 48000));
            pcm[i * 4] = (byte) left; pcm[i * 4 + 1] = (byte) (left >> 8);
            pcm[i * 4 + 2] = (byte) right; pcm[i * 4 + 3] = (byte) (right >> 8);
        }
        AudioCapture.Clip clip = AudioCapture.convert(pcm, new AudioFormat(48000, 16, 2, true, false));
        assertEquals(1.0, clip.seconds, 0.0001);
        assertTrue(clip.rms > 0.18 && clip.rms < 0.20);
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(clip.wav))) {
            assertEquals(16000, audio.getFormat().getSampleRate(), 0);
            assertEquals(1, audio.getFormat().getChannels());
            assertEquals(16000, audio.getFrameLength());
        }
    }
    @Test public void silenceHasZeroEnergy() throws Exception {
        AudioCapture.Clip clip = AudioCapture.convert(new byte[32000], new AudioFormat(16000, 16, 1, true, false));
        assertEquals(0, clip.rms, 0);
        assertEquals(1, clip.seconds, 0);
    }
    @Test public void emptyRecordingProducesValidEmptyWav() throws Exception {
        AudioCapture.Clip clip = AudioCapture.convert(new byte[0], new AudioFormat(48000, 16, 2, true, false));
        assertEquals(0, clip.seconds, 0);
        assertEquals(0, clip.rms, 0);
        assertTrue(clip.wav.length >= 44);
    }
}
