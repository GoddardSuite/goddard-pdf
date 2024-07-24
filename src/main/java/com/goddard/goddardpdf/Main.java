package com.goddard.goddardpdf;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main extends Application {

    private PDDocument document;
    private int currentPage = 0;
    private ImageView pdfView;
    private Button prevButton;
    private Button nextButton;
    private ChoiceBox<String> pageSelect;
    private ObservableList<String> pageSelectContents;
    public Slider zoom;
    private HBox controls;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Goddard PDF");

        BorderPane root = new BorderPane();
        pdfView = new ImageView();
        pdfView.setPreserveRatio(true);
        ZoomableScrollPane scrollPane = new ZoomableScrollPane(pdfView);
        root.setCenter(scrollPane);

        Menu file = new Menu("File");

        MenuItem open = new MenuItem("Open");
        MenuItem exit = new MenuItem("Exit");

        open.setOnAction(e -> openPDF(primaryStage));
        exit.setOnAction(e -> Platform.exit());

        file.getItems().addAll(open, exit);

        MenuBar menuBar = new MenuBar(file);

        controls = new HBox(10);
        controls.setPadding(new Insets(10));

        pageSelectContents = FXCollections.observableArrayList();

        prevButton = new Button("Previous Page");
        nextButton = new Button("Next Page");
        pageSelect = new ChoiceBox<>(pageSelectContents);
        zoom = new Slider(0f, 1f, 0.5f);

        zoom.setShowTickLabels(true);
        zoom.setMajorTickUnit(0.5f);
        zoom.valueProperty().addListener(
            new ChangeListener<>() {
                public void changed(ObservableValue<? extends Number>observable, Number oldValue, Number newValue) {
                    scrollPane.scaleValue = (double) newValue;
                    scrollPane.updateScale();
                }
            }
        );


        prevButton.setOnAction(e -> showPage(currentPage - 1));
        nextButton.setOnAction(e -> showPage(currentPage + 1));
        pageSelect.setOnAction(e -> showPage(pageSelect.getSelectionModel().getSelectedIndex()));

        controls.getChildren().addAll(prevButton, nextButton, pageSelect, zoom);
        controls.getChildren().forEach(n -> n.setDisable(true));

        root.setTop(menuBar);
        root.setBottom(controls);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    private void openPDF(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                document = PDDocument.load(file);
                currentPage = 0;
                controls.getChildren().forEach(n -> n.setDisable(false));
                pageSelectContents.clear();
                for (int i=0; i < document.getNumberOfPages(); i++) pageSelectContents.add(Integer.toString(i + 1));
                pageSelect.getSelectionModel().selectFirst();
                showPage(currentPage);
            } catch (IOException e) { showError("Failed to open PDF: " + e.getMessage()); }
        }
    }

    private void showPage(int pageIndex) {
        if (document == null || pageIndex < 0 || pageIndex >= document.getNumberOfPages()) return;

        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 600);
            pdfView.setImage(SwingFXUtils.toFXImage(image, null));
            currentPage = pageIndex;
            nextButton.setDisable(currentPage + 1 == document.getNumberOfPages());
            prevButton.setDisable(currentPage == 0);
            pageSelect.getSelectionModel().select(pageIndex);
        } catch (IOException e) { showError("Failed to render page: " + e.getMessage()); }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
