package jp.gr.java_conf.pitto.definition;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SEManager implements LineListener {


    private static SEManager me = new SEManager();
    private static HashMap<File, Clip> seData = new HashMap<>();
    private static ArrayList<Clip> taskList = new ArrayList<>();

    private SEManager() {


    }

    public static Clip loadSoundFile(File audioFilePath) {
        if (seData.containsKey(audioFilePath)) {
            return seData.get(audioFilePath);
        }
        Clip createSoundFile = createSoundFile(audioFilePath);
        if (createSoundFile != null) {
            seData.put(audioFilePath, createSoundFile);
        }
        return createSoundFile;
    }

    private static Clip createSoundFile(File audioFilePath) {
        try {

            return getClip(audioFilePath);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Clip getClip(File audioFilePath) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(audioFilePath);
        AudioFormat format = stream.getFormat();
        AudioFormat.Encoding encoding = format.getEncoding();

        if ((encoding == AudioFormat.Encoding.ULAW)|| (encoding == AudioFormat.Encoding.ALAW)) {
            encoding = AudioFormat.Encoding.PCM_SIGNED;

            float sampleRate = format.getSampleRate();
            int sampleSizeInBits= format.getSampleSizeInBits() * 2;
            int channels= format.getChannels();
            int frameSize= format.getFrameSize() * 2;
            float frameRate= format.getFrameRate();
            boolean bigEndian = true;

            AudioFormat newFormat = new AudioFormat(encoding,sampleRate,sampleSizeInBits ,channels ,frameSize ,frameRate
                    ,bigEndian);
            stream = AudioSystem.getAudioInputStream(newFormat, stream);
            format = newFormat;
        }

        DataLine.Info info = new DataLine.Info(Clip.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("エラー: " + audioFilePath + "はサポートされていない形式です");
            System.exit(0);
        }

        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.addLineListener(me);
        clip.open(stream);
        return clip;
    }


    public static void playSoundFile(File audioFilePath) throws FileNotFoundException {
        Clip audioFile = seData.get(audioFilePath);
        if (audioFile == null) {

            audioFile = loadSoundFile(audioFilePath);
            if (audioFile == null) {
                throw new FileNotFoundException();
            }
        }

        if (audioFile.getLongFramePosition() == 0) {
            audioFile.start();
        } else {
            audioFile = createSoundFile(audioFilePath);
            taskList.add(audioFile);
            audioFile.start();
        }
    }

    public static void dispose(File audioFilePath) {
        seData.remove(audioFilePath);
    }

    public void update(LineEvent event) {

        if (event.getType() == LineEvent.Type.STOP) {
            Clip clip = (Clip) event.getSource();
            clip.stop();
            if (seData.containsValue(clip)) {
                clip.setFramePosition(0);
                if (taskList.contains(clip)) {
                    taskList.remove(clip);
                    clip.start();
                }
            } else {
                while (taskList.remove(clip)) {
                }
                clip.drain();
                clip.close();
            }
        }
    }
}
