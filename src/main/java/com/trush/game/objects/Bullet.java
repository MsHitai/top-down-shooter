package com.trush.game.objects;

import com.badlogic.gdx.graphics.g2d.Sprite;
import lombok.Data;

@Data
public class Bullet {

    protected Sprite bulletSprite;


    public void dispose() {
        bulletSprite.getTexture().dispose();
    }
}
