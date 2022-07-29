package tech.fastj.partyhouse.objects;

import tech.fastj.engine.FastJEngine;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.Drawable;
import tech.fastj.graphics.game.GameObject;
import tech.fastj.graphics.game.Model2D;

import tech.fastj.resources.models.ModelUtil;
import tech.fastj.systems.behaviors.Behavior;
import tech.fastj.systems.control.GameHandler;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.UUID;

import tech.fastj.gameloop.CoreLoopState;
import tech.fastj.partyhouse.scenes.multiplayer.snowball.SnowballFight;
import tech.fastj.partyhouse.util.FilePaths;
import tech.fastj.partyhouse.util.Tags;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.SnowballInfo;

public class Snowball extends GameObject implements Behavior {

    public static final float StartingLife = 1f;
    private static final float TravelSpeed = 20f;

    private final SnowballFight scene;
    private final Pointf travelMovement;
    private final Model2D snowballModel;
    private final ClientInfo clientInfo;
    private final Pointf trajectory;

    private float life;
    private final UUID snowballId;

    public Snowball(SnowballInfo snowballInfo, SnowballFight scene) {
        this(
            snowballInfo.clientInfo(),
            snowballInfo.snowballId(),
            new Pointf(snowballInfo.trajectoryX(), snowballInfo.trajectoryY()),
            snowballInfo.rotation(),
            new Pointf(snowballInfo.positionX(), snowballInfo.positionY()),
            scene,
            snowballInfo.currentLife()
        );
    }

    public Snowball(ClientInfo clientInfo, Pointf trajectory, float rotation, Player player, SnowballFight scene, float life) {
        this(clientInfo, UUID.randomUUID(), trajectory, rotation, player.getCenter(), scene, life);
    }

    public Snowball(ClientInfo clientInfo, UUID snowballId, Pointf trajectory, float rotation, Pointf position, SnowballFight scene, float life) {
        this.snowballId = snowballId;
        this.trajectory = trajectory;

        snowballModel = Model2D.fromPolygons(ModelUtil.loadModel(FilePaths.Snowball));

        this.life = life;
        this.travelMovement = Pointf.multiply(trajectory, TravelSpeed);
        this.clientInfo = clientInfo;

        this.scene = scene;
        super.setCollisionPath(snowballModel.getCollisionPath());

        rotate(rotation);
        translate(position);

        addLateBehavior(this, scene);
    }

    @Override
    public void init(GameObject gameObject) {
    }

    @Override
    public void fixedUpdate(GameObject gameObject) {
        if (life <= 0f) {
            return;
        }

        life -= FastJEngine.getFixedDeltaTime();

        if (life <= 0f || !FastJEngine.getCanvas().isOnScreen(this, scene.getCamera())) {
            FastJEngine.runLater(() -> {
                System.out.println("killing snowball from " + clientInfo.clientName());
                scene.removeSnowball(getSnowballInfo());
                destroy(scene);
            }, CoreLoopState.FixedUpdate);
        }

        translate(travelMovement);

        if (scene.isPlayerDead()) {
            return;
        }

        Drawable player = scene.getFirstWithTag(Tags.LocalPlayer);

        if (player instanceof Player localPlayer) {
            if (clientInfo.clientName().equals(localPlayer.getPlayerName())) {
                System.out.println("don't throw a snowball at yourself, dummy");
                return;
            }

            System.out.println("found player " + localPlayer.getPlayerName());

            if (localPlayer.collidesWith(this)) {
                System.out.println("death by snowball?");
                FastJEngine.runLater(() -> this.destroy(scene), CoreLoopState.FixedUpdate);
                scene.deathBySnowball(getSnowballInfo());
            }
        }
    }

    public SnowballInfo getSnowballInfo() {
        return new SnowballInfo(
            clientInfo,
            snowballId,
            trajectory.x,
            trajectory.y,
            getTranslation().x,
            getTranslation().y,
            getRotation(),
            life
        );
    }

    @Override
    public void update(GameObject gameObject) {
    }

    @Override
    public void render(Graphics2D g) {
        if (!shouldRender()) {
            return;
        }

        AffineTransform oldTransform2 = (AffineTransform) g.getTransform().clone();
        g.transform(getTransformation());

        snowballModel.render(g);

        g.setTransform(oldTransform2);
    }

    @Override
    public void destroy(GameHandler origin) {
        super.destroyTheRest(origin);
        life = 0f;
    }
}
