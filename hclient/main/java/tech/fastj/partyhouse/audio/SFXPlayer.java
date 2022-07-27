package tech.fastj.partyhouse.audio;

import tech.fastj.engine.FastJEngine;

import tech.fastj.systems.audio.AudioEvent;
import tech.fastj.systems.audio.StreamedAudio;

import java.nio.file.Path;

public class SFXPlayer {

    public static void playSfx(Path audioPath) {
        StreamedAudio audio = FastJEngine.getAudioManager().loadStreamedAudio(audioPath);
        audio.getAudioEventListener().setAudioStopAction(event -> FastJEngine.runAfterRender(() -> {
            FastJEngine.getGameLoop().removeEventObserver(audio.getAudioEventListener(), AudioEvent.class);
            FastJEngine.getAudioManager().unloadStreamedAudio(audio.getID());
        }));
        audio.gainControl().setValue(-5f);
        audio.play();
    }
}
