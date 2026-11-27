package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.Locale;

/** Individually drawn cast; native-resolution art, independent of combat hitboxes. */
public final class CastArt implements Disposable {
    private final ObjectMap<String, Texture> sheets = new ObjectMap<>();

    public CastArt() {
        for (String name : new String[]{"ShellCop", "RiotCop", "Sniper", "Robber", "Sludger", "Agent",
                "Dynamite", "Hydrant", "GhostKin", "RoboCart", "Android", "Dandelion",
                "DustMite", "WindUpBeetle", "LostMarble", "DustBunnyKit", "PegCroc", "QuiltMoth", "TumbleDie",
                "Biker", "LampMimic", "CubeIce", "CubeLead", "Angel", "AngelBow", "Cherub", "Thundercloud",
                "Diver", "AcidSprayer", "BarrelRoller", "Flufferfish", "TurtleShell", "GiantCrab",
                "GiantOctopus", "StinkPigeon", "SewerGator", "AlleyDog", "Raccoon", "MoonBunny",
                "LivingAsteroid", "IronAsteroid", "Astronaut", "VeteranAstronaut", "Alien", "VeteranAlien",
                "Ufo", "MoonGolem", "GolemHead", "CheeseWorm", "WormHead", "Starfish", "Swordfish",
                "ShapeCircle", "ShapeSquare", "ShapeTriangle", "ShapeStar",
                "CrabClaw", "GolemFist", "RustDrum", "RaccoonHead"}) {
            Texture texture = new Texture("assets/sprites/cast_" + name.toLowerCase(Locale.ROOT) + ".png");
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            sheets.put(name, texture);
        }
    }

    public boolean contains(String kind) {
        return sheets.containsKey(kind) || switch (kind) {
            case "Cube", "SwordAngel", "BowAngel", "Turtle", "Shapeshifter" -> true;
            default -> false;
        };
    }

    public void centered(SpriteBatch batch, String kind, float x, float y, float time,
                         boolean right, boolean preparing, boolean flash, Color tint,
                         float scaleX, float scaleY, float alpha) {
        draw(batch, kind, x, y - 21f / 64 * scaleY, time, right, preparing, flash, tint, scaleX, scaleY, alpha);
    }

    public void centeredPose(SpriteBatch batch, String kind, float x, float y, int pose,
                             boolean right, boolean flash, Color tint, float scale) {
        drawPose(batch, kind, x, y - 21f / 64 * scale, pose, right, flash, tint, scale, scale, 1f);
    }

    public static int walkPose(float time) {
        return time == 0 ? 4 : (int)(time * 8) % 4;
    }

    public void draw(SpriteBatch batch, String kind, float x, float feet, float time,
                     boolean right, boolean preparing, boolean flash, Color tint) {
        draw(batch, kind, x, feet, time, right, preparing, flash, tint, 1, 1, 1);
    }

    public void draw(SpriteBatch batch, String kind, float x, float feet, float time,
                     boolean right, boolean preparing, boolean flash, Color tint,
                     float scaleX, float scaleY, float alpha) {
        drawPose(batch, kind, x, feet, preparing ? 5 : walkPose(time), right, flash, tint, scaleX, scaleY, alpha);
    }

    private void drawPose(SpriteBatch batch, String kind, float x, float feet, int pose,
                          boolean right, boolean flash, Color tint, float scaleX, float scaleY, float alpha) {
        batch.setColor(flash ? Color.WHITE : tint);
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, tint.a * alpha);
        batch.draw(sheets.get(kind), x - 24f / 64 * scaleX, feet - 3f / 64 * scaleY,
                48f / 64 * scaleX, 48f / 64 * scaleY, pose * 48, flash ? 48 : 0, 48, 48, !right, false);
        batch.setColor(Color.WHITE);
    }

    /** Rotating bodies leave the face and attack effects free to animate above them. */
    public void rotated(SpriteBatch batch, String kind, float x, float y, float scaleX, float scaleY,
                        float angle, boolean flipY, boolean flash, Color tint) {
        float w = 48f / 64 * scaleX, h = 48f / 64 * scaleY;
        batch.setColor(flash ? Color.WHITE : tint);
        batch.draw(sheets.get(kind), x - w / 2, y - h / 2, w / 2, h / 2, w, h, 1, 1,
                angle, 4 * 48, flash ? 48 : 0, 48, 48, false, flipY);
        batch.setColor(Color.WHITE);
    }

    @Override public void dispose() { for (Texture texture : sheets.values()) texture.dispose(); }
}
