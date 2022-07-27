package tech.fastj.partyhouse.scripts;

import tech.fastj.engine.FastJEngine;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.game.GameObject;

import tech.fastj.input.keyboard.Keyboard;
import tech.fastj.input.keyboard.Keys;
import tech.fastj.systems.behaviors.Behavior;

import tech.fastj.partyhousecore.ClientGameState;

public class HomeController implements Behavior {

    private final float speed;
    private final float rotation;

    private float currentRotation;
    private float inputRotation;

    private Pointf inputTranslation;
    private final ClientGameState gameState;

    public HomeController(float speedInterval, float rotationInterval, ClientGameState gameState) {
        speed = speedInterval;
        rotation = rotationInterval;
        this.gameState = gameState;
    }

    @Override
    public void init(GameObject obj) {
        inputTranslation = new Pointf();
        inputRotation = 0f;
        currentRotation = 0f;
    }

    @Override
    public void fixedUpdate(GameObject gameObject) {
    }

    @Override
    public void update(GameObject obj) {
        resetTransformations();
        pollMovement();
        movePlayer();
    }

    private void resetTransformations() {
        inputTranslation.reset();
        inputRotation = 0f;
    }

    private void pollMovement() {
        if (Keyboard.isKeyDown(Keys.A)) {
            inputRotation -= rotation * FastJEngine.getDeltaTime();
        } else if (Keyboard.isKeyDown(Keys.D)) {
            inputRotation += rotation * FastJEngine.getDeltaTime();
        }

        currentRotation += inputRotation;

        if (Keyboard.isKeyDown(Keys.W)) {
            inputTranslation.y -= speed * FastJEngine.getDeltaTime();
        } else if (Keyboard.isKeyDown(Keys.S)) {
            inputTranslation.y += speed * FastJEngine.getDeltaTime();
        }

        inputTranslation.rotate(-currentRotation);
    }

    private void movePlayer() {
        if (inputRotation != 0f) {
            gameState.updateVelocity(inputRotation, 0f);
        }

        if (!Pointf.origin().equals(inputTranslation)) {
            gameState.updatePosition(inputTranslation.x, inputTranslation.y);
        }
    }

    @Override
    public void destroy() {
        inputTranslation = null;
        inputRotation = 0f;
        currentRotation = 0f;
    }
}
