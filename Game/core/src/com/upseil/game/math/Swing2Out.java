package com.upseil.game.math;

import com.badlogic.gdx.math.Interpolation;

public class Swing2Out extends Interpolation {

    @Override
    public float apply(float a) {
        a = a - 1;
        return 1 + 0.41495f * a + 2.75642f * a*a + 3.34004f * a*a*a;
    }
    
}
