package tech.fastj.partyhouse.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Objects;

public class FilePaths {

    public static final Path Player = pathFromClassLoad(FilePaths.class, "/player.psdf", "jar");
    public static final Path PlayerArrow = pathFromClassLoad(FilePaths.class, "/playerarrow.psdf", "jar");

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

    public static Path pathFromClassLoad(Class<?> classToLoadFrom, String resourcePath, String expectedScheme) {
        try {
            URI resource = Objects.requireNonNull(
                classToLoadFrom.getResource(resourcePath),
                "Couldn't find resource " + resourcePath
            ).toURI();

            checkFileSystem(resource, expectedScheme);

            return Paths.get(resource);
        } catch (URISyntaxException | IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void checkFileSystem(URI resource, String expectedScheme) throws IOException {
        if (!expectedScheme.equalsIgnoreCase(resource.getScheme())) {
            return;
        }

        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (!provider.getScheme().equalsIgnoreCase(expectedScheme)) {
                continue;
            }

            try {
                provider.getFileSystem(resource);
            } catch (FileSystemNotFoundException e) {
                // the file system doesn't exist yet...
                // in this case we need to initialize it first:
                provider.newFileSystem(resource, Collections.emptyMap());
            }
        }
    }
}
