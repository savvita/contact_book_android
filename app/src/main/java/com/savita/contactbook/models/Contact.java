package com.savita.contactbook.models;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Contact implements Serializable {
    private String id;
    private String displayName;
    private List<Phone> phones = new ArrayList<>();
    private String email;
    private String photoUri;

    public static final String NAME = "name";
    public static final String MOBILE_PHONE = "mobile_phone";
    public static final String HOME_PHONE = "home_phone";
    public static final String WORK_PHONE = "work_phone";

    public static final String EMAIL = "email";
    public static final String PHOTO = "photo";

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

    public String getPhoto() {
        return photoUri;
    }

    public void setPhoto(String photoUri) {
        this.photoUri = photoUri;
    }
}
