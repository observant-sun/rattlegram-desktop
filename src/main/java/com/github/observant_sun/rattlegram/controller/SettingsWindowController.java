package com.github.observant_sun.rattlegram.controller;

import com.github.observant_sun.rattlegram.prefs.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

public class SettingsWindowController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SettingsWindowController.class);


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
    @FXML private Label inputAudioModeChoiceBoxLabel;
    @FXML private ChoiceBox<AudioMode> inputAudioModeChoiceBox;
    @FXML private Label outputSampleRateChoiceBoxLabel;
    @FXML private ChoiceBox<SampleRate> outputSampleRateChoiceBox;
    @FXML private Label outputChannelChoiceBoxLabel;
    @FXML private ChoiceBox<OutputChannel> outputChannelChoiceBox;
    @FXML private Label outputAudioModeChoiceBoxLabel;
    @FXML private ChoiceBox<AudioMode> outputAudioModeChoiceBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateChoiceBoxes();

        loadSettings();
    }

    private void populateChoiceBoxes() {
        inputSampleRateChoiceBox.getItems().addAll(SampleRate.values());
        leadingNoiseChoiceBox.getItems().addAll(LeadingNoise.values());
        inputChannelChoiceBox.getItems().addAll(InputChannel.values());
        inputAudioModeChoiceBox.getItems().addAll(AudioMode.values());
        outputSampleRateChoiceBox.getItems().addAll(SampleRate.values());
        outputChannelChoiceBox.getItems().addAll(OutputChannel.values());
        outputAudioModeChoiceBox.getItems().addAll(AudioMode.values());
    }

    public void loadSettings() {
        log.debug("load preferences");
        AppPreferences prefs = AppPreferences.get();

        SampleRate inputSampleRate = prefs.get(AppPreferences.Pref.INPUT_SAMPLE_RATE, SampleRate.class);
        inputSampleRateChoiceBox.setValue(inputSampleRate);
        InputChannel inputChannel = prefs.get(AppPreferences.Pref.INPUT_CHANNEL, InputChannel.class);
        inputChannelChoiceBox.setValue(inputChannel);
        AudioMode inputAudioMode = prefs.get(AppPreferences.Pref.INPUT_AUDIO_MODE, AudioMode.class);
        inputAudioModeChoiceBox.setValue(inputAudioMode);
        SampleRate outputSampleRate = prefs.get(AppPreferences.Pref.OUTPUT_SAMPLE_RATE, SampleRate.class);
        outputSampleRateChoiceBox.setValue(outputSampleRate);
        Integer carrierFrequency = prefs.get(AppPreferences.Pref.CARRIER_FREQUENCY, Integer.class);
        carrierFrequencySpinner.getValueFactory().setValue(carrierFrequency);
        LeadingNoise leadingNoise = prefs.get(AppPreferences.Pref.LEADING_NOISE, LeadingNoise.class);
        leadingNoiseChoiceBox.setValue(leadingNoise);
        Boolean fancyHeader = prefs.get(AppPreferences.Pref.FANCY_HEADER, Boolean.class);
        fancyHeaderCheckBox.setSelected(fancyHeader);
        OutputChannel outputChannel = prefs.get(AppPreferences.Pref.OUTPUT_CHANNEL, OutputChannel.class);
        outputChannelChoiceBox.setValue(outputChannel);
        AudioMode outputAudioMode = prefs.get(AppPreferences.Pref.OUTPUT_AUDIO_MODE, AudioMode.class);
        outputAudioModeChoiceBox.setValue(outputAudioMode);
    }

    public void saveSettings() {
        log.debug("save preferences");
        AppPreferences prefs = AppPreferences.get();
        SampleRate inputSampleRate = inputSampleRateChoiceBox.getValue();
        prefs.set(AppPreferences.Pref.INPUT_SAMPLE_RATE, inputSampleRate);
        InputChannel inputChannel = inputChannelChoiceBox.getValue();
        prefs.set(AppPreferences.Pref.INPUT_CHANNEL, inputChannel);
        AudioMode inputAudioMode = inputAudioModeChoiceBox.getValue();
        prefs.set(AppPreferences.Pref.INPUT_AUDIO_MODE, inputAudioMode);
        SampleRate outputSampleRate = outputSampleRateChoiceBox.getValue();
        prefs.set(AppPreferences.Pref.OUTPUT_SAMPLE_RATE, outputSampleRate);
        Integer carrierFrequency = carrierFrequencySpinner.getValue();
        prefs.set(AppPreferences.Pref.CARRIER_FREQUENCY, carrierFrequency);
        LeadingNoise leadingNoise = leadingNoiseChoiceBox.getValue();
        prefs.set(AppPreferences.Pref.LEADING_NOISE, leadingNoise);
        Boolean fancyHeader = fancyHeaderCheckBox.isSelected();
        prefs.set(AppPreferences.Pref.FANCY_HEADER, fancyHeader);
        OutputChannel outputChannel = outputChannelChoiceBox.getValue();
        prefs.set(AppPreferences.Pref.OUTPUT_CHANNEL, outputChannel);
        AudioMode outputAudioMode = outputAudioModeChoiceBox.getValue();
        prefs.set(AppPreferences.Pref.OUTPUT_AUDIO_MODE, outputAudioMode);
    }

}
