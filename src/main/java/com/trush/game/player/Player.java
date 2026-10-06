package com.trush.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import lombok.Getter;

@Getter
public class Player {

    private Vector2 position;
    private Texture texture;

    private Sprite playerSprite;
    private Rectangle boundingBox;
    private float speed;
    private int health;

    public Player(float x, float y) {
        position = new Vector2(x, y);
        speed = 3f;
        health = 50;
        texture = new Texture("assets/player.png");
        playerSprite = new Sprite(texture);
        playerSprite.setSize(0.8f * texture.getWidth() / texture.getHeight(), 0.8f);
        playerSprite.setPosition(x, y);
        boundingBox = new Rectangle(x, y, playerSprite.getWidth(), playerSprite.getHeight());
    }

    public void move(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerSprite.translateX(speed * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerSprite.translateX(-speed * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            playerSprite.translateY(speed * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            playerSprite.translateY(-speed * delta);
        }
        boundingBox.setPosition(playerSprite.getX(), playerSprite.getY());
    }

    public void dispose() {
        playerSprite.getTexture().dispose();
    }
}

