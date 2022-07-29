package tech.fastj.partyhouse.ui;

import tech.fastj.engine.FastJEngine;
import tech.fastj.math.Point;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.game.Polygon2D;
import tech.fastj.graphics.game.RenderStyle;
import tech.fastj.graphics.game.Text2D;
import tech.fastj.graphics.ui.UIElement;
import tech.fastj.graphics.util.DrawUtil;

import tech.fastj.input.mouse.events.MouseActionEvent;
import tech.fastj.systems.control.GameHandler;
import tech.fastj.systems.control.Scene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.Shapes;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPoints;
import tech.fastj.partyhousecore.Info;

public class ResultMenu extends UIElement<MouseActionEvent> {

    private Polygon2D alphaScreen;
    private Polygon2D backgroundScreen;
    private Text2D gameEndText;
    private Text2D winnerText;
    private Text2D nextText;
    private ContentBox playerScore;
    private List<ContentBox> otherPlayerScores;

    private int timeLeft;
    private ScheduledExecutorService timeLeftProgressor;
    private String nextTextString = "";

    public ResultMenu(GameHandler origin, ClientInfo winnerInfo) {
        super(origin);

        Pointf center = FastJEngine.getCanvas().getCanvasCenter();
        Point end = FastJEngine.getCanvas().getResolution().copy();
        Pointf[] backgroundMesh = DrawUtil.createBox(50f, 50f, Point.subtract(end, 120, 140).asPointf());
        Pointf[] alphaMesh = DrawUtil.createBox(0f, 0f, end.asPointf());

        setCollisionPath(DrawUtil.createPath(backgroundMesh));

        alphaScreen = Polygon2D.create(alphaMesh)
            .withFill(Colors.darkGray(25).darker().darker())
            .withRenderStyle(RenderStyle.Fill)
            .build();

        backgroundScreen = Polygon2D.create(backgroundMesh)
            .withFill(new Color(Color.lightGray.getRed(), Color.lightGray.getGreen(), Color.lightGray.getBlue(), 15))
            .withOutline(Shapes.ThickerRoundedStroke, Color.black)
            .withRenderStyle(RenderStyle.FillAndOutline)
            .build();

        gameEndText = Text2D.create("Game Finished!")
            .withFont(Fonts.TitleTextFont)
            .withTransform(Pointf.subtract(center, 160f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();

        winnerText = Text2D.create(winnerInfo.clientName() + " wins!")
            .withFont(Fonts.SubtitleTextFont)
            .withTransform(Pointf.subtract(center, 125f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();

        playerScore = new ContentBox(
            origin,
            "Points Gained",
            "Loading..."
        );

        setup(center);
        origin.drawableManager().removeUIElement(playerScore);
        otherPlayerScores = new ArrayList<>();
    }

    private void setup(Pointf center) {
        if (gameEndText != null) {
            gameEndText.setFill(Colors.Snowy);
        }

        if (playerScore != null) {
            playerScore.getStatDisplay().setFont(Fonts.StatTextFont);
            playerScore.getStatDisplay().setFill(Colors.Snowy);
            playerScore.translate(Pointf.subtract(center, playerScore.width(), 90f));
        }
    }

    @Override
    public void render(Graphics2D g) {
        AffineTransform oldTransform = (AffineTransform) g.getTransform().clone();
        g.transform(getTransformation());

        alphaScreen.render(g);
        backgroundScreen.render(g);
        gameEndText.render(g);
        winnerText.render(g);
        playerScore.render(g);

        if (otherPlayerScores != null) {
            for (ContentBox otherPlayerScore : otherPlayerScores) {
                otherPlayerScore.render(g);
            }
        }

        if (nextText != null) {
            nextText.render(g);
        }

        g.setTransform(oldTransform);
    }

    @Override
    public void destroy(GameHandler origin) {
        super.destroyTheRest(origin);

        if (alphaScreen != null) {
            alphaScreen.destroy(origin);
            alphaScreen = null;
        }

        if (backgroundScreen != null) {
            backgroundScreen.destroy(origin);
            backgroundScreen = null;
        }

        if (gameEndText != null) {
            gameEndText.destroy(origin);
            gameEndText = null;
        }

        if (playerScore != null) {
            playerScore.destroy(origin);
            playerScore = null;
        }

        if (winnerText != null) {
            winnerText.destroy(origin);
            winnerText = null;
        }

        if (otherPlayerScores != null) {
            for (ContentBox otherPlayerScore : otherPlayerScores) {
                otherPlayerScore.destroy(origin);
            }

            otherPlayerScores.clear();
        }
    }

    public void addPointsResults(ClientPoints totalPoints, Scene origin, String nextTextString) {
        if (otherPlayerScores == null) {
            otherPlayerScores = new ArrayList<>();
        }

        playerScore.setContent("" + totalPoints.points());

        if (timeLeftProgressor != null) {
            timeLeftProgressor.shutdownNow();
        }

        timeLeft = Info.SessionSwitchTime;
        this.nextTextString = nextTextString;

        nextText = Text2D.create(nextTextString + " in " + timeLeft + "s")
            .withFont(Fonts.StatTextFont)
            .withFill(Colors.Snowy)
            .build();

        nextText.translate(Pointf.add(FastJEngine.getCanvas().getCanvasCenter(), 250f, 200f));

        timeLeftProgressor = Executors.newSingleThreadScheduledExecutor();
        timeLeftProgressor.scheduleAtFixedRate(this::decreaseTimeLeft, 1L, 1L, TimeUnit.SECONDS);
    }

    private void decreaseTimeLeft() {
        if (timeLeft == 0) {
            return;
        }

        timeLeft--;
        nextText.setText(nextTextString + " in " + timeLeft + "s");
    }
}
