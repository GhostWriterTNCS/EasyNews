package com.davide.vgn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {
	private String[] feeds;
	private String[] feedsJS;
	protected ArrayList<RssFeed> rssFeeds = new ArrayList<>();
	protected AppCompatActivity activity;
	protected SwipeRefreshLayout mSwipeLayout;
	public static Date previousDate = null;
	int newFeedsCount = 0;
	String timestampLink = "timestamp";

	protected FetchFeedTask(AppCompatActivity activity, SwipeRefreshLayout mSwipeLayout) {
		this.activity = activity;
		this.mSwipeLayout = mSwipeLayout;
	}

	@Override
	protected void onPreExecute() {
		mSwipeLayout.setRefreshing(true);
		//String urls = MainActivity.sp.getString(Strings.urls, "") + "\n" + MainActivity.sp.getString(Strings.urlsJS, "");
		feeds = MainActivity.sp.getString(Strings.urls, "").split("\n");
		feedsJS = MainActivity.sp.getString(Strings.urlsJS, "").split("\n");
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		for (String urlLink : feeds) {
			if (!ParseUrl(urlLink, false)) {
				return false;
			}
		}
		for (String urlLink : feedsJS) {
			if (!ParseUrl(urlLink, true)) {
				return false;
			}
		}
		if (rssFeeds.size() == 0) {
			return false;
		}

		if (MainActivity.sp.getString(Strings.previousTimestamp, null) != null) {
			try {
				Date previous = RssFeedManager.formatter.parse(MainActivity.sp.getString(Strings.previousTimestamp, null));
				DateFormat format = new SimpleDateFormat("HH:mm");
				rssFeeds.add(new RssFeed(null, "Timestamp " + format.format(previous), timestampLink, null, previous, null, false));
			} catch (Exception ex) {

			}
		}
		if (MainActivity.sp.getString(Strings.previousTimestampBackup, null) != null) {
			try {
				Date previous = RssFeedManager.formatter.parse(MainActivity.sp.getString(Strings.previousTimestampBackup, null));
				rssFeeds.add(new RssFeed(null, "Timestamp (backup)", timestampLink, null, previous, null, false));
			} catch (Exception ex) {

			}
		}

		Collections.sort(rssFeeds, new Comparator<RssFeed>() {
			@Override
			public int compare(RssFeed a, RssFeed b) {
				return b.pubDate.compareTo(a.pubDate);
			}
		});

		for (int i = 0; i < rssFeeds.size(); i++) {
			if (rssFeeds.get(i).link == timestampLink) {
				break;
			}
			newFeedsCount++;
		}

		int previousLogLength = 10;
		boolean previousFound = false;
		List<String> lastNews = new ArrayList<String>();
		for (int index = 0; index < previousLogLength; index++) {
			if (rssFeeds.size() <= index) {
				previousLogLength = index;
				break;
			}
			lastNews.add(rssFeeds.get(index).link);
		}

		for (int index = 0; index < previousLogLength; index++) {
			if (previousFound) {
				break;
			}
			//newFeedsCount = 0;
			String prevLastNews = MainActivity.sp.getString(Strings.previous_ + index, null);
			if (prevLastNews != null) {
				for (int i = 0; i < rssFeeds.size(); i++) {
					String s = rssFeeds.get(i).link;
					if (s.equals(prevLastNews)) {
						rssFeeds.add(i, new RssFeed(null, MainActivity.context.getString(R.string.old_news), null, null, null, null, false));
						previousDate = rssFeeds.get(i).pubDate;
						previousFound = true;
						break;
					}
					//newFeedsCount++;
				}
			}
		}

		boolean previousBackupFound = false;
		for (int index = 0; index < previousLogLength; index++) {
			if (previousBackupFound) {
				break;
			}
			String prevLastNews = MainActivity.sp.getString(Strings.previousBackup_ + index, "");
			if (prevLastNews != null) {
				for (int i = 0; i < rssFeeds.size(); i++) {
					String s = rssFeeds.get(i).link;
					if (s != null && s.equals(prevLastNews)) {
						rssFeeds.add(i, new RssFeed(null, MainActivity.context.getString(R.string.old_news_backup), null, null, null, null, false));
						previousDate = rssFeeds.get(i).pubDate;
						previousBackupFound = true;
						break;
					}
				}
			}
		}

		if (!previousFound) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity,
							MainActivity.context.getResources().getText(R.string.previous_last_not_found),
							Toast.LENGTH_LONG).show();
				}
			});
			//newFeedsCount = 0;
		}

		SharedPreferences.Editor editor = MainActivity.sp.edit();
		for (int index = 0; index < previousLogLength; index++) {
			if (MainActivity.sp.getString(Strings.previous_ + index, null) != null) {
				editor.putString(Strings.previousBackup_ + index, MainActivity.sp.getString(Strings.previous_ + index, null));
			}
		}
		for (int index = 0; index < lastNews.size(); index++) {
			editor.putString(Strings.previous_ + index, lastNews.get(index));
		}
		if (MainActivity.sp.getString(Strings.previousTimestamp, null) != null) {
			editor.putString(Strings.previousTimestampBackup, MainActivity.sp.getString(Strings.previousTimestamp, null));
		}
		editor.putString(Strings.previousTimestamp, RssFeedManager.formatter.format(new Date()));
		editor.apply();
		return true;
	}

	boolean ParseUrl(String urlLink, boolean allowJS) {
		if (urlLink.isEmpty() || urlLink.startsWith("#")) {
			return true;
		}
		try {
			if (!urlLink.startsWith("http://") && !urlLink.startsWith("https://")) {
				urlLink = "http://" + urlLink;
			}
			URL url = new URL(urlLink);
			InputStream inputStream = url.openConnection().getInputStream();
			Log.d(MainActivity.TAG, urlLink);
			rssFeeds.addAll(RssFeedManager.parseFeed(inputStream, allowJS));
			inputStream.close();
		} catch (Exception e) {
			Log.e(MainActivity.TAG, "Error", e);
			final String s = urlLink;
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity, s + " failed", Toast.LENGTH_LONG).show();
				}
			});
			if (MainActivity.attempts > 0) {
				MainActivity.attempts--;
				return false;
			}
		}
		return true;
	}


	@Override
	protected void onPostExecute(Boolean success) {
		onPostExecute(success, false);
		mSwipeLayout.setRefreshing(false);
	}

	protected void onPostExecute(Boolean success, final boolean bookmarks) {
		Set<String> viewedArticles = new HashSet<>();
		viewedArticles.addAll(MainActivity.sp.getStringSet(Strings.viewed, new HashSet<String>()));

		LinearLayout linearLayout = (LinearLayout) ((Activity) activity).findViewById(R.id.verticalLayout);
		linearLayout.removeAllViewsInLayout();
		if (!success) {
			rssFeeds.clear();
			rssFeeds.add(new RssFeed(null, MainActivity.context.getString(R.string.no_news), null, null, null, null, false));
		}

		int i = 1;
		for (final RssFeed rssFeed : rssFeeds) {
			View child = activity.getLayoutInflater().inflate(R.layout.item_rss_feed, null);

			if (rssFeed.link == null || rssFeed.link == timestampLink) {
				((TextView) child.findViewById(R.id.breakText)).setText(rssFeed.title);
				child.findViewById(R.id.titleGroup).setVisibility(View.GONE);
				child.findViewById(R.id.subtitleText).setVisibility(View.GONE);
				child.findViewById(R.id.descriptionText).setVisibility(View.GONE);
				//i = 0;
			} else {
				child.findViewById(R.id.verticalLayout).setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View view) {
						ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString(Strings.savedNews, null));
						if (!rssFeeds.contains(rssFeed)) {
							rssFeeds.add(rssFeed);
							Toast.makeText(MainActivity.context, "[+] " + rssFeed.title, Toast.LENGTH_SHORT).show();
						}
						SharedPreferences.Editor editor = MainActivity.sp.edit();
						editor.putString(Strings.savedNews, RssFeedManager.SerializeList(rssFeeds));
						editor.apply();
						MainActivity.updateRssFeedsSize();
						return true;
					}
				});
				child.findViewById(R.id.verticalLayout).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent;
						if (bookmarks) {
							intent = new Intent(activity, SavedArticleViewActivity.class);
						} else {
							intent = new Intent(activity, ArticleViewActivity.class);
						}
						intent.putExtra(MainActivity.EXTRA_RSS_FEED, RssFeedManager.Serialize(rssFeed));
						activity.startActivity(intent);
					}
				});

				child.findViewById(R.id.breakText).setVisibility(View.GONE);
				((TextView) child.findViewById(R.id.titleText)).setText(rssFeed.title);
				if (rssFeed.description.length() > 0) {
					TextView tv = (TextView) child.findViewById(R.id.descriptionText);
					tv.setEllipsize(TextUtils.TruncateAt.END);
					tv.setMaxLines(4);
					tv.setText(rssFeed.description);
				} else {
					child.findViewById(R.id.descriptionText).setVisibility(View.GONE);
				}
				String channelTitle = rssFeed.channelTitle;
				if (channelTitle == null && rssFeed.link != null) {
					channelTitle = rssFeed.link;
					channelTitle = channelTitle.substring(channelTitle.indexOf("://") + 3);
					channelTitle = channelTitle.substring(0, channelTitle.indexOf("/"));
				}
				DateFormat myFormatter = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
				String count = "";
				if (newFeedsCount > 0 && i <= newFeedsCount) {
					count = i + "/" + newFeedsCount + " ";
					i++;
				}
				String subtitle = count + channelTitle + " - " + myFormatter.format(rssFeed.pubDate);
				((TextView) child.findViewById(R.id.subtitleText)).setText(subtitle);
				if (rssFeed.image != null) {
					ImageView imageView = ((ImageView) child.findViewById(R.id.imageView));
					imageView.setVisibility(View.VISIBLE);
					Picasso.get().load(rssFeed.image).into(imageView);
				}

				if (viewedArticles.contains(rssFeed.link)) {
					((TextView) child.findViewById(R.id.titleText)).setTextColor(MainActivity.activity.getResources().getColor(R.color.colorPrimary));
					((TextView) child.findViewById(R.id.subtitleText)).setTextColor(MainActivity.activity.getResources().getColor(R.color.colorPrimary));
					((TextView) child.findViewById(R.id.descriptionText)).setTextColor(MainActivity.activity.getResources().getColor(R.color.colorPrimary));
				}
			}

			linearLayout.addView(child);
			child.requestLayout();
		}
	}
}
