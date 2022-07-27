package tech.fastj.partyhouse.objects;

import tech.fastj.math.Pointf;
import tech.fastj.graphics.Boundary;
import tech.fastj.graphics.game.GameObject;
import tech.fastj.graphics.game.Model2D;
import tech.fastj.graphics.game.Polygon2D;
import tech.fastj.graphics.game.RenderStyle;
import tech.fastj.graphics.game.Text2D;
import tech.fastj.graphics.util.DrawUtil;

import tech.fastj.resources.models.ModelUtil;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SimpleManager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

import tech.fastj.partyhouse.util.FilePaths;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.Shapes;

public class Player extends GameObject {

    private final Model2D playerModel;
    private final Model2D directionalArrow;
    private final Text2D playerIndicator;
    private final Polygon2D playerIndicatorBorder;
    private String playerName;

    public Player(String playerName) {
        this.playerModel = Model2D.fromPolygons(ModelUtil.loadModel(FilePaths.Player));
        this.playerName = playerName;
        this.playerIndicator = Text2D.create(playerName)
            .withFont(Fonts.DefaultNotoSans)
            .build();

        this.playerIndicatorBorder = Polygon2D.create(DrawUtil.createBox(0f, 0f, 0f))
            .withRenderStyle(RenderStyle.Outline)
            .withOutline(Shapes.ThickStroke, Color.black)
            .build();

        setPlayerName(playerName);

        Polygon2D[] directionalArrowMesh = ModelUtil.loadModel(FilePaths.PlayerArrow);
        directionalArrowMesh[0].setFill(playerModel.getPolygons()[0].getFill());
        this.directionalArrow = Model2D.fromPolygons(directionalArrowMesh);

        super.setCollisionPath(playerModel.getCollisionPath());
    }

    @Override
    public void render(Graphics2D g) {
        if (!shouldRender()) {
            return;
        }

        Stroke oldStroke = g.getStroke();
        AffineTransform oldTransform2 = (AffineTransform) g.getTransform().clone();
        g.transform(getTransformation());

        directionalArrow.render(g);
        playerModel.render(g);

        g.setTransform(oldTransform2);
        g.translate(getTranslation().x, getTranslation().y);

        playerIndicator.render(g);
        playerIndicatorBorder.render(g);

        g.setTransform(oldTransform2);
        g.setStroke(oldStroke);
    }

    @Override
    public void destroy(Scene origin) {
        playerModel.destroy(origin);
        playerIndicator.destroy(origin);
    }

    @Override
    public void destroy(SimpleManager origin) {
        playerModel.destroy(origin);
        playerIndicator.destroy(origin);
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
        playerIndicator.setText(this.playerName);

        Pointf playerIndicatorTranslation = new Pointf(25f, -35f).subtract(playerIndicator.getCenter());
        playerIndicator.translate(playerIndicatorTranslation);

        Pointf borderLocation = playerIndicator.getBound(Boundary.TopLeft).subtract(8f, 4f);
        Pointf borderSize = new Pointf(playerIndicator.width() + 16f, playerIndicator.height() + 8f);
        playerIndicatorBorder.modifyPoints(DrawUtil.createBox(borderLocation, borderSize), false, false, false);
    }
}
