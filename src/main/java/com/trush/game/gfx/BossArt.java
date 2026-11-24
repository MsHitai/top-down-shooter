package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/** Painted materials anchored to the bosses' moving parts. Cell order matches tools/boss_art.py. */
public final class BossArt implements Disposable {
    public enum Part {
        KING, KING_FURY, ROYAL_CROWN, HAY_COAT, HARVEST_APRON, WITCH_HAT, BIN, TRUCK,
        SIGNAL, COCKPIT, PIGEON, BOILER, KAIJU_SCALES, MECH_PLATE, HERALD, SHARK,
        KRAKEN, GORGON, JELLY, CAT_COAT, BRICKWORK, SKUNK_CAPE, MOON_CLOTH, SHADOW_FLUFF,
        CLOCK, NIGHTMARE_HIDE, TEDDY_FABRIC, STRAW_HAT, WITCH_ROBE, VELVET_SOFA, TRAIN_CAB, TEDDY_MUZZLE,
        GRANDFLUFF_GOLD, GRANDFLUFF_SILVER, CRESCENT_GOLD, CRESCENT_SILVER, GRANDFLUFF_BEARD,
        SKUNK_TAIL, SKUNK_HEAD, SKUNK_SASH, SKUNK_CROWN, TRASH_BAG,
        TEDDY_HEAD, TEDDY_PAW, TEDDY_EAR, GORGON_BODICE, GORGON_SCALES, GORGON_SNAKE,
        PIGEON_WING, NIGHTMARE_WING, BIN_LID, TIRE, CLOCK_BELL, CAT_HEAD,
        HERALD_HELM, SHARK_FIN, SACK_FACE, HARVEST_MASK, SHADOW_CROWN,
        DUST_BUNNY, BED_MONSTER, SOCK_NINJA, YARN_HYDRA, WIND_UP_GENERAL, LINT_REGENT
    }

    private static final int CELL = 64;
    private static final int COLUMNS = 8;
    private static final int ROWS = (Part.values().length + COLUMNS - 1) / COLUMNS;
    private final Texture sheet = new Texture("assets/sprites/boss_parts.png");
    private final Texture moon = new Texture("assets/sprites/boss_moon.png");
    private final Texture underBed = new Texture("assets/sprites/boss_under_bed.png");
    private static final int BED_CELL = 128;
    private static final int BED_COLUMNS = 3;
    private static final int BED_ROWS = 2;
    private final ActorInk ink;

    public BossArt(ActorInk ink) {
        this.ink = ink;
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        moon.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        underBed.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    /** Draw a part centred on its animated anchor. */
    public void draw(SpriteBatch batch, Part part, float x, float y, float width, float height,
                     boolean flip, boolean flash, Color tint) {
        draw(batch, part, x, y, width, height, 0f, flip, flash, tint);
    }

    public void draw(SpriteBatch batch, Part part, float x, float y, float width, float height,
                     float angle, boolean flip, boolean flash, Color tint) {
        drawPart(batch, part, x, y, width, height, angle, flip, flash, tint, 0f, 0f);
    }

    /** Pupils move in world space so mirroring and recoil never reverse the gaze. */
    public void drawLooking(SpriteBatch batch, Part part, float x, float y, float width, float height,
                            float angle, boolean flip, Color tint, float gaze) {
        drawPart(batch, part, x, y, width, height, angle, flip, false, tint,
                MathUtils.cosDeg(gaze) * width * 1.25f / BED_CELL,
                MathUtils.sinDeg(gaze) * height * 1.25f / BED_CELL);
    }

    private void drawPart(SpriteBatch batch, Part part, float x, float y, float width, float height,
                          float angle, boolean flip, boolean flash, Color tint, float lookX, float lookY) {
        float left = x - width / 2f;
        float bottom = y - height / 2f;
        if (angle == 0f) {
            ink.bounds(batch, left, bottom, width, height);
        } else {
            float extent = (width + height) / 2f;
            ink.bounds(batch, x - extent, y - extent, extent * 2f, extent * 2f);
        }
        batch.setColor(flash ? Color.WHITE : tint);
        boolean bed = part.ordinal() >= Part.DUST_BUNNY.ordinal()
                && part.ordinal() <= Part.LINT_REGENT.ordinal();
        int cell = bed ? part.ordinal() - Part.DUST_BUNNY.ordinal() : part.ordinal();
        int columns = bed ? BED_COLUMNS : COLUMNS;
        int pixels = bed ? BED_CELL : CELL;
        int row = cell / columns + (flash ? (bed ? BED_ROWS : ROWS) : 0);
        batch.draw(bed ? underBed : sheet, left, bottom, width / 2f, height / 2f, width, height, 1f, 1f, angle,
                cell % columns * pixels, row * pixels, pixels, pixels, flip, false);
        if (bed && !flash) {
            batch.draw(underBed, left + lookX, bottom + lookY, width / 2f, height / 2f,
                    width, height, 1f, 1f, angle, cell % columns * pixels,
                    (cell / columns + BED_ROWS * 2) * pixels, pixels, pixels, flip, false);
        }
        batch.setColor(Color.WHITE);
    }

    /** The moon's large face has its own higher-resolution image. */
    public void drawMoon(SpriteBatch batch, float x, float y, float diameter, boolean flash, Color tint) {
        int pixels = moon.getHeight();
        batch.setColor(tint);
        batch.draw(moon, x - diameter / 2f, y - diameter / 2f, diameter, diameter,
                flash ? pixels : 0, 0, pixels, pixels, false, false);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        sheet.dispose();
        moon.dispose();
        underBed.dispose();
    }
}
