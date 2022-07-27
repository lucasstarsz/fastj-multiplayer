package tech.fastj.partyhouse.ui;

import tech.fastj.graphics.game.Text2D;
import tech.fastj.graphics.ui.UIElement;

import tech.fastj.input.mouse.Mouse;
import tech.fastj.input.mouse.MouseAction;
import tech.fastj.input.mouse.MouseActionListener;
import tech.fastj.input.mouse.events.MouseButtonEvent;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SimpleManager;

import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.function.Consumer;

import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Shapes;

public class LobbyContentBox extends UIElement<MouseButtonEvent> implements MouseActionListener {

    public static final String DefaultStatSeparator = ": ";
    public static final String DefaultFormat = "%s" + DefaultStatSeparator + "%s";

    private final String name;
    private final String format;
    private String content;
    private Text2D statDisplay;
    private final LobbyIdentifier lobby;
    private boolean isFocused;

    public LobbyContentBox(Scene origin, LobbyIdentifier lobby) {
        super(origin);
        this.lobby = lobby;
        this.name = "Lobby " + lobby.name();
        this.content = lobby.currentPlayerCount() + "/" + lobby.maxPlayers();

        this.format = DefaultFormat;
        super.setOnActionCondition(
                event -> Mouse.interactsWith(LobbyContentBox.this, MouseAction.Release)
        );

        origin.inputManager.addMouseActionListener(this);

        updateStatDisplay();
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

    public LobbyIdentifier getLobby() {
        return lobby;
    }

    public void setContent(String content) {
        this.content = content;
        updateStatDisplay();
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setFocused(boolean focused) {
        isFocused = focused;
    }

    private void updateStatDisplay() {
        String formattedStats = String.format(format, name, content);

        if (statDisplay == null) {
            statDisplay = Text2D.create(formattedStats).build();
        } else {
            statDisplay.setText(formattedStats);
        }

        setCollisionPath((Path2D.Float) statDisplay.getCollisionPath().clone());
    }

    @Override
    public void onMouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (onActionCondition.condition(mouseButtonEvent)) {
            isFocused = true;

            for (Consumer<MouseButtonEvent> action : onActionEvents) {
                action.accept(mouseButtonEvent);
            }

            mouseButtonEvent.consume();
        } else {
            isFocused = false;
        }
    }

    @Override
    public void render(Graphics2D g) {
        AffineTransform oldTransform = (AffineTransform) g.getTransform().clone();
        g.transform(getTransformation());
        statDisplay.render(g);

        if (isFocused) {
            Paint oldPaint = g.getPaint();
            Stroke oldStroke = g.getStroke();
            AffineTransform oldTransform2 = (AffineTransform) g.getTransform().clone();

            g.setColor(Colors.Snowy);
            g.setStroke(Shapes.ThickStroke);
            g.transform(statDisplay.getTransformation());

            g.draw(statDisplay.getCollisionPath());

            g.setTransform(oldTransform2);
            g.setStroke(oldStroke);
            g.setPaint(oldPaint);
        }

        g.setTransform(oldTransform);
    }

    @Override
    public void destroy(Scene origin) {
        destroyTheRest(origin);
        origin.inputManager.removeMouseActionListener(this);
        statDisplay = null;
        content = null;
    }

    @Override
    public void destroy(SimpleManager origin) {
        destroyTheRest(origin);
        origin.inputManager.removeMouseActionListener(this);
        statDisplay = null;
        content = null;
    }
}
