package com.savita.contactbook.models;

import java.io.Serializable;

public class Phone implements Serializable {
    private String id;
    private String number;
    private int type;

    public Phone(String number, int type) {
        this.number = number;
        this.type = type;
    }

    public Phone(String id, String number, int type) {
        this(number, type);
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
