package com.davide.vgn;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
	public static final String TAG = "MainActivity";
	public static final String EXTRA_RSS_FEED = ".RSS_FEED";

	public static Context context;
	public static SharedPreferences sp;

	public static AppCompatActivity activity;
	private SwipeRefreshLayout mSwipeLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		activity = this;

		sp = getSharedPreferences(MainActivity.TAG, MainActivity.MODE_PRIVATE);
		context = getApplicationContext();

		isReadStoragePermissionGranted();
		isWriteStoragePermissionGranted();

		if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler("VGN/log"));
		}

		mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
		mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				new FetchFeedTask(activity, mSwipeLayout).execute((Void) null);
			}
		});
		new FetchFeedTask(activity, mSwipeLayout).execute((Void) null);
	}

	public boolean isReadStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
					== PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG, "Read permission is granted.");
				return true;
			} else {

				Log.v(TAG, "Read permission is revoked.");
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
				return false;
			}
		} else { //permission is automatically granted on sdk<23 upon installation
			Log.v(TAG, "Read permission is granted.");
			return true;
		}
	}

	public boolean isWriteStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
					== PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG, "Write permission is granted.");
				return true;
			} else {

				Log.v(TAG, "Write permission is revoked.");
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
				return false;
			}
		} else { //permission is automatically granted on sdk<23 upon installation
			Log.v(TAG, "Write permission is granted.");
			return true;
		}
	}

	private static TextView bookmarksCount;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		final View menu_hotlist = menu.findItem(R.id.bookmarks).getActionView();
		bookmarksCount = (TextView) menu_hotlist.findViewById(R.id.text);
		int rssFeedsSize = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null)).size();
		bookmarksCount.setText(Integer.toString(rssFeedsSize));
		RelativeLayout rl = (RelativeLayout) menu.findItem(R.id.bookmarks).getActionView();
		rl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d(MainActivity.TAG, "BOOKMARKS");
				Intent intent = new Intent(MainActivity.activity, SavedArticlesActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	public static void updateRssFeedsSize() {
		int rssFeedsSize = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null)).size();
		bookmarksCount.setText(Integer.toString(rssFeedsSize));
	}
}
