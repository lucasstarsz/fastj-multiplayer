package tech.fastj.partyhouse.ui;

import tech.fastj.engine.FastJEngine;
import tech.fastj.math.Maths;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.game.GameObject;

import tech.fastj.systems.behaviors.Behavior;
import tech.fastj.systems.behaviors.BehaviorHandler;
import tech.fastj.systems.control.GameHandler;
import tech.fastj.systems.control.LogicManager;
import tech.fastj.systems.control.SceneManager;
import tech.fastj.systems.control.SimpleManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

import tech.fastj.gameloop.CoreLoopState;
import tech.fastj.partyhouse.util.ExtraMaths;

public class Notice extends GameObject implements Behavior {

    /** {@link Color} representing the default color value of {@code (0, 0, 0)}. */
    public static final Color DefaultFill = Color.black;
    /** {@link Font} representing the default font of {@code Tahoma 16px}. */
    public static final Font DefaultFont = new Font("Tahoma", Font.PLAIN, 16);
    /** {@code String} representing default text -- an empty string. */
    public static final String DefaultText = "";

    private static final Pointf OriginInstance = Pointf.origin();
    private static final BufferedImage GraphicsHelper = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);

    private String text;
    private Color fillColor;
    private Font font;
    private final Pointf location;
    private final long timestamp = System.nanoTime();
    private final long endTimestamp = timestamp + (3 * 1_000_000_000L);

    private boolean hasMetrics;

    public Notice(String text, Pointf location, BehaviorHandler handler) {
        this.text = Objects.requireNonNullElse(text, DefaultText);
        this.location = location;

        setFont(DefaultFont);
        setFill(DefaultFill);

        addBehavior(this, Objects.requireNonNull(handler));
    }

    /**
     * Gets the {@code Notice}'s displayed text.
     *
     * @return Returns a String that contains the text displayed.
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the {@code Notice}'s fill {@code Color}.
     *
     * @return Returns the Color value for this Notice.
     */
    public Color getFill() {
        return fillColor;
    }

    /**
     * Gets the {@code Notice}'s {@code Font}.
     *
     * @return Returns the specified Font value for this Notice.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets the {@code Notice}'s text.
     *
     * @param newText The new text value.
     * @return The {@code Notice} instance, for method chaining.
     */
    public Notice setText(String newText) {
        text = Objects.requireNonNullElse(newText, DefaultText);
        Graphics2D graphics = GraphicsHelper.createGraphics();
        setMetrics(graphics);
        graphics.dispose();

        return this;
    }

    /**
     * Sets the {@code Notice}'s {@code Color}.
     *
     * @param newColor The new {@code Color} value.
     * @return The {@code Notice} instance, for method chaining.
     */
    public Notice setFill(Color newColor) {
        fillColor = newColor;
        return this;
    }

    /**
     * Sets the {@code Notice}'s {@code Font}.
     *
     * @param newFont The new {@code Font} value.
     * @return The {@code Notice} instance, for method chaining.
     */
    public Notice setFont(Font newFont) {
        font = newFont;
        Graphics2D graphics = GraphicsHelper.createGraphics();
        setMetrics(graphics);
        graphics.dispose();

        return this;
    }

    @Override
    public void render(Graphics2D g) {
        if (!hasMetrics) {
            setMetrics(g);
        }

        AffineTransform oldTransform = (AffineTransform) g.getTransform().clone();
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();

        g.transform(getTransformation());
        g.setFont(font);
        g.setColor(fillColor);

        g.drawString(text, OriginInstance.x, font.getSize2D());

        g.setTransform(oldTransform);
        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    @Override
    public void destroy(GameHandler origin) {
        text = DefaultText;
        fillColor = DefaultFill;
        font = DefaultFont;
        hasMetrics = false;

        super.destroyTheRest(origin);
    }

    /**
     * Sets up the necessary boundaries for creating the {@code Notice}'s metrics.
     * <p>
     * This also sets the resulting metrics as the {@code Notice}'s collision path.
     *
     * @param g {@code Graphics2D} object that the {@code Notice} is rendered on.
     */
    private void setMetrics(Graphics2D g) {
        hasMetrics = false;

        FontMetrics fm = g.getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        final Rectangle2D.Float bounds = new Rectangle2D.Float(
            Transform2D.DefaultTranslation.x,
            Transform2D.DefaultTranslation.y,
            textWidth,
            textHeight
        );

        setCollisionPath(createMetricsPath(bounds));

        g.dispose();
        hasMetrics = true;
    }

    /**
     * Gets a {@code Path2D.Float} that is based on the parameter {@code Rectangle2D.Float}.
     *
     * @param rect The rectangle which the result {@code Path2D.Float} is based on.
     * @return The newly created {@code Path2D.Float}.
     */
    private Path2D.Float createMetricsPath(Rectangle2D.Float rect) {
        Path2D.Float result = new Path2D.Float();

        result.moveTo(rect.x, rect.y);
        result.lineTo(rect.x + rect.width, rect.y);
        result.lineTo(rect.x + rect.width, rect.y + rect.height);
        result.lineTo(rect.x, rect.y + rect.height);
        result.closePath();

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Notice notice = (Notice) o;
        return Objects.equals(text, notice.text)
            && Objects.equals(fillColor, notice.fillColor)
            && Objects.equals(font, notice.font);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text, fillColor, font, hasMetrics);
    }

    @Override
    public void init(GameObject gameObject) {
    }

    @Override
    public void fixedUpdate(GameObject gameObject) {
    }

    @Override
    public void update(GameObject gameObject) {
        if (fillColor.getAlpha() - 1 == 0) {
            FastJEngine.runLater(() -> {
                LogicManager logicManager = FastJEngine.getLogicManager();
                if (logicManager instanceof SceneManager sceneManager) {
                    gameObject.destroy(sceneManager.getCurrentScene());
                } else if (logicManager instanceof SimpleManager simpleManager) {
                    gameObject.destroy(simpleManager);
                }
            }, CoreLoopState.LateUpdate);
        } else {
            Color moreTransparentColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), fillColor.getAlpha() - 1);
            setFill(moreTransparentColor);

            float yTranslation = Maths.lerp(location.y, location.y + 250f, (float) ExtraMaths.normalize(System.nanoTime(), timestamp, endTimestamp));
            gameObject.setTranslation(Pointf.add(location, 0f, yTranslation));
        }
    }
}
