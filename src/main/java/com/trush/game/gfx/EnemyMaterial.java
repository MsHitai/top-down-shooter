package com.trush.game.gfx;

/** Material-specific authored marks: seams, rivets, facets, fur tufts and scale plates. */
public enum EnemyMaterial {
    PLAIN, FABRIC, METAL, STONE, SCALES, FUR, WOOD, SOFT;

    public static EnemyMaterial forKind(String kind) {
        return switch (kind) {
            case "Bulletmancer", "Spiralmancer", "Bloomancer", "GhostKin", "Robber", "Sniper",
                 "Agent", "Astronaut", "Diver", "SwordAngel", "BowAngel", "Cherub", "Messenger",
                 "HollowWitch", "Haymaker", "BedBoss" -> FABRIC;
            case "SawnOffKin", "VeteranKin", "BandanaKin", "PirateKin", "BlueShotgunKin", "TankerKin",
                 "Hydrant", "ShellCop", "RiotCop", "Biker", "RoboCart", "Android", "Ufo",
                 "Harvestman", "TrashThatCan", "TrafficLight", "RoadRager", "NightHawk", "EvilTrain",
                 "Mech", "AlarmClock", "BarrelRoller", "AcidSprayer" -> METAL;
            case "Cube", "LivingAsteroid", "MoonGolem", "WalkingBuilding" -> STONE;
            case "Alien", "SewerGator", "Turtle", "Swordfish", "Flufferfish", "Starfish", "GiantCrab",
                 "GiantOctopus", "CheeseWorm", "Kaiju", "FluffyShark", "Kraken", "Gorgon" -> SCALES;
            case "Rat", "StinkPigeon", "AlleyDog", "Raccoon", "MoonBunny", "PigeonKing",
                 "FluffyKing", "Grandfluff", "ShadowKing", "AlleyCat", "SkunkKing", "BigTeddy" -> FUR;
            case "Dynamite", "LampMimic" -> WOOD;
            case "Shapeshifter", "Thundercloud", "Dandelion", "Sludger", "BlueBlob", "Nightmare", "BedCritter" -> SOFT;
            default -> PLAIN;
        };
    }
}
