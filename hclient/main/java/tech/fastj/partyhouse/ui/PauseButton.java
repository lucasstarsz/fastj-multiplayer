package tech.fastj.partyhouse.ui;

import tech.fastj.math.Maths;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.ui.UIElement;
import tech.fastj.graphics.util.DrawUtil;

import tech.fastj.input.mouse.Mouse;
import tech.fastj.input.mouse.MouseAction;
import tech.fastj.input.mouse.MouseActionListener;
import tech.fastj.input.mouse.MouseButtons;
import tech.fastj.input.mouse.events.MouseButtonEvent;
import tech.fastj.input.mouse.events.MouseMotionEvent;
import tech.fastj.systems.control.GameHandler;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SimpleManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PauseButton extends UIElement<MouseButtonEvent> implements MouseActionListener {

    /** The default size of a {@link PauseButton}: (100f, 25f). */
    public static final Pointf DefaultSize = new Pointf(100f, 25f);
    /** {@link Paint} representing the default color value of {@code (192, 192, 192)}. */
    public static final Paint DefaultFill = Color.lightGray;
    /** {@link Stroke} representing the default outline stroke value as a 5px outline with round edges. */
    public static final BasicStroke DefaultOutlineStroke = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0f);
    /** {@link Color} representing the default outline color value as the color black. */
    public static final Color DefaultOutlineColor = Color.black;

    private Paint paint;
    private Color outlineColor;
    private BasicStroke outlineStroke;

    private boolean isMouseHovering;

    protected final List<Consumer<MouseMotionEvent>> onEnterHoverEvents;
    protected final List<Consumer<MouseMotionEvent>> onExitHoverEvents;

    /**
     * Constructs a button with a default location and size.
     *
     * @param origin The scene to add the button as a gui object to.
     */
    public PauseButton(Scene origin) {
        this(origin, Transform2D.DefaultTranslation, DefaultSize);
    }

    /**
     * Constructs a button with a default location and size.
     *
     * @param origin The simple manager to add the button as a gui object to.
     */
    public PauseButton(SimpleManager origin) {
        this(origin, Transform2D.DefaultTranslation, DefaultSize);
    }

    /**
     * Constructs a button with the specified location and initial size.
     *
     * @param origin      The scene to add the button as a gui object to.
     * @param location    The location to create the button at.
     * @param initialSize The initial size of the button, though the button will get larger if the text outgrows it.
     */
    public PauseButton(Scene origin, Pointf location, Pointf initialSize) {
        super(origin);
        if (initialSize.x < Maths.FloatPrecision || initialSize.y < Maths.FloatPrecision) {
            throw new IllegalArgumentException(
                "The size " + initialSize + " is too small." +
                    System.lineSeparator() +
                    "The minimum size in both x and y directions is " + Maths.FloatPrecision + "."
            );
        }

        super.setOnActionCondition(event -> Mouse.interactsWith(PauseButton.this, MouseAction.Press) && Mouse.isMouseButtonPressed(MouseButtons.Left));

        Pointf[] buttonCoords = DrawUtil.createBox(Pointf.origin(), initialSize);
        super.setCollisionPath(DrawUtil.createPath(buttonCoords));

        this.paint = DefaultFill;
        this.outlineStroke = DefaultOutlineStroke;
        this.outlineColor = DefaultOutlineColor;
        this.onEnterHoverEvents = new ArrayList<>();
        this.onExitHoverEvents = new ArrayList<>();

        translate(location);
        origin.inputManager().addMouseActionListener(this);
    }

    /**
     * Constructs a button with the specified location and initial size.
     *
     * @param origin      The simple manager to add the button as a gui object to.
     * @param location    The location to create the button at.
     * @param initialSize The initial size of the button, though the button will get larger if the text outgrows it.
     */
    public PauseButton(SimpleManager origin, Pointf location, Pointf initialSize) {
        super(origin);
        if (initialSize.x < Maths.FloatPrecision || initialSize.y < Maths.FloatPrecision) {
            throw new IllegalArgumentException(
                "The size " + initialSize + " is too small." +
                    System.lineSeparator() +
                    "The minimum size in both x and y directions is " + Maths.FloatPrecision + "."
            );
        }

        super.setOnActionCondition(event -> Mouse.interactsWith(PauseButton.this, MouseAction.Press) && Mouse.isMouseButtonPressed(MouseButtons.Left));

        Pointf[] buttonCoords = DrawUtil.createBox(Pointf.origin(), initialSize);
        super.setCollisionPath(DrawUtil.createPath(buttonCoords));

        this.paint = DefaultFill;
        this.outlineStroke = DefaultOutlineStroke;
        this.outlineColor = DefaultOutlineColor;
        this.onEnterHoverEvents = new ArrayList<>();
        this.onExitHoverEvents = new ArrayList<>();

        translate(location);

        origin.inputManager().addMouseActionListener(this);
    }

    /**
     * Gets the {@link Paint} object for the button.
     *
     * @return The Button's {@code Paint}.
     */
    public Paint getFill() {
        return paint;
    }

    /**
     * Sets the {@link Paint} object for the button.
     *
     * @param paint The new paint for the button.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton setFill(Paint paint) {
        this.paint = paint;
        return this;
    }

    /**
     * Sets the polygon's outline color.
     *
     * @param newOutlineColor The outline {@code Color} to be used for the polygon.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton setOutlineColor(Color newOutlineColor) {
        outlineColor = newOutlineColor;
        return this;
    }

    /**
     * Sets the polygon's outline stroke.
     *
     * @param newOutlineStroke The outline {@code BasicStroke} to be used for the polygon.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton setOutlineStroke(BasicStroke newOutlineStroke) {
        outlineStroke = newOutlineStroke;
        return this;
    }

    /**
     * Sets the polygon's outline stroke and color.
     *
     * @param newOutlineStroke The outline {@code BasicStroke} to be used for the polygon.
     * @param newOutlineColor  The outline {@code Color} to be used for the polygon.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton setOutline(BasicStroke newOutlineStroke, Color newOutlineColor) {
        outlineStroke = newOutlineStroke;
        outlineColor = newOutlineColor;
        return this;
    }

    /**
     * Gets the {@link PauseButton}'s outline color.
     *
     * @return The {@link PauseButton}'s outline {@code Color}.
     */
    public Color getOutlineColor() {
        return outlineColor;
    }

    /**
     * Gets the {@link PauseButton}'s outline stroke.
     *
     * @return The {@link PauseButton}'s outline {@code BasicStroke}.
     */
    public BasicStroke getOutlineStroke() {
        return outlineStroke;
    }

    /**
     * Gets whether the mouse is currently hovering on the button.
     *
     * @return Whether the mouse is currently hovering on the button.
     */
    public boolean isMouseHovering() {
        return isMouseHovering;
    }

    /**
     * Sets the Button's {@code onAction} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@link PauseButton}, for method chaining.
     */
    @Override
    public PauseButton setOnAction(Consumer<MouseButtonEvent> action) {
        onActionEvents.clear();
        onActionEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onAction} events.
     *
     * @param action The action to add.
     * @return The {@link PauseButton}, for method chaining.
     */
    @Override
    public PauseButton addOnAction(Consumer<MouseButtonEvent> action) {
        onActionEvents.add(action);
        return this;
    }

    /**
     * Sets the Button's {@code onHoverEnter} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton setOnHoverEnter(Consumer<MouseMotionEvent> action) {
        onEnterHoverEvents.clear();
        onEnterHoverEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onHoverEnter} events.
     *
     * @param action The action to add.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton addOnHoverEnter(Consumer<MouseMotionEvent> action) {
        onEnterHoverEvents.add(action);
        return this;
    }

    /**
     * Sets the Button's {@code onHoverExit} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton setOnHoverExit(Consumer<MouseMotionEvent> action) {
        onExitHoverEvents.clear();
        onExitHoverEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onHoverExit} events.
     *
     * @param action The action to add.
     * @return The {@link PauseButton}, for method chaining.
     */
    public PauseButton addOnHoverExit(Consumer<MouseMotionEvent> action) {
        onExitHoverEvents.add(action);
        return this;
    }

    @Override
    public void render(Graphics2D g) {
        AffineTransform oldTransform = (AffineTransform) g.getTransform().clone();
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();
        Rectangle2D.Float renderCopyLeft = (Rectangle2D.Float) collisionPath.getBounds2D();
        Rectangle2D.Float renderCopyRight = (Rectangle2D.Float) collisionPath.getBounds2D();

        renderCopyLeft.width *= 0.4;
        renderCopyRight.x += renderCopyRight.width * 0.6;
        renderCopyRight.width *= 0.4;

        g.transform(getTransformation());

        g.setPaint(paint);
        g.fill(renderCopyLeft);
        g.fill(renderCopyRight);
        g.setStroke(outlineStroke);
        g.setPaint(outlineColor);
        g.draw(renderCopyLeft);
        g.draw(renderCopyRight);

        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
        g.setFont(oldFont);
        g.setTransform(oldTransform);
    }

    @Override
    public void destroy(GameHandler origin) {
        super.destroyTheRest(origin);
        paint = DefaultFill;
        outlineColor = DefaultOutlineColor;
        outlineStroke = DefaultOutlineStroke;
        origin.inputManager().removeMouseActionListener(this);
    }

    /**
     * Fires the button's {@code onAction} event(s), if its condition is met.
     *
     * @param mouseButtonEvent The mouse event causing the {@code onAction} event(s) to be fired.
     */
    @Override
    public void onMousePressed(MouseButtonEvent mouseButtonEvent) {
        if (onActionCondition.condition(mouseButtonEvent)) {
            for (Consumer<MouseButtonEvent> action : onActionEvents) {
                action.accept(mouseButtonEvent);
            }
            mouseButtonEvent.consume();
        }
    }

    @Override
    public void onMouseMoved(MouseMotionEvent mouseMotionEvent) {
        if (!isMouseHovering && Mouse.interactsWith(PauseButton.this, MouseAction.Move)) {
            isMouseHovering = true;
            for (Consumer<MouseMotionEvent> action : onEnterHoverEvents) {
                action.accept(mouseMotionEvent);
            }
            mouseMotionEvent.consume();
        } else if (isMouseHovering && !Mouse.interactsWith(PauseButton.this, MouseAction.Move)) {
            isMouseHovering = false;
            for (Consumer<MouseMotionEvent> action : onExitHoverEvents) {
                action.accept(mouseMotionEvent);
            }
            mouseMotionEvent.consume();
        }
    }
}
