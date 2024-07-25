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
import java.util.Objects;

public class Main extends Application {

    private PDDocument document;
    private int currentPage = 0;

    private BorderPane root;
    private ImageView pdfView;
    private Button prevButton;
    private Button nextButton;
    private ChoiceBox<String> pageSelect;
    public Slider zoom;
    private Stage primaryStage;

    private Menu view;
    private MenuItem save;
    private Menu export;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Goddard PDF");
        root = new BorderPane();

        MenuBar menuBar = getMenuItems();
        root.setTop(menuBar);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    private MenuBar getMenuItems() {
        Menu file = new Menu("File");
        view = new Menu("View");

        MenuItem open = new MenuItem("Open");
        save = new MenuItem("Save");
        export = new Menu("Export");
        MenuItem exit = new MenuItem("Exit");

        save.setDisable(true);
        export.setDisable(true);

        MenuItem exportPng = new MenuItem("PNG");
        MenuItem exportJpeg = new MenuItem("JPEG");
        MenuItem exportTxt = new MenuItem("TXT");

        export.getItems().addAll(exportPng, exportJpeg, exportTxt);
        export.getItems().forEach(item -> item.setOnAction(actionEvent -> export(item.getText())));

        open.setOnAction(actionEvent -> openPDF());
        save.setOnAction(actionEvent -> export("PDF"));
        exit.setOnAction(actionEvent -> Platform.exit());

        CheckMenuItem pageByPage = new CheckMenuItem("Page by Page");
        CheckMenuItem slideshow = new CheckMenuItem("Slideshow");
        CheckMenuItem thumbnails = new CheckMenuItem("Thumbnails");
        CheckMenuItem scroll = new CheckMenuItem("Continuous Scroll");

        file.getItems().addAll(open, save, export, exit);
        view.getItems().addAll(pageByPage, slideshow, thumbnails, scroll);

        pageByPage.setSelected(true);
        view.getItems().forEach(item -> item.setDisable(true));
        view.getItems().forEach(item -> item.setOnAction(actionEvent -> setViewMode(item.getText(), view)));

        return new MenuBar(file, view);
    }

    private void setViewMode(String mode, Menu view) {
        view.getItems().forEach(item -> {
            if (item instanceof CheckMenuItem && !Objects.equals(item.getText(), mode)) {
                ((CheckMenuItem) item).setSelected(false);
            }
        });

        switch (mode) {
            case "Page by Page" -> pageByPage();
            case "Slideshow" -> slideshow();
            case "Thumbnails" -> thumbnails();
            case "Continuous Scroll" -> continuousScroll();
        }
    }

    private void pageByPage() {
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));

        ObservableList<String> pageSelectContents = FXCollections.observableArrayList();

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

        prevButton.setOnAction(e -> showPage(currentPage - 1, true));
        nextButton.setOnAction(e -> showPage(currentPage + 1, true));
        pageSelect.setOnAction(e -> showPage(pageSelect.getSelectionModel().getSelectedIndex(), true));

        controls.getChildren().addAll(prevButton, nextButton, pageSelect, zoom);

        root.setBottom(controls);

        currentPage = 0;
        save.setDisable(false);
        export.setDisable(false);
        pageSelectContents.clear();
        for (int i = 0; i < document.getNumberOfPages(); i++) pageSelectContents.add(Integer.toString(i + 1));
        pageSelect.getSelectionModel().selectFirst();
        showPage(currentPage, true);
    }

    private void slideshow() {
        root.setBottom(null);
        pdfView = new ImageView();
        pdfView.setPreserveRatio(true);

        ScrollPane pane = new ScrollPane(pdfView);
        root.setCenter(pane);

        currentPage = 0;
        save.setDisable(false);
        export.setDisable(false);
        showPage(currentPage, false);
    }

    private void thumbnails() {
        root.setBottom(null);

    }

    private void continuousScroll() {
        root.setBottom(null);

    }

    private void openPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            view.getItems().forEach(item -> item.setDisable(false));
            try {
                document = PDDocument.load(file);
                primaryStage.setTitle(file.getName());
                pageByPage();
            } catch (IOException e) { showError("Failed to open PDF: " + e.getMessage()); }
        }
    }

    private void showPage(int pageIndex, boolean pageByPage) {
        if (document == null || pageIndex < 0 || pageIndex >= document.getNumberOfPages()) return;

        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);
            pdfView.setImage(SwingFXUtils.toFXImage(image, null));
            currentPage = pageIndex;
            if (pageByPage) {
                nextButton.setDisable(currentPage + 1 == document.getNumberOfPages());
                prevButton.setDisable(currentPage == 0);
                pageSelect.getSelectionModel().select(pageIndex);
            }
        } catch (IOException e) { showError("Failed to render page: " + e.getMessage()); }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void export(String fileType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fileType + " Files", "*." + fileType.toLowerCase())
        );

        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        switch (fileType) {
            case "PDF" -> {
                try {
                    document.save(file);
                    showInfo("PDF exported successfully!");
                } catch (IOException e) { showError("Failed to export PDF: " + e.getMessage()); }
            }
            case "PNG", "JPEG" -> {
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
                            File imageFile = new File(file.getParent(), file.getName().replace("." + fileType.toLowerCase(), "") + "_" + (i + 1) + "." + fileType.toLowerCase());
                            javax.imageio.ImageIO.write(image, fileType.toLowerCase(), imageFile);
                            final double progress = (double) (i + 1) / numPages;
                            final String progressText = (i + 1) + " out of " + numPages + " exported";
                            Platform.runLater(() -> {
                                progressBar.setProgress(progress);
                                progressLabel.setText(progressText);
                            });
                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                showError("Failed to export images: " + e.getMessage());
                                progressStage.close();
                            });
                            return;
                        }
                    }
                    Platform.runLater(() -> {
                        showInfo("Images exported successfully!");
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
