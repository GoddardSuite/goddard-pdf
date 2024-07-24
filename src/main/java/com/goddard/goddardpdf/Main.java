package com.goddard.goddardpdf;

import javafx.application.Application;
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

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GoddardPDF");

        BorderPane root = new BorderPane();
        pdfView = new ImageView();
        pdfView.setPreserveRatio(true);
        ScrollPane scrollPane = new ScrollPane(pdfView);
        root.setCenter(scrollPane);

        Menu file = new Menu("File");

        MenuItem open = new MenuItem("Open");
        open.setOnAction(e -> openPDF(primaryStage));
        file.getItems().add(open);

        MenuBar menuBar = new MenuBar(file);

        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));

        pageSelectContents = FXCollections.observableArrayList();

        prevButton = new Button("Previous");
        nextButton = new Button("Next");
        pageSelect = new ChoiceBox<>(pageSelectContents);

        prevButton.setDisable(true);
        nextButton.setDisable(true);
        pageSelect.setDisable(true);

        prevButton.setOnAction(e -> showPage(currentPage - 1));
        nextButton.setOnAction(e -> showPage(currentPage + 1));
        pageSelect.setOnAction(e -> showPage(pageSelect.getSelectionModel().getSelectedIndex()));

        controls.getChildren().addAll(prevButton, nextButton, pageSelect);

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
                nextButton.setDisable(false);
                prevButton.setDisable(false);
                pageSelect.setDisable(false);
                pageSelectContents.clear();
                for (int i = 0; i < document.getNumberOfPages(); i++) pageSelectContents.add(Integer.toString(i + 1));
                pageSelect.getSelectionModel().selectFirst();
                showPage(currentPage);
            } catch (IOException e) { showError("Failed to open PDF: " + e.getMessage()); }
        }
    }

    private void showPage(int pageIndex) {
        if (document == null || pageIndex < 0 || pageIndex >= document.getNumberOfPages()) return;

        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);
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
