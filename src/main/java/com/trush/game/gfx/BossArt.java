package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/** Painted materials anchored to the bosses' moving parts. Cell order matches tools/boss_art.py. */
public final class BossArt implements Disposable {
    public enum Part {
        KING, KING_FURY, ROYAL_CROWN, HAY_COAT, HARVEST_APRON, WITCH_HAT, BIN, TRUCK,
        SIGNAL, COCKPIT, PIGEON, BOILER, KAIJU_SCALES, MECH_PLATE, HERALD, SHARK,
        KRAKEN, GORGON, JELLY, CAT_COAT, BRICKWORK, SKUNK_CAPE, MOON_CLOTH, SHADOW_FLUFF,
        CLOCK, NIGHTMARE_HIDE, TEDDY_FABRIC, STRAW_HAT, WITCH_ROBE, VELVET_SOFA, TRAIN_CAB, TEDDY_MUZZLE
    }

    private static final int CELL = 64;
    private static final int COLUMNS = 8;
    private final Texture sheet = new Texture("assets/sprites/boss_parts.png");
    private final ActorInk ink;

    public BossArt(ActorInk ink) {
        this.ink = ink;
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    /** Draw a part centred on its animated anchor. */
    public void draw(SpriteBatch batch, Part part, float x, float y, float width, float height,
                     boolean flip, boolean flash, Color tint) {
        float left = x - width / 2f;
        float bottom = y - height / 2f;
        ink.bounds(batch, left, bottom, width, height);
        batch.setColor(flash ? Color.WHITE : tint);
        int cell = part.ordinal();
        int row = cell / COLUMNS + (flash ? 4 : 0);
        batch.draw(sheet, left, bottom, width, height, cell % COLUMNS * CELL, row * CELL,
                CELL, CELL, flip, false);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        sheet.dispose();
    }
}
