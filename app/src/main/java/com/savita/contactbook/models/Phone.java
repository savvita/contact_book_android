package com.savita.contactbook.models;

import java.io.Serializable;

public class Phone implements Serializable {
    private String number;
    private int type;

    public Phone(String number, int type) {
        this.number = number;
        this.type = type;
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
}
