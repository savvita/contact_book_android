package com.savita.contactbook;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.savita.contactbook.controllers.MediaController;
import com.savita.contactbook.models.Contact;
import com.savita.contactbook.models.Phone;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    private final String LOG_TAG = "contact_view_log";
    private final int REQUEST_PERMISSION_WRITE_CONTACTS = 2;
    private final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 3;
    private final int REQUEST_PERMISSION_CAMERA = 4;
    private final int IMAGE_PICK_REQUEST = 12345;

    private final Set<String> photoPermissions = new HashSet<>();
    private final String GALLERY = "Gallery";
    private final String CAMERA = "Camera";
    private final String CANCEL = "Cancel";

    private String capturedPhoto;

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


        contactName.setOnKeyListener((view, i, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                contact.setDisplayName(contactName.getText().toString());
            }
            return false;
        });

        contactEmail.setOnKeyListener((view, i, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                contact.setEmail(contactEmail.getText().toString());
            }
            return false;
        });

        contactMobilePhone.setOnKeyListener((view, i, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                setPhone(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, contactMobilePhone.getText().toString());
            }
            return false;
        });

        contactHomePhone.setOnKeyListener((view, i, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                setPhone(ContactsContract.CommonDataKinds.Phone.TYPE_HOME, contactHomePhone.getText().toString());
            }
            return false;
        }
        );
        contactWorkPhone.setOnKeyListener((view, i, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                setPhone(ContactsContract.CommonDataKinds.Phone.TYPE_WORK, contactWorkPhone.getText().toString());
            }
            return false;
        });

        initializeContact();
        checkPermissions();
    }

    private void setPhone(int type, String number) {
        Phone phone = contact.getPhones().stream().filter(x -> x.getType() == type).findFirst().orElse(null);
        if(phone == null) {
            phone = new Phone(number, type);
            contact.getPhones().add(phone);
        } else {
            phone.setNumber(number);
        }
    }

    private void showPhotoChoiceDialog() {
        Log.d(LOG_TAG, "On showPhotoChoiceDialog");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setIcon(R.mipmap.ic_launcher_round);
        dialogBuilder.setTitle("Select image from");

        String[] options = new String[photoPermissions.size() + 1];
        photoPermissions.toArray(options);
        options[options.length - 1] = CANCEL;

        dialogBuilder.setItems(options, (dialog, i) -> showPhotoActivity(dialog, options, i));

        dialogBuilder.show();
    }

    private void showPhotoActivity(DialogInterface dialog, String[] options, int i) {
        if (options[i].equals(GALLERY)) {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(
                    Intent.createChooser(intent, "Select profile picture"), IMAGE_PICK_REQUEST);
        } else if(options[i].equals(CAMERA)) {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

            capturedPhoto = String.valueOf(System.currentTimeMillis()) + ".jpg";
            File f = new File(android.os.Environment.getExternalStorageDirectory(), capturedPhoto);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                    getPackageName() + ".provider", f));
            startActivityForResult(Intent.createChooser(intent, "Select profile picture"), IMAGE_PICK_REQUEST);
        } else if(options[i].equals(CANCEL)) {
            dialog.dismiss();
        } else {
            Toast.makeText(this, "Oops...", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    String imagePath = MediaController.getRealPathFromURI(this, selectedImageUri);
                    contactPhoto.setImageURI(Uri.parse(imagePath));
                    contact.setPhoto(imagePath);
                }
                else  {
                    String imagePath = MediaController.getMedia(capturedPhoto);
                    Log.d(LOG_TAG, "Captured image path : " + imagePath);
                    if(imagePath != null) {
                        contactPhoto.setImageURI(Uri.parse(imagePath));
                        contact.setPhoto(imagePath);
                    }
                }
            }
        }
    }


    protected void onResume() {
        checkPermissions();
        super.onResume();
    }

    private void checkPermissions() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            saveBtn.setOnClickListener((view) ->
                    ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.WRITE_CONTACTS }, REQUEST_PERMISSION_WRITE_CONTACTS));
            contactPhoto.setOnClickListener(null);
        } else {
            saveBtn.setOnClickListener((view) -> saveContact());

            contactPhoto.setOnClickListener((view) -> checkPhotoPermissions());
        }
    }


    private void checkPhotoPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
            contactPhoto.setOnClickListener((view) -> checkPhotoPermissions());
        } else {
            photoPermissions.add(GALLERY);
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_CAMERA);
            contactPhoto.setOnClickListener((view) -> checkPhotoPermissions());
        } else {
            photoPermissions.add(CAMERA);
        }


        if(photoPermissions.size() > 0) {
            contactPhoto.setOnClickListener((view) -> showPhotoChoiceDialog());
            showPhotoChoiceDialog();
        } else {
            contactPhoto.setOnClickListener((view) -> checkPhotoPermissions());
        }
    }

    private void saveContact() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_WRITE_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveBtn.setOnClickListener((view) -> saveContact());
            } else {
                saveBtn.setOnClickListener((view) ->
                        ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.WRITE_CONTACTS }, REQUEST_PERMISSION_WRITE_CONTACTS));
            }
        }

        if (requestCode == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                photoPermissions.add(GALLERY);
            } else {
                photoPermissions.remove(GALLERY);
            }
        }

        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                photoPermissions.add(CAMERA);
            } else {
                photoPermissions.remove(CAMERA);
            }
        }

        if(photoPermissions.size() > 0) {
            contactPhoto.setOnClickListener((view) -> showPhotoChoiceDialog());
        } else {
            contactPhoto.setOnClickListener((view) -> checkPhotoPermissions());
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