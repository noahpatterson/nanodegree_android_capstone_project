package me.noahpatterson.destinycasts.model;


/**
 * Created by noahpatterson on 4/9/16.
 */
public class Podcast {
    public String name;
    public int photo;// drawable reference id
    public String rssFeedUrlString;
    public boolean isSelected;

    public Podcast(String name, int photo, String rssFeedUrlString)
    {
        this.name = name;
        this.photo = photo;
        this.rssFeedUrlString = rssFeedUrlString;
        isSelected = false;
    }

}
