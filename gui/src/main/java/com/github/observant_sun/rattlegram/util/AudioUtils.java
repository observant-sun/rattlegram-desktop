package com.github.observant_sun.rattlegram.util;

import com.github.observant_sun.rattlegram.entity.AudioMixerInfoWrapper;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AudioUtils {

    private static final String DEFAULT_STRING = "Default";
    private static final AudioMixerInfoWrapper DEFAULT_MIXER = new AudioMixerInfoWrapper(DEFAULT_STRING, null);

    private AudioUtils() {}

    public static List<AudioMixerInfoWrapper> getMixerInfos() {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        List<AudioMixerInfoWrapper> list = new ArrayList<>();
        list.add(DEFAULT_MIXER);
        for (Mixer.Info mixerInfo : mixerInfos) {
            AudioMixerInfoWrapper audioMixerInfoWrapper = new AudioMixerInfoWrapper(mixerInfo.toString(), mixerInfo);
            list.add(audioMixerInfoWrapper);
        }
        return list;
    }

    public static Optional<AudioMixerInfoWrapper> getByStringRepresentation(String stringRepresentation) {
        return getMixerInfos().stream()
                .filter(info -> info.toString().equals(stringRepresentation))
                .findFirst();
    }

    public static AudioMixerInfoWrapper getDefaultMixer() {
        return DEFAULT_MIXER;
    }
}
