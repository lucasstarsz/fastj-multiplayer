package tech.fastj.partyhouse.ui;

import tech.fastj.graphics.game.Text2D;
import tech.fastj.graphics.ui.UIElement;

import tech.fastj.input.InputActionEvent;
import tech.fastj.systems.control.GameHandler;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class ContentBox extends UIElement<InputActionEvent> {

    public static final String DefaultStatSeparator = ": ";
    public static final String DefaultFormat = "%s" + DefaultStatSeparator + "%s";
    public static final String DefaultContent = "None";

    private final String name;
    private final String format;
    private String content;
    private Text2D statDisplay;

    public ContentBox(GameHandler origin, String name) {
        this(origin, name, DefaultContent, DefaultFormat);
    }

    public ContentBox(GameHandler origin, String name, String content) {
        this(origin, name, content, DefaultFormat);
    }

    public ContentBox(GameHandler origin, String name, String content, String customFormat) {
        super(origin);
        this.name = name;
        this.content = content;
        this.format = customFormat;

        updateStatDisplay();
        setCollisionPath((Path2D.Float) statDisplay.getCollisionPath().clone());
    }

    public Text2D getStatDisplay() {
        return statDisplay;
    }

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        updateStatDisplay();
    }

    private void updateStatDisplay() {
        String formattedStats = String.format(format, name, content);
        if (statDisplay == null) {
            statDisplay = Text2D.create(formattedStats).build();
        } else {
            statDisplay.setText(formattedStats);
        }
    }

    @Override
    public void render(Graphics2D g) {
        AffineTransform oldTransform = (AffineTransform) g.getTransform().clone();
        g.transform(getTransformation());

        statDisplay.render(g);

        g.setTransform(oldTransform);
    }

    @Override
    public void destroy(GameHandler origin) {
        destroyTheRest(origin);
        statDisplay = null;
        content = null;
    }
}
