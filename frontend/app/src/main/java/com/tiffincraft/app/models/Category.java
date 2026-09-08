package com.tiffincraft.app.models;

public class Category {
    private String slug;
    private String emoji;
    private String name;
    private boolean isSelected;

    public Category(String slug, String emoji, String name) {
        this.slug = slug;
        this.emoji = emoji;
        this.name = name;
        this.isSelected = false;
    }

    public String getSlug() {
        return slug;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
