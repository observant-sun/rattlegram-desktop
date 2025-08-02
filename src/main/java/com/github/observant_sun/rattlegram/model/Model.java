package com.github.observant_sun.rattlegram.model;

import com.github.observant_sun.rattlegram.encoding.Decoder;
import com.github.observant_sun.rattlegram.entity.*;
import com.github.observant_sun.rattlegram.prefs.*;
import com.github.observant_sun.rattlegram.util.SimplePublisher;
import com.github.observant_sun.rattlegram.util.VoidPublisher;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Model {

    private static final class InstanceHolder {
        private static final Model instance = new Model();
    }

    public static Model get() {
        return InstanceHolder.instance;
    }

    @Getter
    private final AtomicReference<Decoder> decoderReference = new AtomicReference<>();

    @Getter
    private ObservableList<Message> messages = FXCollections.observableArrayList();
    @Getter
    private ObservableList<Message> incomingMessages = FXCollections.observableArrayList();
    @Getter
    private final SimplePublisher<OutgoingMessage> newOutgoingMessagePublisher = new SimplePublisher<>();

    @Getter
    private final SimplePublisher<Message> newIncomingMessagePublisher = new SimplePublisher<>();
    private List<StatusUpdate> statusUpdates = new ArrayList<>();
    @Getter
    private final SimplePublisher<StatusUpdate> statusUpdatePublisher = new SimplePublisher<>();

    @Getter
    private final VoidPublisher transmissionBeginPublisher = new VoidPublisher();
    @Getter
    private final VoidPublisher listeningBeginPublisher = new VoidPublisher();
    @Getter
    private final SimplePublisher<SpectrumImages> updateSpectrogramPublisher = new SimplePublisher<>();

    private Model() {

    }

    void processStatusUpdate(StatusUpdate statusUpdate) {
        statusUpdates.add(statusUpdate);
        statusUpdatePublisher.submit(statusUpdate);
    }

    private volatile BooleanProperty showSpectrumAnalyzer;
    public BooleanProperty showSpectrumAnalyzerProperty() {
        if (showSpectrumAnalyzer == null) {
            synchronized (this) {
                if (showSpectrumAnalyzer == null) {
                    boolean initialValue = AppPreferences.get().get(Pref.SHOW_SPECTRUM_ANALYZER, Boolean.class);
                    showSpectrumAnalyzer = new SimpleBooleanProperty(this, "showSpectrogram", initialValue);
                    showSpectrumAnalyzer.addListener((observable, oldValue, newValue) -> {
                        AppPreferences.get().set(Pref.SHOW_SPECTRUM_ANALYZER, newValue);
                    });
                }
            }
        }
        return showSpectrumAnalyzer;
    }

    private final BooleanProperty showRepeaterWindow = new SimpleBooleanProperty(false);
    public BooleanProperty showRepeaterWindowProperty() {
        return showRepeaterWindow;
    }

    public void toggleRepeaterWindow() {
        showRepeaterWindow.set(!showRepeaterWindow.get());
    }

    private volatile BooleanProperty repeaterModeEnabled;
    public BooleanProperty repeaterModeEnabledProperty() {
        if (repeaterModeEnabled == null) {
            synchronized (this) {
                if (repeaterModeEnabled == null) {
                    boolean initialValue = AppPreferences.get().get(Pref.REPEATER_MODE_ENABLED, Boolean.class);
                    repeaterModeEnabled = new SimpleBooleanProperty(this, "repeaterModeEnabled", initialValue);
                    repeaterModeEnabled.addListener((observable, oldValue, newValue) -> {
                        AppPreferences.get().set(Pref.REPEATER_MODE_ENABLED, newValue);
                    });
                }
            }
        }
        return repeaterModeEnabled;
    }

    private volatile IntegerProperty repeaterDelay;
    public IntegerProperty repeaterDelayProperty() {
        if (repeaterDelay == null) {
            synchronized (this) {
                if (repeaterDelay == null) {
                    int initialValue = AppPreferences.get().get(Pref.REPEATER_DELAY, Integer.class);
                    repeaterDelay = new SimpleIntegerProperty(this, "repeaterDelay", initialValue);
                    repeaterDelay.addListener((observable, oldValue, newValue) -> {
                        AppPreferences.get().set(Pref.REPEATER_DELAY, newValue);
                    });
                }
            }
        }
        return repeaterDelay;
    }

    private volatile IntegerProperty repeaterDebounceTime;
    public IntegerProperty repeaterDebounceTimeProperty() {
        if (repeaterDebounceTime == null) {
            synchronized (this) {
                if (repeaterDebounceTime == null) {
                    int initialValue = AppPreferences.get().get(Pref.REPEATER_DEBOUNCE_TIME, Integer.class);
                    repeaterDebounceTime = new SimpleIntegerProperty(this, "repeaterDebounceTime", initialValue);
                    repeaterDebounceTime.addListener((observable, oldValue, newValue) -> {
                        AppPreferences.get().set(Pref.REPEATER_DEBOUNCE_TIME, newValue);
                    });
                }
            }
        }
        return repeaterDebounceTime;
    }

    public Decoder getDecoder() {
        return decoderReference.get();
    }

    public void setDecoder(Decoder decoder) {
        this.decoderReference.set(decoder);
    }

}
