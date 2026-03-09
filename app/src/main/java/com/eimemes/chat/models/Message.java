package com.eimemes.chat.models;

public class Message {
    public static final String ROLE_USER      = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TYPING    = "typing"; // placeholder while streaming

    private String role;
    private String content;
    private String time;
    private boolean disclaimer;

    public Message() {}

    public Message(String role, String content, String time) {
        this.role    = role;
        this.content = content;
        this.time    = time;
    }

    public String getRole()       { return role; }
    public String getContent()    { return content; }
    public String getTime()       { return time; }
    public boolean isDisclaimer() { return disclaimer; }

    public void setContent(String content) { this.content = content; }
    public void setRole(String role)       { this.role = role; }
    public void setDisclaimer(boolean d)   { this.disclaimer = d; }
}
