package tech.fastj.partyhouse.ui;

import tech.fastj.graphics.game.Text2D;
import tech.fastj.graphics.ui.UIElement;

import tech.fastj.input.InputActionEvent;
import tech.fastj.systems.control.GameHandler;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class PercentageBox<T extends Number> extends UIElement<InputActionEvent> {

    public static final String DefaultStatSeparator = "/";
    public static final String DefaultFormat = "%s%s" + DefaultStatSeparator + "%s";

    private final T maxValue;
    private final String format;
    private final String name;

    private T currentValue;
    private Text2D statDisplay;

    public PercentageBox(GameHandler origin, T initialValue, T maxValue, String name) {
        this(origin, initialValue, maxValue, name, DefaultFormat);
    }

    public PercentageBox(GameHandler origin, T initialValue, T maxValue, String name, String customFormat) {
        super(origin);
        this.currentValue = initialValue;
        this.maxValue = maxValue;
        this.name = name;
        this.format = customFormat;

        updateStatDisplay();
        setCollisionPath((Path2D.Float) statDisplay.getCollisionPath().clone());
    }

    public T getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(T currentValue) {
        this.currentValue = currentValue;
        updateStatDisplay();
    }

    public T getMaxValue() {
        return maxValue;
    }

    public String getFormat() {
        return format;
    }

    public Text2D getStatDisplay() {
        return statDisplay;
    }

    private void updateStatDisplay() {
        String formattedStats = String.format(format, name, currentValue, maxValue);
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
        currentValue = null;
        statDisplay = null;
    }
}
