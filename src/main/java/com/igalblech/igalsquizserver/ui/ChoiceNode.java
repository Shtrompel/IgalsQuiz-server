package com.igalblech.igalsquizserver.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import lombok.Getter;

@Getter
public class ChoiceNode extends StackPane {

    private Media media;
    private Text textLabel;
    private ImageView backgroundImageView;

    public ChoiceNode(GridPane pane, String text, Image backgroundImage, Color color, Media media) {
        this.media = media;

        textLabel = new Text(text);
        backgroundImageView = new ImageView(backgroundImage);


        BackgroundFill backgroundFill = new BackgroundFill(color, null, null);
        Background background = new Background(backgroundFill);

        Pane imagePane = new Pane();
        imagePane.setBackground(background);

        backgroundImageView.setPreserveRatio(false);  // Allow the image to stretch
        //backgroundImageView.fitWidthProperty().bind(imagePane.widthProperty());
        backgroundImageView.setFitWidth(250);
        backgroundImageView.fitHeightProperty().bind(imagePane.heightProperty());

        imagePane.setMinHeight(100);
        imagePane.setMaxHeight(100);

        //backgroundImageView.setFitWidth(200);
        //backgroundImageView.setFitHeight(100);

        backgroundImageView.setSmooth(true);

        imagePane.getChildren().add(backgroundImageView);

        // Add both to the stack
        this.getChildren().addAll(imagePane, textLabel);

        //this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        textLabel.setId("answer-text");
        textLabel.setFill(Paint.valueOf("black"));
        textLabel.setStroke(Paint.valueOf("white"));
        textLabel.setStrokeWidth(1);
    }

    public void setText(String text) {
        textLabel.setText(text);
    }

    public void setBackgroundImage(Image image) {
        backgroundImageView.setImage(image);
    }

    public void highlight(Color color) {
        setStyle("-fx-border-color: " + color.toString().replace("0x", "#") + "; -fx-border-width: 3;");
    }

    public void resetHighlight() {
        setStyle("-fx-border-color: transparent; -fx-border-width: 2;");
    }
}
