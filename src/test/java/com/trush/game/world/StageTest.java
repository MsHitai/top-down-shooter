package com.trush.game.world;

import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StageTest {

    @Test
    void floorsMapToStagesAndStayOnTheLastOne() {
        assertEquals(Stage.VILLAGE, Stage.forFloor(1));
        assertEquals(Stage.TOWN, Stage.forFloor(2));
        assertEquals(Stage.TOWN, Stage.forFloor(7));
    }

    @Test
    void theTownSwapsFurniture() {
        assertEquals(Prop.Kind.HAY, Stage.VILLAGE.propFor('H'));
        assertEquals(Prop.Kind.BENCH, Stage.TOWN.propFor('H'));
        assertEquals(Prop.Kind.PILLAR, Stage.VILLAGE.propFor('P'));
        assertEquals(Prop.Kind.LAMP, Stage.TOWN.propFor('P'));
        assertEquals(Prop.Kind.TABLE, Stage.TOWN.propFor('T'));
    }
}
