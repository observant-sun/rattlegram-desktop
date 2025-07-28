module rattlegram {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires java.desktop;
    requires java.prefs;
    requires static lombok;

    opens com.github.observant_sun.rattlegram.controller to javafx.fxml;
    exports com.github.observant_sun.rattlegram;
}