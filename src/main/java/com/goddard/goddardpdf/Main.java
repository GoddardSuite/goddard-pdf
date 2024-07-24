package com.goddard.goddardpdf;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.WindowEvent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

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
    private MenuItem save;
    private Menu export;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Goddard PDF");
        BorderPane root = new BorderPane();

        MenuBar menuBar = getMenuItems();

        controls = new HBox(10);
        controls.setPadding(new Insets(10));

        pageSelectContents = FXCollections.observableArrayList();

        prevButton = new Button("Previous Page");
        nextButton = new Button("Next Page");
        pageSelect = new ChoiceBox<>(pageSelectContents);
        zoom = new Slider(0f, 1f, 0.5f);

        pdfView = new ImageView();
        pdfView.setPreserveRatio(true);
        ZoomableScrollPane scrollPane = new ZoomableScrollPane(pdfView, zoom);
        root.setCenter(scrollPane);

        zoom.valueProperty().addListener(
            (observable, oldValue, newValue) -> {
                scrollPane.scaleValue = (double) newValue;
                scrollPane.updateScale();
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

    private MenuBar getMenuItems() {
        Menu file = new Menu("File");
        Menu view = new Menu("View");

        MenuItem open = new MenuItem("Open");
        save = new MenuItem("Save");
        export = new Menu("Export");
        MenuItem exit = new MenuItem("Exit");

        save.setDisable(true);
        export.setDisable(true);

        MenuItem exportPng = new MenuItem("PNG");
        MenuItem exportTxt = new MenuItem("TXT");

        exportPng.setOnAction(e -> export("PNG"));
        exportTxt.setOnAction(e -> export("TXT"));

        export.getItems().addAll(exportPng, exportTxt);

        open.setOnAction(e -> openPDF(primaryStage));
        save.setOnAction(e -> export("PDF"));
        exit.setOnAction(e -> Platform.exit());

        CheckMenuItem pageByPage = new CheckMenuItem("Page by Page");
        CheckMenuItem slideshow = new CheckMenuItem("Slideshow");
        CheckMenuItem thumbnails = new CheckMenuItem("Thumbnails");

        file.getItems().addAll(open, save, export, exit);
        view.getItems().addAll(pageByPage, slideshow, thumbnails);

        return new MenuBar(file, view);
    }

    private void openPDF(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                document = PDDocument.load(file);
                primaryStage.setTitle(file.getName());
                currentPage = 0;
                controls.getChildren().forEach(n -> n.setDisable(false));
                save.setDisable(false);
                export.setDisable(false);
                pageSelectContents.clear();
                for (int i = 0; i < document.getNumberOfPages(); i++) pageSelectContents.add(Integer.toString(i + 1));
                pageSelect.getSelectionModel().selectFirst();
                showPage(currentPage);
            } catch (IOException e) {
                showError("Failed to open PDF: " + e.getMessage());
            }
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
        } catch (IOException e) {
            showError("Failed to render page: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void export(String fileType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(fileType + " Files", "*." + fileType.toLowerCase()));

        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        switch (fileType) {
            case "PDF" -> {
                try {
                    document.save(file);
                    showInfo("PDF exported successfully!");
                } catch (IOException e) {
                    showError("Failed to export PDF: " + e.getMessage());
                }
            }
            case "PNG" -> {
                Stage progressStage = new Stage();
                Label progressLabel = new Label("0 out of 0 exported");
                ProgressBar progressBar = new ProgressBar(0);
                VBox progressBox = new VBox(10, progressLabel, progressBar);
                progressBox.setPadding(new Insets(10));
                progressStage.setScene(new Scene(progressBox));
                progressStage.initModality(Modality.APPLICATION_MODAL);
                progressStage.setTitle("Export Progress");
                progressStage.setOnCloseRequest(WindowEvent::consume);
                progressStage.show();

                PDFRenderer renderer = new PDFRenderer(document);
                int numPages = document.getNumberOfPages();
                new Thread(() -> {
                    for (int i = 0; i < numPages; i++) {
                        try {
                            BufferedImage image = renderer.renderImageWithDPI(i, 300);
                            File pngFile = new File(file.getParent(), file.getName().replace(".png", "") + "_" + (i + 1) + ".png");
                            javax.imageio.ImageIO.write(image, "png", pngFile);
                            final double progress = (double) (i + 1) / numPages;
                            final String progressText = (i + 1) + " out of " + numPages + " exported";
                            Platform.runLater(() -> {
                                progressBar.setProgress(progress);
                                progressLabel.setText(progressText);
                            });
                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                showError("Failed to export PNG: " + e.getMessage());
                                progressStage.close();
                            });
                            return;
                        }
                    }
                    Platform.runLater(() -> {
                        showInfo("PNG images exported successfully!");
                        progressStage.close();
                    });
                }).start();
            }
            case "TXT" -> {
                StringBuilder textContent = new StringBuilder();
                try {
                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        textContent.append(new PDFTextStripper().getText(document));
                    }
                    java.nio.file.Files.write(file.toPath(), textContent.toString().getBytes());
                    showInfo("Text exported successfully!");
                } catch (IOException e) {
                    showError("Failed to export TXT: " + e.getMessage());
                }
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
