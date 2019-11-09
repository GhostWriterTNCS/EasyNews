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
import java.util.HashSet;
import java.util.Set;

import javax.mail.AuthenticationFailedException;

import static com.davide.vgn.MainActivity.context;

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
		/*if (urls.startsWith("https://it.ign.com/") && !urls.startsWith("https://it.ign.com/m/")) {
			urls = urls.replace("https://it.ign.com/", "https://it.ign.com/m/");
		}*/
		Log.d(MainActivity.TAG, url);

		mWebView = ((WebView) findViewById(R.id.webView));
		String domain = url.substring(url.indexOf("//") + 2);
		domain = domain.substring(0, domain.indexOf("/"));
		if (MainActivity.sp.getString(Strings.urlsJS, "").contains("://" + domain + "/")) {
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
						+ ": " + description + " (urls: " + failingUrl + ")");
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
		ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString(Strings.savedNews, null));
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

		Set<String> viewedArticles = new HashSet<>();
		viewedArticles.addAll(MainActivity.sp.getStringSet(Strings.viewed, new HashSet<String>()));
		viewedArticles.add(rssFeed.link);
		SharedPreferences.Editor editor = MainActivity.sp.edit();
		editor.putStringSet(Strings.viewed, viewedArticles);
		editor.apply();
	}

	void changeBookmarkStatus() {
		ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString(Strings.savedNews, null));
		if (!rssFeeds.contains(rssFeed)) {
			rssFeeds.add(rssFeed);
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
		editor.putString(Strings.savedNews, RssFeedManager.SerializeList(rssFeeds));
		editor.apply();
		MainActivity.updateRssFeedsSize();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		getMenuInflater().inflate(R.menu.article_menu, menu);
		MainActivity.sp = getSharedPreferences(MainActivity.TAG, MODE_PRIVATE);
		ArrayList<RssFeed> rssFeeds = RssFeedManager.DeserializeList(MainActivity.sp.getString(Strings.savedNews, null));
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
				Toast.makeText(ArticleViewActivity.this, context.getString(R.string.sending_email), Toast.LENGTH_SHORT).show();
				new SendEmailAsyncTask(ArticleViewActivity.this, "[Easy News] " + rssFeed.title, rssFeed.link).execute();
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
	AppCompatActivity activity;
	String subject;
	String body;

	public SendEmailAsyncTask(AppCompatActivity activity, String subject, String body) {
		this.activity = activity;
		this.subject = subject;
		this.body = body;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (MainActivity.sp.getString(Strings.emailFrom, "").isEmpty()) {
			String mailto = "mailto:" + MainActivity.sp.getString(Strings.emailTo, "") +
					"?subject=" + Uri.encode(subject) +
					"&body=" + Uri.encode(body);
			Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(mailto));
			activity.startActivity(Intent.createChooser(emailIntent, MainActivity.context.getString(R.string.send_email)));
			return true;
		} else {
			GMailSender sender = new GMailSender(MainActivity.sp.getString(Strings.emailFrom, ""), MainActivity.sp.getString(Strings.emailPassword, ""));
			try {
				if (sender.sendMail(subject, // subject
						body, // body
						"bot@easynews.com", // sender
						MainActivity.sp.getString(Strings.emailTo, ""))) { // recipients
					return true;
				}
			} catch (AuthenticationFailedException e) {
				Toast.makeText(activity, context.getString(R.string.send_email_error), Toast.LENGTH_LONG).show();
				e.printStackTrace();
		/*} catch (MessagingException e) {
			Log.e(SendEmailAsyncTask.class.getName(), e.getMessage());
			e.printStackTrace();*/
			} catch (Exception e) {
				e.printStackTrace();
			}
			activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(activity, context.getString(R.string.send_email_error), Toast.LENGTH_LONG).show();
				}
			});
			return false;
		}
	}
}
