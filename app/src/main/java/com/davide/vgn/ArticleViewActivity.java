package com.davide.vgn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;

public class ArticleViewActivity extends AppCompatActivity {
	private RssFeed rssFeed;
	private SwipeRefreshLayout mSwipeLayout;
	private WebView mWebView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_view);

		mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);

		// Get the Intent that started this activity and extract the string
		Intent intent = getIntent();
		rssFeed = RssFeedManager.Deserialize(intent.getStringExtra(MainActivity.EXTRA_RSS_FEED));
		mWebView = ((WebView) findViewById(R.id.webView));
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				mSwipeLayout.setRefreshing(false);
				super.onPageFinished(mWebView, url);
			}

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Toast.makeText(MainActivity.activity,
						"WebView error " + errorCode,
						Toast.LENGTH_LONG).show();
				Log.e(MainActivity.TAG, "WebView error " + errorCode
						+ ": " + description + " (url: " + failingUrl + ")");
			}
		});
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		}
		setTitle(rssFeed.title);

		mSwipeLayout.setRefreshing(true);
		mWebView.loadUrl(rssFeed.link);

		mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				mSwipeLayout.setRefreshing(true);
				mWebView.loadUrl(rssFeed.link);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.article_menu, menu);
		MainActivity.sp = getSharedPreferences(MainActivity.TAG, MODE_PRIVATE);
		ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null));
		if (!rssFeeds.contains(rssFeed)) {
			menu.findItem(R.id.bookmark).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark_outline, null));
		} else {
			menu.findItem(R.id.bookmark).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark, null));
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.bookmark:
				ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null));
				if (!rssFeeds.contains(rssFeed)) {
					rssFeeds.add(0, rssFeed);
					item.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark, null));
				} else {
					rssFeeds.remove(rssFeed);
					item.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark_outline, null));
				}
				SharedPreferences.Editor editor = MainActivity.sp.edit();
				editor.putString("saved_news", RssFeedManager.SerializeList(rssFeeds));
				editor.apply();
				MainActivity.updateRssFeedsSize();
				return true;
			case R.id.send_email:
				Toast.makeText(ArticleViewActivity.this, MainActivity.context.getString(R.string.sending_email), Toast.LENGTH_SHORT).show();
				new SendEmailAsyncTask(ArticleViewActivity.this,"From VGN: " + rssFeed.title, rssFeed.link).execute();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}

class SendEmailAsyncTask extends AsyncTask<Void, Void, Boolean> {
	GMailSender sender = new GMailSender("", "");
	ArticleViewActivity activity;
	String title;
	String body;

	public SendEmailAsyncTask(ArticleViewActivity activity, String title, String body) {
		this.activity = activity;
		this.title = title;
		this.body = body;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (BuildConfig.DEBUG) Log.v(SendEmailAsyncTask.class.getName(), "doInBackground()");
		try {
			if (sender.sendMail(title, // subject
					body, // body
					"bot@vgn.com", // sender
					"")) { // recipients
				return true;
			}
			return true;
		} catch (AuthenticationFailedException e) {
			Log.e(SendEmailAsyncTask.class.getName(), "Bad account details");
			e.printStackTrace();
		} catch (MessagingException e) {
			Log.e(SendEmailAsyncTask.class.getName(), e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Toast.makeText(activity, MainActivity.context.getString(R.string.send_email_error), Toast.LENGTH_LONG).show();
		return false;
	}
}
