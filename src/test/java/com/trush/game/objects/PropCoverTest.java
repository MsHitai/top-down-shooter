package com.trush.game.objects;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.trush.game.gfx.Sprites;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class PropCoverTest {

    /** Sprites that hand out a region of the given pixel size for every prop. */
    private static Sprites sprites(int w, int h) {
        // subclass mocks: the inline mock maker can't instrument these classes on newer JDKs
        TextureRegion r = mock(TextureRegion.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(r.getRegionWidth()).thenReturn(w);
        when(r.getRegionHeight()).thenReturn(h);
        Sprites s = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(s.prop(anyString())).thenReturn(r);
        return s;
    }

    @Test
    void tallCoverOnlyStopsBulletsGoingUpTheScreen() {
        Prop pillar = new Prop(Prop.Kind.PILLAR, 3f, 2f);
        Sprites s = sprites(18, 30);
        Rectangle low = new Rectangle(pillar.bulletRect(s, false));
        Rectangle tall = pillar.bulletRect(s, true);
        assertEquals(pillar.solid, low, "down/sideways bullets only hit the footprint");
        assertTrue(tall.height > low.height + 0.3f, "up-the-screen bullets hit the whole pillar");
        assertEquals(low.y, tall.y, 1e-6);
    }

    @Test
    void aStreetLampOnlyCoversWithItsSlimPost() {
        Prop lamp = new Prop(Prop.Kind.LAMP, 3f, 2f);
        Sprites s = sprites(12, 40);
        Rectangle low = lamp.bulletRect(s, false);
        assertEquals(0.14f, low.width, 1e-6);
        assertEquals(3f, low.x + low.width / 2f, 1e-6);
        assertTrue(low.width < lamp.solid.width, "thinner than the base you bump into");
        assertEquals(0.14f, lamp.bulletRect(s, true).width, 1e-6);
    }
}
