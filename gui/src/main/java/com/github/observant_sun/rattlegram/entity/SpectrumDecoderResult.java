package com.github.observant_sun.rattlegram.entity;

import javafx.scene.image.PixelBuffer;

import java.nio.IntBuffer;

public record SpectrumDecoderResult(
        PixelBuffer<IntBuffer> spectrumPixels,
        PixelBuffer<IntBuffer> spectrogramPixels
) {
}
