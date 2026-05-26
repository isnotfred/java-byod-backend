module pup.edu.ph.it.javabyodsystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens pup.edu.ph.it.javabyodsystem to javafx.fxml;
    exports pup.edu.ph.it.javabyodsystem;
}