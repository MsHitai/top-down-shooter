package com.trush.game.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import lombok.Data;

@Data
public class Weapon {

    protected Sound shoot;
    protected int power;
    protected float speed;
    protected boolean hasBonuses;
    protected Array<Bullet> bullet;

    public void shoot(float delta) {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
//todo
        }
    }
}
