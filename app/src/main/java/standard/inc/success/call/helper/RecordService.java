package standard.inc.success.call.helper;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

public class RecordService {

  private MediaRecorder mediaRecorder;
  private String fileName;
  private final String path;

  RecordService(Context context) {
    path = context.getFilesDir().getPath();
  }

  public static final String TAG = "RecordService";

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void startRecord() {
    String file = path + "/" + fileName;
    mediaRecorder = new MediaRecorder();
    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    mediaRecorder.setAudioChannels(1);
    mediaRecorder.setOutputFile(file);
    mediaRecorder.setAudioEncodingBitRate(44100 * 16);
    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    mediaRecorder.setAudioSamplingRate(44100);

    try {
      mediaRecorder.prepare();
      mediaRecorder.start();

      Log.d(TAG, "started: " + file);
    } catch (IOException e) {
      Log.e(TAG, "Microphone is already in use by another app. IOException: " + e);
    } catch (IllegalStateException e) {
      Log.e(TAG, "Microphone is already in use by another app. IllegalStateException: " + e);
    } catch (Exception e) {
      Log.e(TAG, "Something went wrong in start recording. Exception: " + e);
    }
  }

  public String stopRecord() {
    if (mediaRecorder == null) return "";
    try {
      mediaRecorder.stop();
      Log.i(TAG, "stop record");
    } catch (Exception e) {
      Log.e("stopping_failed", "Stop failed:" + e);
    }

    try {
      mediaRecorder.reset();
      mediaRecorder.release();
    } catch (Exception e) {
      Log.e("stopping_failed", "Stop failed:" + e);
    }

    return FileContentProvider.getUriForFile(fileName);
  }
}
