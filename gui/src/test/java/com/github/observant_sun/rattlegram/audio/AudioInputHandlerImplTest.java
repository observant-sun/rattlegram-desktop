package com.github.observant_sun.rattlegram.audio;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import static org.mockito.Mockito.*;

class AudioInputHandlerImplTest {

    private final int sampleRate = 44100;
    private final int channelCount = 2;
    int sampleSizeInBits = 16; // 2 bytes
    boolean signed = true;
    boolean bigEndian = false;
    AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channelCount, signed, bigEndian);

    AudioInputHandlerImpl audioInputHandler = spy(new AudioInputHandlerImpl(sampleRate, channelCount, null));

    TargetDataLine line = mock(TargetDataLine.class);
    AudioInputStream audioInputStream = mock(AudioInputStream.class);

    private AudioInputHandlerImpl getStartedAudioInputHandler() throws LineUnavailableException {
        doReturn(line).when(audioInputHandler).getTargetDataLine(audioFormat);
        doReturn(audioInputStream).when(audioInputHandler).getAudioInputStream();
        doReturn(audioFormat).when(audioInputHandler).getAudioFormat();

        audioInputHandler.start();
        return audioInputHandler;
    }

    @Test
    void start() throws LineUnavailableException {
        doReturn(line).when(audioInputHandler).getTargetDataLine(audioFormat);
        doReturn(audioInputStream).when(audioInputHandler).getAudioInputStream();
        doReturn(audioFormat).when(audioInputHandler).getAudioFormat();

        audioInputHandler.start();

        verify(line).open();
        verify(line).start();
        verify(audioInputHandler).getAudioInputStream();
    }

    @Test
    void pause_resume() throws LineUnavailableException {
        AudioInputHandlerImpl handler = getStartedAudioInputHandler();
        handler.pause();
        handler.resume();
        handler.pause();
        handler.resume();

        verify(line, times(3)).start();
        verify(line, times(2)).stop();
    }
}