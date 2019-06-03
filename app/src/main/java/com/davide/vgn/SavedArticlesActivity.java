package com.davide.vgn;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SavedArticlesActivity extends AppCompatActivity {

	private AppCompatActivity activity;
	private SwipeRefreshLayout mSwipeLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		activity = this;

		mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
		mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				new SavedFetchFeedTask(activity, mSwipeLayout).execute((Void) null);
			}
		});
		new SavedFetchFeedTask(activity, mSwipeLayout).execute((Void) null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.saved_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.trash:
				new AlertDialog.Builder(this)
						.setTitle(MainActivity.context.getString(R.string.delete_all))
						.setMessage(MainActivity.context.getString(R.string.are_you_sure))
						//.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								SharedPreferences.Editor editor = MainActivity.sp.edit();
								editor.remove("saved_news");
								editor.apply();
								new SavedFetchFeedTask(activity, mSwipeLayout).execute((Void) null);
							}
						})
						.setNegativeButton(R.string.cancel, null).show();
				return true;
			case R.id.export_json:
				ExportJSON();
				return true;
			case R.id.import_json:
				ImportJSON();
				return true;
			case R.id.send_json:
				SendJSON();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static String filename = "bookmarks.json";

	public String RssFeedsToJSON() {
		List<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null));
		JSONArray jsonObject = new JSONArray();
		try {
			for (RssFeed rf : rssFeeds) {
				JSONObject j = new JSONObject();
				j.put("channelTitle", rf.channelTitle);
				j.put("title", rf.title);
				j.put("link", rf.link);
				j.put("description", rf.description);
				j.put("pubDate", RssFeedManager.formatter.format(rf.pubDate));
				j.put("image", rf.image);
				jsonObject.put(j);
			}
			Log.d("JSON", jsonObject.toString());
			return jsonObject.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public void ExportJSON() {
		String json = RssFeedsToJSON();
		if (!json.isEmpty()) {
			if (CustomIO.WriteFile("VGN", filename, json)) {
				Toast.makeText(activity, MainActivity.context.getString(R.string.export_json_success), Toast.LENGTH_SHORT).show();
				return;
			}
		}
		Toast.makeText(activity, MainActivity.context.getString(R.string.an_error_occurred), Toast.LENGTH_LONG).show();
	}

	public void ImportJSON() {
		try {
			JSONArray jsonObject = new JSONArray(CustomIO.ReadFile("VGN", filename));
			List<RssFeed> rssFeeds = new ArrayList<RssFeed>();
			for (int i = 0; i < jsonObject.length(); i++) {
				JSONObject j = jsonObject.getJSONObject(i);
				rssFeeds.add(new RssFeed(
						j.getString("channelTitle"),
						j.getString("title"),
						j.getString("link"),
						j.getString("description"),
						RssFeedManager.formatter.parse(j.getString("pubDate")),
						j.has("image") ? j.getString("image") : null
				));
			}
			SharedPreferences.Editor editor = MainActivity.sp.edit();
			editor.putString("saved_news", RssFeedManager.SerializeList(rssFeeds));
			editor.apply();
			MainActivity.updateRssFeedsSize();
			new SavedFetchFeedTask(activity, mSwipeLayout).execute((Void) null);
			Toast.makeText(activity, MainActivity.context.getString(R.string.import_json_success), Toast.LENGTH_SHORT).show();
			Log.d("JSON", rssFeeds.size() + " bookmarks imported.");
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("JSON", "Bookmarks not imported.");
			Toast.makeText(activity, MainActivity.context.getString(R.string.an_error_occurred), Toast.LENGTH_LONG).show();
		}
	}

	public void SendJSON() {
		Toast.makeText(activity, MainActivity.context.getString(R.string.sending_email), Toast.LENGTH_SHORT).show();
		new SendEmailAsyncTask(SavedArticlesActivity.this, "From VGN: bookmarks", RssFeedsToJSON()).execute();
	}

	private class SavedFetchFeedTask extends FetchFeedTask {

		protected SavedFetchFeedTask(AppCompatActivity activity, SwipeRefreshLayout mSwipeLayout) {
			super(activity, mSwipeLayout);
		}

		@Override
		protected void onPreExecute() {
			mSwipeLayout.setRefreshing(true);
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null));
			return rssFeeds.size() != 0;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			activity.setTitle(MainActivity.context.getString(R.string.bookmarks) + " (" + rssFeeds.size() + ")");
			onPostExecute(success, true);
			mSwipeLayout.setRefreshing(false);
		}
	}
}
