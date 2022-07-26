package tech.fastj.partyhouse.ui;

import tech.fastj.engine.FastJEngine;
import tech.fastj.graphics.game.Polygon2D;
import tech.fastj.graphics.game.RenderStyle;
import tech.fastj.graphics.ui.EventCondition;
import tech.fastj.graphics.ui.UIElement;
import tech.fastj.graphics.util.DrawUtil;
import tech.fastj.input.mouse.Mouse;
import tech.fastj.input.mouse.MouseAction;
import tech.fastj.input.mouse.MouseActionListener;
import tech.fastj.input.mouse.MouseButtons;
import tech.fastj.input.mouse.events.MouseButtonEvent;
import tech.fastj.math.Pointf;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SimpleManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Consumer;

public class ArrowButton extends UIElement<MouseButtonEvent> implements MouseActionListener {

    public static final String DefaultText = "";
    public static final Paint DefaultFill = Color.lightGray;
    public static final Font DefaultFont = new Font("Tahoma", Font.PLAIN, 16);

    private final List<String> options;
    private int selectedOption;
    private final Polygon2D arrowLeft, arrowRight;

    private Paint paint;
    private Path2D.Float renderPath;

    private Font font;
    private String text;
    private Rectangle2D.Float textBounds;
    private boolean hasMetrics;
    private final EventCondition eventCondition = event -> {
        if (Mouse.interactsWith(ArrowButton.this.arrowLeft, MouseAction.Press) || Mouse.interactsWith(ArrowButton.this.arrowRight, MouseAction.Press)) {
            return Mouse.isMouseButtonPressed(MouseButtons.Left);
        }
        return false;
    };

    public ArrowButton(Scene origin, Pointf location, Pointf initialSize, List<String> options, int selectedOption) {
        super(origin);
        super.setOnActionCondition(eventCondition);

        Pointf[] buttonCoords = DrawUtil.createBox(Pointf.origin(), initialSize);
        renderPath = DrawUtil.createPath(buttonCoords);
        super.setCollisionPath(renderPath);

        Pointf arrowPadding = Pointf.multiply(initialSize, 0.1f, 0.25f);
        Pointf arrowCenter = Pointf.multiply(initialSize, 0.5f);
        float arrowLength = initialSize.y - (arrowPadding.y * 2f);
        float arrowHorizontalLength = (float) Math.sqrt(arrowLength * arrowLength - (arrowLength / 2f) * (arrowLength / 2f));

        Pointf[] arrowLeftCoords = {
                new Pointf(arrowPadding.x, arrowCenter.y),
                new Pointf(arrowPadding.x + arrowHorizontalLength, arrowPadding.y),
                new Pointf(arrowPadding.x + arrowHorizontalLength, initialSize.y - arrowPadding.y)
        };
        arrowLeft = Polygon2D.create(arrowLeftCoords)
                .withRenderStyle(RenderStyle.FillAndOutline)
                .withFill(Color.white)
                .withOutline(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND), Color.black)
                .build();

        Pointf[] arrowRightCoords = {
                new Pointf(initialSize.x - arrowPadding.x, arrowCenter.y),
                new Pointf(initialSize.x - (arrowPadding.x + arrowHorizontalLength), arrowPadding.y),
                new Pointf(initialSize.x - (arrowPadding.x + arrowHorizontalLength), initialSize.y - arrowPadding.y)
        };
        arrowRight = Polygon2D.create(arrowRightCoords)
                .withRenderStyle(RenderStyle.FillAndOutline)
                .withFill(Color.white)
                .withOutline(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND), Color.black)
                .build();

        this.paint = DefaultFill;
        this.font = DefaultFont;
        this.text = DefaultText;

        translate(location);
        setMetrics(FastJEngine.getCanvas().getGraphics());
        this.options = options;
        this.selectedOption = selectedOption;
        setText(options.get(selectedOption));

        origin.inputManager.addMouseActionListener(this);
    }

    @Override
    public void translate(Pointf translationMod) {
        super.translate(translationMod);
        arrowLeft.translate(translationMod);
        arrowRight.translate(translationMod);
    }

    public Paint getFill() {
        return paint;
    }

    public ArrowButton setFill(Paint paint) {
        this.paint = paint;
        return this;
    }

    public String getText() {
        return text;
    }

    private void setText(String text) {
        this.text = text;
        setMetrics(FastJEngine.getCanvas().getGraphics());
    }

    public Font getFont() {
        return font;
    }

    public ArrowButton setFont(Font font) {
        this.font = font;
        setMetrics(FastJEngine.getCanvas().getGraphics());
        return this;
    }

    public int getSelectedOption() {
        return selectedOption;
    }

    public Polygon2D getArrowLeft() {
        return arrowLeft;
    }

    public Polygon2D getArrowRight() {
        return arrowRight;
    }

    @Override
    public void onMousePressed(MouseButtonEvent mouseButtonEvent) {
        if (!eventCondition.condition(mouseButtonEvent)) {
            return;
        }
        if (mouseButtonEvent.getMouseButton() != MouseEvent.BUTTON1) {
            return;
        }

        Pointf mouseLocation = Mouse.getMouseLocation();
        if (mouseLocation.intersects(arrowLeft.getCollisionPath())) {
            selectedOption--;
            if (selectedOption < 0) {
                selectedOption += options.size();
            }
        } else if (mouseLocation.intersects(arrowRight.getCollisionPath())) {
            selectedOption++;
            if (selectedOption >= options.size()) {
                selectedOption -= options.size();
            }
        }

        setText(options.get(selectedOption));
        for (Consumer<MouseButtonEvent> onActionEvent : onActionEvents) {
            onActionEvent.accept(mouseButtonEvent);
        }
        mouseButtonEvent.consume();
    }

    @Override
    public void render(Graphics2D g) {
        if (!shouldRender()) {
            return;
        }

        AffineTransform oldTransform = (AffineTransform) g.getTransform().clone();
        Paint oldPaint = g.getPaint();
        Font oldFont = g.getFont();

        g.transform(getTransformation());

        Rectangle2D.Float renderCopy = (Rectangle2D.Float) renderPath.getBounds2D();

        g.setPaint(paint);
        g.fill(renderCopy);
        g.setPaint(Color.black);
        g.draw(renderCopy);

        if (!hasMetrics) {
            setMetrics(g);
        }

        g.setFont(font);
        g.drawString(text, textBounds.x, textBounds.y * 1.5f);

        g.setTransform(oldTransform);

        arrowLeft.render(g);
        arrowRight.render(g);

        g.setPaint(oldPaint);
        g.setFont(oldFont);
    }

    @Override
    public void destroy(Scene origin) {
        super.destroyTheRest(origin);
        paint = null;
        renderPath = null;
        arrowLeft.destroy(origin);
        origin.inputManager.removeMouseActionListener(this);
    }

    @Override
    public void destroy(SimpleManager origin) {
        super.destroyTheRest(origin);
        paint = null;
        renderPath = null;
        arrowLeft.destroy(origin);
        origin.inputManager.removeMouseActionListener(this);
    }

    private void setMetrics(Graphics2D g) {
        hasMetrics = false;

        FontMetrics fm = g.getFontMetrics(font);

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        Rectangle2D.Float renderPathBounds = (Rectangle2D.Float) collisionPath.getBounds2D();

        textBounds = new Rectangle2D.Float(
                (renderPathBounds.width - textWidth) / 2f,
                textHeight / 1.25f,
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
