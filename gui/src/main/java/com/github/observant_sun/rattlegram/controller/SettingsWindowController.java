package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.util.AudioUtils;
import com.github.observant_sun.rattlegram.entity.AudioMixerInfoWrapper;
import com.github.observant_sun.rattlegram.model.Model;
import com.github.observant_sun.rattlegram.prefs.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;

@Slf4j
public class SettingsWindowController implements Initializable {

    private final Model model = Model.get();

    @FXML private TilePane tilePane;
    @FXML private Label inputSampleRateChoiceBoxLabel;
    @FXML private ChoiceBox<SampleRate> inputSampleRateChoiceBox;
    @FXML private Label carrierFrequencySpinnerLabel;
    @FXML private Spinner<Integer> carrierFrequencySpinner;
    @FXML private Label leadingNoiseChoiceBoxLabel;
    @FXML private ChoiceBox<LeadingNoise> leadingNoiseChoiceBox;
    @FXML private Label fancyHeaderCheckBoxLabel;
    @FXML private CheckBox fancyHeaderCheckBox;
    @FXML private Label inputChannelChoiceBoxLabel;
    @FXML private ChoiceBox<InputChannel> inputChannelChoiceBox;
    @FXML private Label outputSampleRateChoiceBoxLabel;
    @FXML private ChoiceBox<SampleRate> outputSampleRateChoiceBox;
    @FXML private Label outputChannelChoiceBoxLabel;
    @FXML private ChoiceBox<OutputChannel> outputChannelChoiceBox;
    @FXML private Label stopListeningWhenTransmittingCheckBoxLabel;
    @FXML private CheckBox stopListeningWhenTransmittingCheckBox;
    @FXML private Label blockAudioOutputDrainWorkaroundCheckBoxLabel;
    @FXML private CheckBox blockAudioOutputDrainWorkaroundCheckBox;
    @FXML private Label inputMixerInfoChoiceBoxLabel;
    @FXML private ChoiceBox<AudioMixerInfoWrapper> inputMixerInfoChoiceBox;
    @FXML private Label outputMixerInfoChoiceBoxLabel;
    @FXML private ChoiceBox<AudioMixerInfoWrapper> outputMixerInfoChoiceBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateChoiceBoxes();

