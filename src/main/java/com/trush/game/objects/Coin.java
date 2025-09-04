package com.trush.game.objects;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Coin {

    private Vector2 position;
    private Texture texture;
    private Rectangle boundingBox;

    public Coin(float x, float y) {
        position = new Vector2(x, y);
        texture = new Texture("player.png");
        boundingBox = new Rectangle(x, y, texture.getWidth(), texture.getHeight());
    }

    // Add movement and collision logic here

    public void draw(SpriteBatch batch) {
        batch.draw(texture, position.x, position.y);
    }

    public void dispose() {
        texture.dispose();
    }
}

