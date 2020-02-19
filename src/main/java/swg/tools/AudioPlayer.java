package swg.tools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import swg.SWGAide;

/**
 * Plays back audio using the Clip in Java Sound API.
 * 
 * @author Mr-Miagi
 *
 */
public class AudioPlayer implements LineListener {
	
	/**
     * this flag indicates whether the play back completes or not.
     */
    boolean playCompleted;
    
    /**
     * Play a given audio resource within the project
     * 
     * @param path Path of the audio resource.
     */
    public void play(String path) {
 
        try {
        	InputStream buffered = new BufferedInputStream(SWGAide.class.getResourceAsStream(path));
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(buffered);
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip audioClip = (Clip) AudioSystem.getLine(info);
            audioClip.addLineListener(this);
            audioClip.open(audioStream);
            audioClip.start();
            while (!playCompleted) {
                // wait for the play back to complete
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            audioClip.close();
        } catch (UnsupportedAudioFileException ex) {
        	SWGAide.printError("AudioPlayer: The specified audio file is not supported.", ex);
        } catch (LineUnavailableException ex) {
        	SWGAide.printError("AudioPlayer: Audio line for playing back is unavailable.", ex);
        } catch (IOException ex) {
        	SWGAide.printError("AudioPlayer: Error playing the audio file.", ex);
        }
         
    }

	@Override
	public void update(LineEvent event) {
		LineEvent.Type type = event.getType();
        if (type == LineEvent.Type.STOP) {
            playCompleted = true;
        }
	}
}