        loadSettings();
    }

    private void populateChoiceBoxes() {
        inputSampleRateChoiceBox.getItems().addAll(SampleRate.values());
        leadingNoiseChoiceBox.getItems().addAll(LeadingNoise.values());
        inputChannelChoiceBox.getItems().addAll(InputChannel.values());
        outputSampleRateChoiceBox.getItems().addAll(SampleRate.values());
        outputChannelChoiceBox.getItems().addAll(OutputChannel.values());
        inputMixerInfoChoiceBox.getItems().addAll(AudioUtils.getMixerInfos());
        outputMixerInfoChoiceBox.getItems().addAll(AudioUtils.getMixerInfos());
    }

    public void loadSettings() {
        log.debug("load preferences");
        AppPreferences prefs = AppPreferences.get();

        SampleRate inputSampleRate = prefs.get(Pref.INPUT_SAMPLE_RATE, SampleRate.class);
        inputSampleRateChoiceBox.setValue(inputSampleRate);
        InputChannel inputChannel = prefs.get(Pref.INPUT_CHANNEL, InputChannel.class);
        inputChannelChoiceBox.setValue(inputChannel);
        SampleRate outputSampleRate = prefs.get(Pref.OUTPUT_SAMPLE_RATE, SampleRate.class);
        outputSampleRateChoiceBox.setValue(outputSampleRate);
        Integer carrierFrequency = prefs.get(Pref.CARRIER_FREQUENCY, Integer.class);
        carrierFrequencySpinner.getValueFactory().setValue(carrierFrequency);
        LeadingNoise leadingNoise = prefs.get(Pref.LEADING_NOISE, LeadingNoise.class);
        leadingNoiseChoiceBox.setValue(leadingNoise);
        Boolean fancyHeader = prefs.get(Pref.FANCY_HEADER, Boolean.class);
        fancyHeaderCheckBox.setSelected(fancyHeader);
        OutputChannel outputChannel = prefs.get(Pref.OUTPUT_CHANNEL, OutputChannel.class);
        outputChannelChoiceBox.setValue(outputChannel);
        Boolean stopListeningWhenTransmitting = prefs.get(Pref.STOP_LISTENING_WHEN_TRANSMITTING, Boolean.class);
        stopListeningWhenTransmittingCheckBox.setSelected(stopListeningWhenTransmitting);
        Boolean blockAudioOutputDrainWorkaround = prefs.get(Pref.BLOCK_OUTPUT_DRAIN_WORKAROUND, Boolean.class);
        blockAudioOutputDrainWorkaroundCheckBox.setSelected(blockAudioOutputDrainWorkaround);

        String inputMixerStringRepresentation = prefs.get(Pref.INPUT_AUDIO_MIXER_STRING_REPRESENTATION, String.class);
        inputMixerInfoChoiceBox.getItems().stream()
                .filter(item -> item.toString().equals(inputMixerStringRepresentation))
                .findFirst()
                .ifPresent(item -> inputMixerInfoChoiceBox.getSelectionModel().select(item));
        String outputMixerStringRepresentation = prefs.get(Pref.OUTPUT_AUDIO_MIXER_STRING_REPRESENTATION, String.class);
        outputMixerInfoChoiceBox.getItems().stream()
                .filter(item -> item.toString().equals(outputMixerStringRepresentation))
                .findFirst()
                .ifPresent(item -> outputMixerInfoChoiceBox.getSelectionModel().select(item));
    }

    public void saveSettings() {
        log.debug("save preferences");
        AppPreferences prefs = AppPreferences.get();
        SampleRate inputSampleRate = inputSampleRateChoiceBox.getValue();
        prefs.set(Pref.INPUT_SAMPLE_RATE, inputSampleRate);
        InputChannel inputChannel = inputChannelChoiceBox.getValue();
        prefs.set(Pref.INPUT_CHANNEL, inputChannel);
        SampleRate outputSampleRate = outputSampleRateChoiceBox.getValue();
        prefs.set(Pref.OUTPUT_SAMPLE_RATE, outputSampleRate);
        Integer carrierFrequency = carrierFrequencySpinner.getValue();
        prefs.set(Pref.CARRIER_FREQUENCY, carrierFrequency);
        LeadingNoise leadingNoise = leadingNoiseChoiceBox.getValue();
        prefs.set(Pref.LEADING_NOISE, leadingNoise);
        Boolean fancyHeader = fancyHeaderCheckBox.isSelected();
        prefs.set(Pref.FANCY_HEADER, fancyHeader);
        OutputChannel outputChannel = outputChannelChoiceBox.getValue();
        prefs.set(Pref.OUTPUT_CHANNEL, outputChannel);
        Boolean stopListeningWhenTransmitting = stopListeningWhenTransmittingCheckBox.isSelected();
        prefs.set(Pref.STOP_LISTENING_WHEN_TRANSMITTING, stopListeningWhenTransmitting);
        Boolean blockAudioOutputDrainWorkaround = blockAudioOutputDrainWorkaroundCheckBox.isSelected();
        prefs.set(Pref.BLOCK_OUTPUT_DRAIN_WORKAROUND, blockAudioOutputDrainWorkaround);
        AudioMixerInfoWrapper inputMixerInfoWrapper = inputMixerInfoChoiceBox.getValue();
        model.inputMixerInfoProperty().set(inputMixerInfoWrapper);
        AudioMixerInfoWrapper outputMixerInfoWrapper = outputMixerInfoChoiceBox.getValue();
        model.outputMixerInfoProperty().set(outputMixerInfoWrapper);
    }

}
