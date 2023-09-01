package com.savita.contactbook.models;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class Contact {
    private String id;
    private String displayName;
    private List<Phone> phones = new ArrayList<>();
    private String email;
    private Bitmap photo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<Phone> getPhones() {
        return phones;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Bitmap getPhoto() {
        return photo;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
    }
}
