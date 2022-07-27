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
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SimpleManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link UIElement} that can be assigned an action on left click.
 *
 * @author Andrew Dey
 * @since 1.0.0
 */
public class BetterButton extends UIElement<MouseButtonEvent> implements MouseActionListener {

    /** The default size of a {@link BetterButton}: (100f, 25f). */
    public static final Pointf DefaultSize = new Pointf(100f, 25f);
    /** The default text value of a {@link BetterButton}: an empty string. */
    public static final String DefaultText = "";
    /** {@link Paint} representing the default color value of {@code (192, 192, 192)}. */
    public static final Paint DefaultFill = Color.lightGray;
    /** {@link Color} representing the default text color value as the color black. */
    public static final Color DefaultTextColor = Color.black;
    /** {@link Stroke} representing the default outline stroke value as a 5px outline with round edges. */
    public static final BasicStroke DefaultOutlineStroke = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0f);
    /** {@link Color} representing the default outline color value as the color black. */
    public static final Color DefaultOutlineColor = Color.black;
    /** {@link Font} representing the default font of {@code Tahoma 16px}. */
    public static final Font DefaultFont = new Font("Tahoma", Font.PLAIN, 16);

    private static final BufferedImage GraphicsHelper = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);

    private Paint paint;
    private Color textColor;
    private Color outlineColor;
    private BasicStroke outlineStroke;

    private Font font;
    private String text;
    private Rectangle2D.Float textBounds;
    private boolean hasMetrics;

    private boolean isMouseHovering;

    protected final List<Consumer<MouseMotionEvent>> onEnterHoverEvents;
    protected final List<Consumer<MouseMotionEvent>> onExitHoverEvents;

    /**
     * Constructs a button with a default location and size.
     *
     * @param origin The scene to add the button as a gui object to.
     */
    public BetterButton(Scene origin) {
        this(origin, Transform2D.DefaultTranslation, DefaultSize);
    }

    /**
     * Constructs a button with a default location and size.
     *
     * @param origin The simple manager to add the button as a gui object to.
     */
    public BetterButton(SimpleManager origin) {
        this(origin, Transform2D.DefaultTranslation, DefaultSize);
    }

    /**
     * Constructs a button with the specified location and initial size.
     *
     * @param origin      The scene to add the button as a gui object to.
     * @param location    The location to create the button at.
     * @param initialSize The initial size of the button, though the button will get larger if the text outgrows it.
     */
    public BetterButton(Scene origin, Pointf location, Pointf initialSize) {
        super(origin);
        if (initialSize.x < Maths.FloatPrecision || initialSize.y < Maths.FloatPrecision) {
            throw new IllegalArgumentException(
                "The size " + initialSize + " is too small." + System.lineSeparator() +
                    "The minimum size in both x and y directions is " + Maths.FloatPrecision + "."
            );
        }

        super.setOnActionCondition(event -> Mouse.interactsWith(BetterButton.this, MouseAction.Press) && Mouse.isMouseButtonPressed(MouseButtons.Left));

        Pointf[] buttonCoords = DrawUtil.createBox(Pointf.origin(), initialSize);
        super.setCollisionPath(DrawUtil.createPath(buttonCoords));

        this.paint = DefaultFill;
        this.font = DefaultFont;
        this.text = DefaultText;
        this.textColor = DefaultTextColor;
        this.outlineStroke = DefaultOutlineStroke;
        this.outlineColor = DefaultOutlineColor;
        this.onEnterHoverEvents = new ArrayList<>();
        this.onExitHoverEvents = new ArrayList<>();

        translate(location);
        Graphics2D graphics = GraphicsHelper.createGraphics();
        setMetrics(graphics);
        graphics.dispose();

        origin.inputManager.addMouseActionListener(this);
    }

    /**
     * Constructs a button with the specified location and initial size.
     *
     * @param origin      The simple manager to add the button as a gui object to.
     * @param location    The location to create the button at.
     * @param initialSize The initial size of the button, though the button will get larger if the text outgrows it.
     */
    public BetterButton(SimpleManager origin, Pointf location, Pointf initialSize) {
        super(origin);
        if (initialSize.x < Maths.FloatPrecision || initialSize.y < Maths.FloatPrecision) {
            throw new IllegalArgumentException(
                "The size " + initialSize + " is too small." + System.lineSeparator() +
                    "The minimum size in both x and y directions is " + Maths.FloatPrecision + "."
            );
        }

        super.setOnActionCondition(event -> Mouse.interactsWith(BetterButton.this, MouseAction.Press) && Mouse.isMouseButtonPressed(MouseButtons.Left));

        Pointf[] buttonCoords = DrawUtil.createBox(Pointf.origin(), initialSize);
        super.setCollisionPath(DrawUtil.createPath(buttonCoords));

        this.paint = DefaultFill;
        this.font = DefaultFont;
        this.text = DefaultText;
        this.textColor = DefaultTextColor;
        this.outlineStroke = DefaultOutlineStroke;
        this.outlineColor = DefaultOutlineColor;
        this.onEnterHoverEvents = new ArrayList<>();
        this.onExitHoverEvents = new ArrayList<>();

        translate(location);
        Graphics2D graphics = GraphicsHelper.createGraphics();
        setMetrics(graphics);
        graphics.dispose();

        origin.inputManager.addMouseActionListener(this);
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
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setFill(Paint paint) {
        this.paint = paint;
        return this;
    }

    /**
     * Gets the text for the button.
     *
     * @return The Button's text.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text for the button.
     *
     * @param text The new text for the button.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setText(String text) {
        this.text = text;
        Graphics2D graphics = GraphicsHelper.createGraphics();
        setMetrics(graphics);
        graphics.dispose();

        return this;
    }

    /**
     * Gets the {@link Font} object for the button.
     *
     * @return The Button's {@code Font}.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets the {@link Font} for the button.
     *
     * @param font The new {@code Font} object for the button.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setFont(Font font) {
        this.font = font;
        Graphics2D graphics = GraphicsHelper.createGraphics();
        setMetrics(graphics);
        graphics.dispose();

        return this;
    }

    /**
     * Sets the polygon's outline color.
     *
     * @param newOutlineColor The outline {@code Color} to be used for the polygon.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setOutlineColor(Color newOutlineColor) {
        outlineColor = newOutlineColor;
        return this;
    }

    /**
     * Sets the polygon's text color.
     *
     * @param newTextColor The text {@code Color} to be used for the polygon.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setTextColor(Color newTextColor) {
        textColor = newTextColor;
        return this;
    }

    /**
     * Sets the polygon's outline stroke.
     *
     * @param newOutlineStroke The outline {@code BasicStroke} to be used for the polygon.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setOutlineStroke(BasicStroke newOutlineStroke) {
        outlineStroke = newOutlineStroke;
        return this;
    }

    /**
     * Sets the polygon's outline stroke and color.
     *
     * @param newOutlineStroke The outline {@code BasicStroke} to be used for the polygon.
     * @param newOutlineColor  The outline {@code Color} to be used for the polygon.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setOutline(BasicStroke newOutlineStroke, Color newOutlineColor) {
        outlineStroke = newOutlineStroke;
        outlineColor = newOutlineColor;
        return this;
    }

    /**
     * Gets the {@link BetterButton}'s text color.
     *
     * @return The {@link BetterButton}'s text {@code Color}.
     */
    public Color getTextColor() {
        return textColor;
    }

    /**
     * Gets the {@link BetterButton}'s outline color.
     *
     * @return The {@link BetterButton}'s outline {@code Color}.
     */
    public Color getOutlineColor() {
        return outlineColor;
    }

    /**
     * Gets the {@link BetterButton}'s outline stroke.
     *
     * @return The {@link BetterButton}'s outline {@code BasicStroke}.
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
     * @return The {@link BetterButton}, for method chaining.
     */
    @Override
    public BetterButton setOnAction(Consumer<MouseButtonEvent> action) {
        onActionEvents.clear();
        onActionEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onAction} events.
     *
     * @param action The action to add.
     * @return The {@link BetterButton}, for method chaining.
     */
    @Override
    public BetterButton addOnAction(Consumer<MouseButtonEvent> action) {
        onActionEvents.add(action);
        return this;
    }

    /**
     * Sets the Button's {@code onHoverEnter} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setOnHoverEnter(Consumer<MouseMotionEvent> action) {
        onEnterHoverEvents.clear();
        onEnterHoverEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onHoverEnter} events.
     *
     * @param action The action to add.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton addOnHoverEnter(Consumer<MouseMotionEvent> action) {
        onEnterHoverEvents.add(action);
        return this;
    }

    /**
     * Sets the Button's {@code onHoverExit} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton setOnHoverExit(Consumer<MouseMotionEvent> action) {
        onExitHoverEvents.clear();
        onExitHoverEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onHoverExit} events.
     *
     * @param action The action to add.
     * @return The {@link BetterButton}, for method chaining.
     */
    public BetterButton addOnHoverExit(Consumer<MouseMotionEvent> action) {
        onExitHoverEvents.add(action);
        return this;
    }

    @Override
    public void render(Graphics2D g) {
        if (!hasMetrics) {
            setMetrics(g);
        }

        AffineTransform oldTransform = (AffineTransform) g.getTransform().clone();
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();
        Rectangle2D.Float renderCopy = (Rectangle2D.Float) collisionPath.getBounds2D();

        g.transform(getTransformation());

        g.setPaint(paint);
        g.fill(renderCopy);
        g.setStroke(outlineStroke);
        g.setPaint(outlineColor);
        g.draw(renderCopy);

        g.setStroke(oldStroke);
        g.setFont(font);
        g.drawString(text, textBounds.x, textBounds.y);

        g.setPaint(oldPaint);
        g.setFont(oldFont);
        g.setTransform(oldTransform);
    }

    @Override
    public void destroy(Scene origin) {
        super.destroyTheRest(origin);
        text = DefaultText;
        font = DefaultFont;
        paint = DefaultFill;
        outlineColor = DefaultOutlineColor;
        outlineStroke = DefaultOutlineStroke;
        origin.inputManager.removeMouseActionListener(this);
    }

    @Override
    public void destroy(SimpleManager origin) {
        super.destroyTheRest(origin);
        text = DefaultText;
        font = DefaultFont;
        paint = DefaultFill;
        outlineColor = DefaultOutlineColor;
        outlineStroke = DefaultOutlineStroke;
        origin.inputManager.removeMouseActionListener(this);
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
        if (!isMouseHovering && Mouse.interactsWith(BetterButton.this, MouseAction.Move)) {
            isMouseHovering = true;
            for (Consumer<MouseMotionEvent> action : onEnterHoverEvents) {
                action.accept(mouseMotionEvent);
            }
            mouseMotionEvent.consume();
        } else if (isMouseHovering && !Mouse.interactsWith(BetterButton.this, MouseAction.Move)) {
            isMouseHovering = false;
            for (Consumer<MouseMotionEvent> action : onExitHoverEvents) {
                action.accept(mouseMotionEvent);
            }
            mouseMotionEvent.consume();
        }
    }

    /**
     * Sets up the necessary boundaries for creating text metrics, and aligns the text with the button.
     * <p>
     * If the text metrics show that the text does not fit in the button, the button will be resized to fit the text.
     *
     * @param g {@code Graphics2D} object that the {@code Text2D} is rendered on.
     */
    private void setMetrics(Graphics2D g) {
        hasMetrics = false;

        FontMetrics fm = g.getFontMetrics(font);

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        Rectangle2D.Float renderPathBounds = (Rectangle2D.Float) collisionPath.getBounds2D();

        textBounds = new Rectangle2D.Float(
            (renderPathBounds.width - textWidth) / 2f,
            textHeight,
            textWidth,
            textHeight
        );

        Rectangle2D.Float newPathBounds = (Rectangle2D.Float) super.collisionPath.getBounds2D();
        if (renderPathBounds.width < textBounds.width) {
            float diff = (textBounds.width - renderPathBounds.width) / 2f;
            newPathBounds.width = textBounds.width + diff;
        }

        if (renderPathBounds.height < textBounds.height) {
            float diff = (textBounds.height - renderPathBounds.height) / 2f;
            newPathBounds.height = textBounds.height + diff;
        }

        super.setCollisionPath(DrawUtil.createPath(DrawUtil.createBox(newPathBounds)));

        g.dispose();
        hasMetrics = true;
    }
}
