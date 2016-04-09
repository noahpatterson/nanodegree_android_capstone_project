package me.noahpatterson.destinycasts;

import android.net.Uri;


/**
 * Created by noahpatterson on 4/9/16.
 */
public class Podcast {
    String name;
    int photo;// drawable reference id
    String rssFeedUrlString;

    public Podcast(String name, int photo, String rssFeedUrlString)
    {
        this.name = name;
        this.photo = photo;
        this.rssFeedUrlString = rssFeedUrlString;
    }
}
