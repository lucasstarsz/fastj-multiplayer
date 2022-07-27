package tech.fastj.partyhouse.ui;

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

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class LinkText extends UIElement<MouseButtonEvent> implements MouseActionListener {

    /** The default text value of a {@link LinkText}: an empty string. */
    public static final String DefaultText = "";
    /** {@link Color} representing the default text color value as the color black. */
    public static final Color DefaultTextColor = Color.black;
    /** {@link Font} representing the default font of {@code Tahoma 16px}. */
    public static final Font DefaultFont = new Font("Tahoma", Font.PLAIN, 16);

    private static final BufferedImage GraphicsHelper = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
    private final URL linkURL;

    private Color textColor;
    private Font font;
    private String text;
    private Rectangle2D.Float textBounds;
    private boolean hasMetrics;

    private boolean isMouseHovering;

    protected final List<Consumer<MouseMotionEvent>> onEnterHoverEvents;
    protected final List<Consumer<MouseMotionEvent>> onExitHoverEvents;

    /**
     * Constructs a button with the specified location and initial size.
     *
     * @param origin The scene to add the button as a gui object to.
     */
    public LinkText(Scene origin, String text, URL linkURL) {
        super(origin);
        this.linkURL = linkURL;
        super.setOnActionCondition(event -> Mouse.interactsWith(LinkText.this, MouseAction.Press) && Mouse.isMouseButtonPressed(MouseButtons.Left));

        this.font = DefaultFont;
        this.text = Objects.requireNonNullElse(text, DefaultText);
        this.textColor = DefaultTextColor;
        this.onEnterHoverEvents = new ArrayList<>();
        this.onExitHoverEvents = new ArrayList<>();


        Graphics2D graphics = GraphicsHelper.createGraphics();
        FontMetrics fm = graphics.getFontMetrics(font);

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        setCollisionPath(DrawUtil.createPath(DrawUtil.createBox(new Rectangle2D.Float(0, 0, textWidth, textHeight))));

        origin.inputManager.addMouseActionListener(this);
    }

    /**
     * Gets the {@link Color} object for the button.
     *
     * @return The Button's {@code Color}.
     */
    public Color getFill() {
        return textColor;
    }

    /**
     * Sets the {@link Color} object for the button.
     *
     * @param textColor The new paint for the button.
     * @return The {@link LinkText}, for method chaining.
     */
    public LinkText setFill(Color textColor) {
        this.textColor = textColor;
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
     * @return The {@link LinkText}, for method chaining.
     */
    public LinkText setText(String text) {
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
     * @return The {@link LinkText}, for method chaining.
     */
    public LinkText setFont(Font font) {
        this.font = font;
        Graphics2D graphics = GraphicsHelper.createGraphics();
        setMetrics(graphics);
        graphics.dispose();

        return this;
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
     * @return The {@link LinkText}, for method chaining.
     */
    @Override
    public LinkText setOnAction(Consumer<MouseButtonEvent> action) {
        onActionEvents.clear();
        onActionEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onAction} events.
     *
     * @param action The action to add.
     * @return The {@link LinkText}, for method chaining.
     */
    @Override
    public LinkText addOnAction(Consumer<MouseButtonEvent> action) {
        onActionEvents.add(action);
        return this;
    }

    /**
     * Sets the Button's {@code onHoverEnter} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@link LinkText}, for method chaining.
     */
    public LinkText setOnHoverEnter(Consumer<MouseMotionEvent> action) {
        onEnterHoverEvents.clear();
        onEnterHoverEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onHoverEnter} events.
     *
     * @param action The action to add.
     * @return The {@link LinkText}, for method chaining.
     */
    public LinkText addOnHoverEnter(Consumer<MouseMotionEvent> action) {
        onEnterHoverEvents.add(action);
        return this;
    }

    /**
     * Sets the Button's {@code onHoverExit} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@link LinkText}, for method chaining.
     */
    public LinkText setOnHoverExit(Consumer<MouseMotionEvent> action) {
        onExitHoverEvents.clear();
        onExitHoverEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the Button's {@code onHoverExit} events.
     *
     * @param action The action to add.
     * @return The {@link LinkText}, for method chaining.
     */
    public LinkText addOnHoverExit(Consumer<MouseMotionEvent> action) {
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
        Font oldFont = g.getFont();

        g.transform(getTransformation());

        g.setPaint(textColor);
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
        textColor = DefaultTextColor;
        hasMetrics = false;
        origin.inputManager.removeMouseActionListener(this);
    }

    @Override
    public void destroy(SimpleManager origin) {
        super.destroyTheRest(origin);
        text = DefaultText;
        font = DefaultFont;
        textColor = DefaultTextColor;
        hasMetrics = false;
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
            openWebpage(linkURL);
            mouseButtonEvent.consume();
        }
    }

    @Override
    public void onMouseMoved(MouseMotionEvent mouseMotionEvent) {
        if (!isMouseHovering && Mouse.interactsWith(LinkText.this, MouseAction.Move)) {
            isMouseHovering = true;
            for (Consumer<MouseMotionEvent> action : onEnterHoverEvents) {
                action.accept(mouseMotionEvent);
            }

            mouseMotionEvent.consume();
        } else if (isMouseHovering && !Mouse.interactsWith(LinkText.this, MouseAction.Move)) {
            isMouseHovering = false;
            for (Consumer<MouseMotionEvent> action : onExitHoverEvents) {
                action.accept(mouseMotionEvent);
            }
            mouseMotionEvent.consume();
        }
    }

    public static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
        return false;
    }

    public static boolean openWebpage(URL url) {
        try {
            return openWebpage(url.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(exception);
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
