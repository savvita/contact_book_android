package com.savita.contactbook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputEditText;
import com.savita.contactbook.models.Contact;
import com.savita.contactbook.models.Phone;

import java.util.List;

public class ContactActivity extends AppCompatActivity {
    private Contact contact;
    private TextInputEditText contactName;
    private TextInputEditText contactEmail;
    private TextInputEditText contactMobilePhone;
    private TextInputEditText contactHomePhone;
    private TextInputEditText contactWorkPhone;
    private ImageView contactPhoto;
    private Button saveBtn;

    private final String CONTACT_KEY = "contact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        contactName = findViewById(R.id.contact_view_name);
        contactEmail = findViewById(R.id.contact_view_email);
        contactMobilePhone = findViewById(R.id.contact_view_mobile_phone);
        contactHomePhone = findViewById(R.id.contact_view_home_phone);
        contactWorkPhone = findViewById(R.id.contact_view_work_phone);
        contactPhoto = findViewById(R.id.contact_view_photo);
        saveBtn = findViewById(R.id.save_contact_btn);

        initializeContact();
        checkPermissions();
    }

    protected void onResume() {
        checkPermissions();
        super.onResume();
    }

    private void checkPermissions() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            saveBtn.setVisibility(View.GONE);
        } else {
            saveBtn.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(CONTACT_KEY, contact);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        contact = (Contact)savedInstanceState.getSerializable(CONTACT_KEY);
        setContactData();
    }

    private void initializeContact() {
        contact = new Contact();
        Intent intent = getIntent();

        if(intent.hasExtra(Contact.NAME)) {
            contact.setDisplayName(intent.getStringExtra(Contact.NAME));
        }

        if(intent.hasExtra(Contact.MOBILE_PHONE)) {
            contact.getPhones().add(new Phone(intent.getStringExtra(Contact.MOBILE_PHONE), ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE));
        }

        if(intent.hasExtra(Contact.WORK_PHONE)) {
            contact.getPhones().add(new Phone(intent.getStringExtra(Contact.WORK_PHONE), ContactsContract.CommonDataKinds.Phone.TYPE_WORK));
        }

        if(intent.hasExtra(Contact.HOME_PHONE)) {
            contact.getPhones().add(new Phone(intent.getStringExtra(Contact.HOME_PHONE), ContactsContract.CommonDataKinds.Phone.TYPE_HOME));
        }

        if(intent.hasExtra(Contact.EMAIL)) {
            contact.setEmail(intent.getStringExtra(Contact.EMAIL));
        }

        if(intent.hasExtra(Contact.PHOTO)) {
            contact.setPhoto(intent.getStringExtra(Contact.PHOTO));
        }

        setContactData();
    }

    private void setContactData() {
        if(contact != null) {
            contactName.setText(contact.getDisplayName());
            contactEmail.setText(contact.getEmail());
            String photo = contact.getPhoto();
            if(photo != null) {
                contactPhoto.setImageURI(Uri.parse(photo));
            }
            List<Phone> phones = contact.getPhones();
            phones.forEach(phone -> {
                if(phone.getType() == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                    contactMobilePhone.setText(phone.getNumber());
                } else if(phone.getType() == ContactsContract.CommonDataKinds.Phone.TYPE_WORK) {
                    contactWorkPhone.setText(phone.getNumber());
                } else if(phone.getType() == ContactsContract.CommonDataKinds.Phone.TYPE_HOME) {
                    contactHomePhone.setText(phone.getNumber());
                }
            });
        }
    }
}