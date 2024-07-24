module com.goddard.goddardpdf {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires org.apache.pdfbox;


    opens com.goddard.goddardpdf to javafx.fxml;
    exports com.goddard.goddardpdf;
}