package tech.fastj.partyhouse.util;

import java.awt.Color;

public class Colors {

    public static final Color LightSnowy = new Color(231, 231, 255);
    public static final Color Snowy = new Color(215, 215, 235);

    public static final Color PerfectGold = new Color(255, 215, 0);

    public static Color darkGray(int alpha) {
        return new Color(Color.darkGray.getRed(), Color.darkGray.getGreen(), Color.darkGray.getBlue(), alpha);
    }

    public static Color gray(int alpha) {
        return new Color(Color.gray.getRed(), Color.gray.getGreen(), Color.gray.getBlue(), alpha);
    }
}
