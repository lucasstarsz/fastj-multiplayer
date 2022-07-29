package tech.fastj.partyhouse.ui;

import tech.fastj.graphics.game.Text2D;
import tech.fastj.graphics.ui.UIElement;

import tech.fastj.input.InputActionEvent;
import tech.fastj.systems.control.GameHandler;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SimpleManager;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class StatusBox extends UIElement<InputActionEvent> {

    public static final String DefaultStatSeparator = ": ";
    public static final String DefaultFormat = "%s" + DefaultStatSeparator + "%s";

    public static final String DefaultReadyStatus = "Ready";
    public static final String DefaultNotReadyStatus = "Not Ready";

    private final String name;
    private final String format;
    private final String readyStatus, notReadyStatus;

    private boolean currentStatus;
    private Text2D statDisplay;

    public StatusBox(GameHandler origin, String name, boolean currentStatus) {
        this(origin, name, currentStatus, DefaultReadyStatus, DefaultNotReadyStatus, DefaultFormat);
    }

    public StatusBox(GameHandler origin, String name, boolean currentStatus, String readyStatus, String notReadyStatus, String customFormat) {
        super(origin);
        this.name = name;
        this.readyStatus = readyStatus;
        this.notReadyStatus = notReadyStatus;
        this.currentStatus = currentStatus;
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

    public String getReadyStatus() {
        return readyStatus;
    }

    public String getNotReadyStatus() {
        return notReadyStatus;
    }

    public boolean getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(boolean currentStatus) {
        this.currentStatus = currentStatus;
        updateStatDisplay();
    }

    private void updateStatDisplay() {
        String formattedStats = String.format(format, name, currentStatus ? readyStatus : notReadyStatus);
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
        super.destroyTheRest(origin);
        currentStatus = false;
        statDisplay = null;
    }
}
