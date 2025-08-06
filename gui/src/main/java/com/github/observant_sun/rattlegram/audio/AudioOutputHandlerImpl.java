package com.github.observant_sun.rattlegram.audio;

import com.google.common.base.Stopwatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
class AudioOutputHandlerImpl implements AudioOutputHandler {

    private final int sampleRate;
    private final int channelCount;
    private final boolean useOutputDrainBlockWorkaround;

    @Override
    public synchronized void play(byte[] buffer) throws LineUnavailableException {
        Clip clip = AudioSystem.getClip();
        AudioFormat format;
        format = getAudioFormat();
        clip.open(format, buffer, 0, buffer.length);
        if (useOutputDrainBlockWorkaround) {
            startAndDrainBlockingly(clip, buffer.length);
        } else {
            startAndDrain(clip);
        }
    }

    private void startAndDrain(Clip clip) {
        clip.start();
        clip.drain();
    }

    private void startAndDrainBlockingly(Clip clip, int bufferSize) {
        // clip.getMicrosecondLength() returns incorrect duration
        long clipDurationMs = bufferSize * 1000L / sampleRate / 2 / channelCount;
        Stopwatch stopwatch = Stopwatch.createStarted();
        clip.start();
        clip.drain();
        long elapsedMs = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        long sleepDuration = clipDurationMs - elapsedMs;
        if (sleepDuration > 0) {
            try {
                Thread.sleep(sleepDuration);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.debug("duration: {}", elapsedMs);
    }

    private AudioFormat getAudioFormat() {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        int sampleSizeInBits = 16;
        int frameSize = 2 * channelCount;
        float frameRate = (float) (sampleRate / frameSize);
        boolean bigEndian = false;
        return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channelCount, frameSize, frameRate, bigEndian);
    }
}
