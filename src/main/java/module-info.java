module com.parking {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // JDBC + driver MySQL (module automatique depuis mysql-connector-j.jar)
    requires java.sql;
    requires mysql.connector.j;

    // Sécurité & utilitaires
    requires jbcrypt;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    // Logging (SLF4J API ; Logback résolu via classpath)
    requires org.slf4j;

    opens com.parking.controller to javafx.fxml;

    exports com.parking;
    exports com.parking.config;
    exports com.parking.controller;
    exports com.parking.exception;
}
