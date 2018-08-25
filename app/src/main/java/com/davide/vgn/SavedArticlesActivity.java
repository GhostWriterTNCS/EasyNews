package com.davide.vgn;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

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
							}})
						.setNegativeButton(R.string.cancel, null).show();
				return true;
		}
		return super.onOptionsItemSelected(item);
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
