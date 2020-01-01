package com.davide.vgn;

import android.content.SharedPreferences;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RssFeedManager {
	public static final DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

	public static List<RssFeed> parseFeed(InputStream inputStream, boolean allowJS) throws Exception {
		List<RssFeed> items = new ArrayList<>();
		String channelTitle = null;
		String title = null;
		String link = null;
		String description = null;
		Date pubDate = new Date();
		String image = null;

		if (inputStream == null) {
			return items;
		}

		boolean isItem = false;
		XmlPullParser xmlPullParser = Xml.newPullParser();
		xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		xmlPullParser.setInput(inputStream, null);

		xmlPullParser.nextTag();
		while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
			int eventType = xmlPullParser.getEventType();

			String name = xmlPullParser.getName();
			//Log.d("MainActivity", "Parsing name ==> " + name);
			if (name == null)
				continue;

			if (eventType == XmlPullParser.END_TAG) {
				if (name.equalsIgnoreCase("item")) {
					if (title != null && link != null && description != null && pubDate != null) {
						if (link.contains("://multiplayer.it/") && image != null) {
							image = image.replace("_100x55_crop_", "_800x0_crop_");
						}
						items.add(new RssFeed(channelTitle, title, link, description, pubDate, image, allowJS));
						title = null;
						link = null;
						description = null;
						pubDate = null;
					}
					isItem = false;
				}
				continue;
			}

			if (eventType == XmlPullParser.START_TAG) {
				if (name.equalsIgnoreCase("item")) {
					isItem = true;
					continue;
				} else if (name.equalsIgnoreCase("channel")) {
					continue;
				}
			}

			String result = "";
			if (xmlPullParser.next() == XmlPullParser.TEXT) {
				result = xmlPullParser.getText().trim();
				xmlPullParser.nextTag();
			}
			//Log.d("MainActivity", "Parsing name ==> " + name + ": " + result);

			if (name.equalsIgnoreCase("title")) {
				if (isItem) {
					title = result;
				} else {
					channelTitle = result;
				}
			} else if (name.equalsIgnoreCase("link")) {
				link = result;
			} else if (name.equalsIgnoreCase("channelTitle")) {
				channelTitle = result;
			} else if (name.equalsIgnoreCase("description")) {
				description = result;
				if (description.contains("<img")) {
					if (description.contains("/>")) {
						image = description.substring(0, description.indexOf("/>"));
						image = image.substring(image.indexOf("src=\"") + 5);
						image = image.substring(0, image.indexOf("\""));
						description = description.substring(description.indexOf("/>") + 2).trim();
					}
				}
				if (description.contains("<")) {
					description = Html.fromHtml(description).toString();
					description = description.replaceAll("[\n|\r]+", " ");
					description = description.trim();
				}
			} else if (name.equalsIgnoreCase("pubDate")) {
				try {
					pubDate = RssFeedManager.formatter.parse(result);
				} catch (ParseException e) {
					Log.e("MainActivity", "DateFormat error: " + e.toString());
				}
			}
		}
		SharedPreferences.Editor editor = MainActivity.sp.edit();
		editor.putString("last_check", RssFeedManager.formatter.format(new Date()));
		editor.apply();
		return items;
	}

	public static String Serialize(RssFeed rssFeed) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(bo);
			oo.writeObject(rssFeed);
			oo.close();
			return new String(Base64.encode(bo.toByteArray(), Base64.DEFAULT));
		} catch (Exception e) {
			Log.e(MainActivity.TAG, "Serialize error: " + e.toString());
			return null;
		}
	}

	public static RssFeed Deserialize(String s) {
		if (s == null) {
			return null;
		}
		try {
			byte b[] = Base64.decode(s.getBytes(), Base64.DEFAULT);
			ByteArrayInputStream bi = new ByteArrayInputStream(b);
			ObjectInputStream oi = new ObjectInputStream(bi);
			return (RssFeed) oi.readObject();
		} catch (Exception e) {
			Log.e(MainActivity.TAG, "Deserialize error: " + e.toString());
			return null;
		}
	}

	public static String SerializeList(List<RssFeed> rssFeeds) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ObjectOutputStream oo = new ObjectOutputStream(bo);
			oo.writeObject(rssFeeds);
			oo.close();
			return new String(Base64.encode(bo.toByteArray(), Base64.DEFAULT));
		} catch (Exception e) {
			Log.e(MainActivity.TAG, "Serialize error: " + e.toString());
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<RssFeed> DeserializeList(String s) {
		if (s == null) {
			return new ArrayList<>();
		}
		try {
			byte b[] = Base64.decode(s.getBytes(), Base64.DEFAULT);
			ByteArrayInputStream bi = new ByteArrayInputStream(b);
			ObjectInputStream oi = new ObjectInputStream(bi);
			return (ArrayList<RssFeed>) oi.readObject();
		} catch (Exception e) {
			Log.e(MainActivity.TAG, "Deserialize error: " + e.toString());
			return new ArrayList<>();
		}
	}
}
