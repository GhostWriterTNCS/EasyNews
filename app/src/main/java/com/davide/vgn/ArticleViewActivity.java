package com.davide.vgn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;

import javax.mail.AuthenticationFailedException;

public class ArticleViewActivity extends AppCompatActivity {
	private RssFeed rssFeed;
	private SwipeRefreshLayout mSwipeLayout;
	private WebView mWebView;
	private String url;
	private Menu menu;
	private FloatingActionButton fab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_view);

		mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);

		// Get the Intent that started this activity and extract the string
		Intent intent = getIntent();
		rssFeed = RssFeedManager.Deserialize(intent.getStringExtra(MainActivity.EXTRA_RSS_FEED));
		url = rssFeed.link;
		if (url.startsWith("https://it.ign.com/") && !url.startsWith("https://it.ign.com/m/")) {
			url = url.replace("https://it.ign.com/", "https://it.ign.com/m/");
		}
		Log.d(MainActivity.TAG, url);

		mWebView = ((WebView) findViewById(R.id.webView));
		if (url.startsWith("https://www.gamasutra.com/") || url.startsWith("http://feedproxy.google.com/")) {
			mWebView.getSettings().setJavaScriptEnabled(true);
		}
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

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (view.getHitTestResult().getType() > 0) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(i);
				} else {
					view.loadUrl(url);
				}
				return true;
			}
		});
		//if (android.os.Build.VERSION.SDK_INT >= 21) {
		mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		//}
		setTitle(rssFeed.title);

		mSwipeLayout.setRefreshing(true);
		mWebView.loadUrl(url);

		mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				mSwipeLayout.setRefreshing(true);
				mWebView.loadUrl(url);
			}
		});
		fab = findViewById(R.id.fab);
		ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null));
		if (!rssFeeds.contains(rssFeed)) {
			fab.setImageResource(R.drawable.ic_bookmark_outline);
		} else {
			fab.setImageResource(R.drawable.ic_bookmark);
		}
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				changeBookmarkStatus();
			}
		});
	}

	void changeBookmarkStatus() {
		ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null));
		boolean newStatus;
		if (!rssFeeds.contains(rssFeed)) {
			rssFeeds.add(0, rssFeed);
			fab.setImageResource(R.drawable.ic_bookmark);
			if (menu != null)
				menu.findItem(R.id.bookmark).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark, null));
		} else {
			rssFeeds.remove(rssFeed);
			fab.setImageResource(R.drawable.ic_bookmark_outline);
			if (menu != null)
				menu.findItem(R.id.bookmark).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark_outline, null));
		}
		SharedPreferences.Editor editor = MainActivity.sp.edit();
		editor.putString("saved_news", RssFeedManager.SerializeList(rssFeeds));
		editor.apply();
		MainActivity.updateRssFeedsSize();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		getMenuInflater().inflate(R.menu.article_menu, menu);
		MainActivity.sp = getSharedPreferences(MainActivity.TAG, MODE_PRIVATE);
		ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString("saved_news", null));
		if (rssFeeds.contains(rssFeed)) {
			menu.findItem(R.id.bookmark).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark, null));
		} else {
			menu.findItem(R.id.bookmark).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmark_outline, null));
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.bookmark:
				changeBookmarkStatus();
				return true;
			case R.id.send_email:
				Toast.makeText(ArticleViewActivity.this, MainActivity.context.getString(R.string.sending_email), Toast.LENGTH_SHORT).show();
				new SendEmailAsyncTask(ArticleViewActivity.this, "From VGN: " + rssFeed.title, rssFeed.link).execute();
				return true;
			case R.id.open_in_browser:
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(rssFeed.link));
				startActivity(browserIntent);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}


class SendEmailAsyncTask extends AsyncTask<Void, Void, Boolean> {
	GMailSender sender = new GMailSender("", "");
	AppCompatActivity activity;
	String title;
	String body;

	public SendEmailAsyncTask(AppCompatActivity activity, String title, String body) {
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
		} catch (AuthenticationFailedException e) {
			Log.e(SendEmailAsyncTask.class.getName(), "Bad account details");
			e.printStackTrace();
		/*} catch (MessagingException e) {
			Log.e(SendEmailAsyncTask.class.getName(), e.getMessage());
			e.printStackTrace();*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(activity, MainActivity.context.getString(R.string.send_email_error), Toast.LENGTH_LONG).show();
			}
		});
		return false;
	}
}
