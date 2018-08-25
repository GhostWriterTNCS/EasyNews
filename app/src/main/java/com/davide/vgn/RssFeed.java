package com.davide.vgn;

import android.util.Log;

import java.io.Serializable;
import java.util.Date;

public class RssFeed implements Serializable {

    public String channelTitle;
    public String title;
    public String link;
    public String description;
    public Date pubDate;
    public String image;

    public RssFeed(String channelTitle, String title, String link, String description, Date pubDate, String image) {
        this.channelTitle = channelTitle;
        this.title = title;
        this.pubDate = pubDate;
        this.link = link;
        this.description = description;
        this.image = image;
    }

    @Override
	public boolean equals(Object other) {
    	if(other != null && other.getClass() == RssFeed.class) {
    		RssFeed o = (RssFeed)other;
    		try {
			    return channelTitle.equals(o.channelTitle) &&
					    title.equals(o.title) &&
					    link.equals(o.link) &&
					    description.equals(o.description) &&
					    pubDate.equals(o.pubDate) &&
					    (image == null && o.image == null || (image != null && image.equals(o.image)));
		    } catch(Exception e) {
    			Log.e(MainActivity.TAG, "RssFeed.equals error: " + e.toString());
		    }
	    }
	    return false;
    }
}
