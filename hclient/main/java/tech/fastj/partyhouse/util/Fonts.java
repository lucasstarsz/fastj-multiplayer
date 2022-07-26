package tech.fastj.partyhouse.util;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class Fonts {

    public static final Font DefaultNotoSans = Fonts.notoSans(Font.BOLD, 16);
    public static final Font ButtonTextFont = Fonts.notoSans(Font.PLAIN, 24);
    public static final Font StatTextFont = Fonts.notoSans(Font.BOLD, 20);
    public static final Font SmallStatTextFontBold = Fonts.notoSans(Font.BOLD, 16);
    public static final Font SmallStatTextFontPlain = Fonts.notoSans(Font.PLAIN, 16);
    public static final Font TitleTextFont = Fonts.notoSans(Font.BOLD, 48);
    public static final Font SubtitleTextFont = Fonts.notoSans(Font.BOLD, 36);
    public static final Font MonoStatTextFont = Fonts.notoSansMono(Font.PLAIN, 20);

    public static Font notoSans(int style, int size) {
        return new Font("Noto Sans", style, size);
    }

    public static Font notoSansMono(int style, int size) {
        return new Font("Noto Sans Mono", style, size);
    }

    static {
        try {
            Fonts.load(FilePaths.NotoSansRegular);
            Fonts.load(FilePaths.NotoSansBold);
            Fonts.load(FilePaths.NotoSansBoldItalic);
            Fonts.load(FilePaths.NotoSansItalic);
            Fonts.load(FilePaths.NotoSansMono);
        } catch (FontFormatException | IOException exception) {
            Log.error(Fonts.class, "Couldn't load fonts", exception);
            FastJEngine.closeGame();
        }
    }

    private static void load(InputStream fontFile) throws IOException, FontFormatException {
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
    }
}
