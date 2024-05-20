package lol.koblizek.bytelens.api.ui;

import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lol.koblizek.bytelens.api.ToolWindow;

public class SideToolButton extends ToggleButton {

    private final ToolWindow tw;
    private SidePane controlledPane;

    public SideToolButton(ToolWindow tw, SidePane controlledPane) {
        super();
        this.tw = tw;
        ImageView imageView = new ImageView(tw.icon());
        imageView.setFitWidth(16);
        imageView.setFitHeight(16);
        setGraphic(imageView);
        getStyleClass().add("tool-bar-button");
        this.controlledPane = controlledPane;
    }

    public ToolWindow getToolWindow() {
        return tw;
    }

    public SidePane getControlledPane() {
        return controlledPane;
    }

    public void setControlledPane(SidePane controlledPane) {
        this.controlledPane = controlledPane;
    }
}
