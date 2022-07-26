package tech.fastj.partyhouse.util;

import tech.fastj.graphics.util.DrawUtil;
import tech.fastj.math.Pointf;

import java.awt.*;

public class Shapes {

    private static Pointf[] getBlockMesh(Pointf size) {
        return DrawUtil.createBox(Pointf.origin(), size);
    }

    public static final BasicStroke ThickStroke = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static final BasicStroke ThickerRoundedStroke = new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static final BasicStroke ThickEdgedStroke = new BasicStroke(4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);

    public static final float NoteSize = 30f;

    public static final Pointf ButtonSize = new Pointf(200f, 50f);
}
