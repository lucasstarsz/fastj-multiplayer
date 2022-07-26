package tech.fastj.partyhouse.util;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

public class FilePaths {

    public static final Path StackAttackJson = Path.of("json/Stack Attack.json");
    public static final Path LadybirdJson = Path.of("json/Ladybird.json");
    public static final Path MainMenuMusic = Path.of("audio/Letter_to_the_Hand_Man-Percussion.wav");

    public static final InputStream NotoSansRegular = streamResource("/notosans/NotoSans-Regular.ttf");
    public static final InputStream NotoSansBold = streamResource("/notosans/NotoSans-Bold.ttf");
    public static final InputStream NotoSansBoldItalic = streamResource("/notosans/NotoSans-BoldItalic.ttf");
    public static final InputStream NotoSansItalic = streamResource("/notosans/NotoSans-Italic.ttf");
    public static final InputStream NotoSansMono = streamResource("/notosansmono/NotoSansMono-VariableFont_wdth,wght.ttf");

    public static InputStream streamResource(String resourcePath) {
        return Objects.requireNonNull(
                FilePaths.class.getResourceAsStream(resourcePath),
                "Couldn't find resource " + resourcePath
        );
    }
}
