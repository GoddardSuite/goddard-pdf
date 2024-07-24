import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GoddardPDF extends Application {

    private PDDocument document;
    private int currentPage = 0;
    private ImageView pdfView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GoddardPDF");

        BorderPane root = new BorderPane();
        pdfView = new ImageView();
        pdfView.setPreserveRatio(true);
        ScrollPane scrollPane = new ScrollPane(pdfView);
        root.setCenter(scrollPane);

        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        Button openButton = new Button("Open");
        Button prevButton = new Button("Previous");
        Button nextButton = new Button("Next");

        prevButton.setDisable(true);
        nextButton.setDisable(true);

        openButton.setOnAction(e -> openPDF(primaryStage));
        prevButton.setOnAction(e -> showPage(currentPage - 1));
        nextButton.setOnAction(e -> showPage(currentPage + 1));

        controls.getChildren().addAll(openButton, prevButton, nextButton);
        root.setBottom(controls);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openPDF(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                document = Loader.loadPDF(file);
                currentPage = 0;
                showPage(currentPage);
            } catch (Exception e) {

            }
        }
    }

    private void showPage(int pageIndex) {
        if (document == null || pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            return;
        }

        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(pageIndex);
            pdfView.setImage(SwingFXUtils.toFXImage(image, null));
            currentPage = pageIndex;
        } catch (IOException e) {
            showError("Failed to render page: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
