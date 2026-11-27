package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.trush.game.gfx.Sprites;
import com.trush.game.save.Flufficon.Medal;

/** Shared medal sprite and caption color for the bell, Flufficon and victory screen. */
final class RematchMedalArt {
    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color SILVER = Color.valueOf("c5d4ea");

    private RematchMedalArt() {
    }

    static Color color(Medal medal) {
        return medal == Medal.GOLD ? GOLD : SILVER;
    }

    static void draw(SpriteBatch batch, Sprites sprites, Medal medal, float cx, float cy, float height) {
        if (medal == Medal.NONE) return;
        TextureRegion region = sprites.prop(medal == Medal.GOLD ? "rematch_medal_gold" : "rematch_medal_silver");
        float width = height * region.getRegionWidth() / region.getRegionHeight();
        batch.draw(region, cx - width / 2f, cy - height / 2f, width, height);
    }
}
