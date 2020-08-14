package de.spicysources.bubblepenetration.util;

public class Utilities {
    public static float normalize(float[] vector) {
        if(vector.length < 2 || vector.length > 3)
            return 0.0f;	// invalid vector -> abort

        float len = 0.0f;
        for (float f : vector) {
            len += f * f;
        }
        len = (float) Math.sqrt(len);

        for(int i = 0; i < vector.length; i++)
            vector[i] /= len;

        return len;
    }
}
