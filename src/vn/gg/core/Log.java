package vn.gg.core;

import com.unity3d.player.UnityPlayer;

import android.widget.Toast;

/**
 * 
 * @author truongps
 *
 */
public class Log {
	
	public static void log(final String TAG, final String message) {
		UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(UnityPlayer.currentActivity, TAG + " - " + message, Toast.LENGTH_SHORT).show();	
			}
		});
		android.util.Log.d(TAG, message);
	}
	
	public static void error(String TAG, String message) {
		android.util.Log.e(TAG, message);
	}
}
