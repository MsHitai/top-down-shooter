package com.trush.game.world;

/** Drives a game mode: what spawns when, banners, music, and when the run is won. */
public interface Director {

    void update(float delta, GameWorld world);

    /** Called after the player walks into a room (and once for the first room). */
    void onRoomEntered(Room room, GameWorld world);

    /** Big centred text, or null. */
    String banner();

    float bannerAlpha();

    /** Name of the track in assets/music/ that should be playing (without .ogg). */
    String music();

    /** True once the mode is won (story: stage boss defeated). */
    boolean isFinished();

    /** A fight is on: a locked room / an endless wave (no big map, the Flufful keeps quiet). */
    boolean inFight(GameWorld world);
}
