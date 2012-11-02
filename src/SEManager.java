

import java.io.File;
import java.io.FileInputStream;
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

public class SEManager implements LineListener{
	private static SEManager me=new SEManager();
//	private static HashMap<String,Pair<AudioFormat,Pair<Byte[],Integer>>> seData=new HashMap<String,Pair<AudioFormat,Pair<Byte[],Integer>>>();
	private static HashMap<File,Clip> seData=new HashMap<File,Clip>();
	private static ArrayList<Clip> taskList=new ArrayList<Clip>();
	private SEManager(){

	}
	public static Clip load(File filePath){
		if(seData.containsKey(filePath)){
			return seData.get(filePath);
		}
		Clip c=create(filePath);
		if(c!=null){
			seData.put(filePath, c);
		}
		return c;
	}
	private static Clip create(File filePath){
		try {
		//	AbstractFileLoad f=AbstractFileLoad.create(filePath);

			// オーディオストリームを開く
            AudioInputStream stream = AudioSystem
                    .getAudioInputStream(new FileInputStream(filePath));

            // オーディオ形式を取得
            AudioFormat format = stream.getFormat();
            // ULAW/ALAW形式の場合はPCM形式に変更
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW)
                    || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat newFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        format.getSampleSizeInBits() * 2, format.getChannels(),
                        format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(newFormat, stream);
                format = newFormat;
            }

            // ライン情報を取得
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            // サポートされてる形式かチェック
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("エラー: " + filePath + "はサポートされていない形式です");
                System.exit(0);
            }
         // 空のクリップを作成
            Clip clip = (Clip) AudioSystem.getLine(info);
            // クリップのイベントを監視
            clip.addLineListener(me);
            // オーディオストリームをクリップとして開く
            clip.open(stream);
            return clip;
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return null;
	}
	public static void play(File filePath){
		Clip se=seData.get(filePath);
		if(se==null){
			se=load(filePath);
		}
		if(se.getLongFramePosition()==0){
			se.start();
		}else{
			se=create(filePath);
			taskList.add(se);
			se.start();
		}
	}
	public static void dispose(File filePath){
		seData.remove(filePath);
	}
	public void update(LineEvent event) {
        // ストップか最後まで再生された場合
        if (event.getType() == LineEvent.Type.STOP) {
            Clip clip = (Clip) event.getSource();
            clip.stop();
            if(seData.containsValue(clip)){
            	clip.setFramePosition(0); // 再生位置を最初に戻す
            	if(taskList.contains(clip)){
            		taskList.remove(clip);
            		clip.start();
            	}
            }else{
            	while(taskList.remove(clip)){
            	}
            	clip.drain();
            	clip.close();
            }
        }
    }
}
