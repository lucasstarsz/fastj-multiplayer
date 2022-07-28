package tech.fastj.partyhouse.scripts;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.game.GameObject;

import tech.fastj.input.keyboard.Keyboard;
import tech.fastj.input.keyboard.Keys;
import tech.fastj.systems.behaviors.Behavior;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import tech.fastj.partyhouse.objects.Player;
import tech.fastj.partyhouse.scenes.multiplayer.snowball.SnowballFight;

public class SnowballController implements Behavior {

    public static final int MaxSnowballsCarried = 5;
    private static final int SnowballMakeCooldown = 1000;
    private static final int SnowballThrowCooldown = 500;

    private boolean canThrowSnowball;
    private boolean canMakeSnowball;
    private int snowballCount;

    private final SnowballFight scene;

    private ExecutorService cooldownManager;

    public SnowballController(SnowballFight scene) {
        this.scene = scene;
    }

    @Override
    public void init(GameObject gameObject) {
        this.cooldownManager = Executors.newWorkStealingPool();
        snowballMakeCooldown();
        snowballThrowCooldown();
    }

    @Override
    public void fixedUpdate(GameObject gameObject) {
    }

    @Override
    public void update(GameObject gameObject) {
        pollKeyboard(gameObject);
    }

    @Override
    public void destroy() {
        if (cooldownManager != null) {
            cooldownManager.shutdownNow();
            cooldownManager = null;
        }
    }

    private void pollKeyboard(GameObject gameObject) {
        if (scene.isPlayerDead()) {
            return;
        }

        if (gameObject instanceof Player player) {
            if (Keyboard.isKeyRecentlyPressed(Keys.Space)) {
                if (!canThrowSnowball) {
                    Log.info(SnowballController.class, "Snowball throwing is still on cooldown.");
                    return;
                }

                if (snowballCount < 1) {
                    Log.info(SnowballController.class, "You don't have any snowballs to throw!");
                    return;
                }

                FastJEngine.runAfterUpdate(() -> {
                    float playerRotation = player.getRotationWithin360();
                    Pointf trajectory = Pointf.up().rotate(-playerRotation);

                    scene.spawnSnowball(player, trajectory, playerRotation);
                    snowballCount--;

                    snowballThrowCooldown();
                    scene.updateSnowballsCarried(snowballCount, canMakeSnowball, canThrowSnowball);
                });
            }

            if (Keyboard.isKeyRecentlyPressed(Keys.R)) {
                if (!canMakeSnowball) {
                    Log.debug(SnowballController.class, "Snowball making is still on cooldown.");
                    return;
                }

                makeSnowball();
            }
        }
    }

    private void makeSnowball() {
        if (snowballCount == MaxSnowballsCarried) {
            Log.debug(SnowballController.class, "You can't carry more than {} snowballs!", MaxSnowballsCarried);
            return;
        }

        snowballCount++;
        snowballMakeCooldown();

        scene.updateSnowballsCarried(snowballCount, canMakeSnowball, canThrowSnowball);
        Log.debug(SnowballController.class, "You created a snowball.");
    }

    private void snowballThrowCooldown() {
        canThrowSnowball = false;

        cooldownManager.submit(() -> {
            TimeUnit.MILLISECONDS.sleep(SnowballThrowCooldown);
            canThrowSnowball = true;
            return 0;
        });
    }

    private void snowballMakeCooldown() {
        canMakeSnowball = false;

        cooldownManager.submit(() -> {
            TimeUnit.MILLISECONDS.sleep(SnowballMakeCooldown);
            canMakeSnowball = true;
            return 0;
        });
    }
}
