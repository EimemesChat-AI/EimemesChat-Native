package com.eimemes.chat.models;

public class Conversation {
    private String id;
    private String title;

    public Conversation(String id, String title) {
        this.id    = id;
        this.title = title;
    }

    public String getId()    { return id; }
    public String getTitle() { return title != null ? title : "New conversation"; }
    public void setTitle(String title) { this.title = title; }
}
