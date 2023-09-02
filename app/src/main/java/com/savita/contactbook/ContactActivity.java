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
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.savita.contactbook.controllers.ContactController;
import com.savita.contactbook.controllers.MediaController;
import com.savita.contactbook.models.Contact;
import com.savita.contactbook.models.Phone;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContactActivity extends AppCompatActivity {
    private Contact contact;
    private TextInputEditText contactName;
    private TextInputEditText contactEmail;
    private TextInputEditText contactMobilePhone;
    private TextInputEditText contactHomePhone;
    private TextInputEditText contactWorkPhone;
    private ImageView contactPhoto;
    private Button saveBtn;
    private ImageButton deletePhotoBtn;

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
    private String errorMessage = "";

    private final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);


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
        deletePhotoBtn = findViewById(R.id.contact_view_delete_photo_btn);

        contactName.addTextChangedListener(new EditTextWatcher(contactName, (str) -> contact.setDisplayName(str)));
        contactEmail.addTextChangedListener(new EditTextWatcher(contactEmail, (str) -> contact.setEmail(str)));
        contactMobilePhone.addTextChangedListener(new EditTextWatcher(contactMobilePhone,
                (str) -> setPhone(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, str)));
        contactHomePhone.addTextChangedListener(new EditTextWatcher(contactHomePhone,
                (str) -> setPhone(ContactsContract.CommonDataKinds.Phone.TYPE_HOME, str)));
        contactWorkPhone.addTextChangedListener(new EditTextWatcher(contactWorkPhone,
                (str) -> setPhone(ContactsContract.CommonDataKinds.Phone.TYPE_WORK, str)));

        deletePhotoBtn.setOnClickListener((view) -> deletePhoto());

        initializeContact();
        checkPermissions();
    }

    private void deletePhoto() {
        contact.setPhoto(null);
        contactPhoto.setImageResource(R.drawable.ic_launcher_foreground);
        deletePhotoBtn.setVisibility(View.GONE);
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
                    BuildConfig.APPLICATION_ID + ".provider", f));
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
                    String imagePath = MediaController.getAbsolutePath(capturedPhoto);
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
            deletePhotoBtn.setVisibility(View.GONE);
        } else {
            saveBtn.setOnClickListener((view) -> saveContact());
            if(contact.getPhoto() != null) {
                deletePhotoBtn.setVisibility(View.VISIBLE);
            }
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
        if(!validateContact()) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            return;
        }

        if(contact.getId() == null || contact.getId().isEmpty()) {
            if(ContactController.create(getContentResolver(), contact, getApplicationContext())) {
                backToContactListActivity();
            } else {
                Toast.makeText(this, "Oops...", Toast.LENGTH_LONG).show();
            }
        } else {
            if(ContactController.update(getApplicationContext(), contact)) {
                backToContactListActivity();
            } else {
                Toast.makeText(this, "Oops...", Toast.LENGTH_LONG).show();
            }
        }
    }


    private boolean validateContact() {
        List<String> errors = new ArrayList<>();

        if(contact == null) {
            errors.add("Contact is null");
        } else {
            prepareContact();
            if(contact.getDisplayName() == null) {
                errors.add("Name is required");
            }

            if(contact.getEmail() != null && !validateEmail(contact.getEmail())) {
                errors.add("Incorrect email");
            }
        }

        if(errors.size() > 0) {
            errorMessage = String.join("\n", errors);
        } else {
            errorMessage = "";
        }

        return errorMessage.isEmpty();
    }


    public boolean validateEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.matches();
    }

    private void prepareContact() {
        contact.setDisplayName(trimString(contact.getDisplayName()));
        contact.setEmail(trimString(contact.getEmail()));
        contact.getPhones().forEach(phone -> phone.setNumber(trimString(phone.getNumber())));
        contact.setPhones(contact.getPhones().stream().filter(phone -> phone.getNumber() != null).collect(Collectors.toList()));
    }

    private String trimString(String str) {
        if(str == null) return null;
        str = str.trim();
        if(str.length() == 0) return null;
        return str;
    }

    private void backToContactListActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_WRITE_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveBtn.setOnClickListener((view) -> saveContact());
                if(contact.getPhoto() != null) {
                    deletePhotoBtn.setVisibility(View.VISIBLE);
                }
            } else {
                saveBtn.setOnClickListener((view) ->
                        ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.WRITE_CONTACTS }, REQUEST_PERMISSION_WRITE_CONTACTS));
                deletePhotoBtn.setVisibility(View.GONE);
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

        if(intent.hasExtra(Contact.ID)) {
            contact.setId(intent.getStringExtra(Contact.ID));
        }


        if(intent.hasExtra(Contact.NAME)) {
            contact.setDisplayName(intent.getStringExtra(Contact.NAME));
        }

        if(intent.hasExtra(Contact.MOBILE_PHONE)) {
            contact.getPhones().add(new Phone(
                    intent.getStringExtra(Contact.MOBILE_PHONE_ID),
                    intent.getStringExtra(Contact.MOBILE_PHONE),
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE));
        }

        if(intent.hasExtra(Contact.WORK_PHONE)) {
            contact.getPhones().add(new Phone(
                    intent.getStringExtra(Contact.WORK_PHONE_ID),
                    intent.getStringExtra(Contact.WORK_PHONE),
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK));
        }

        if(intent.hasExtra(Contact.HOME_PHONE)) {
            contact.getPhones().add(new Phone(
                    intent.getStringExtra(Contact.HOME_PHONE_ID),
                    intent.getStringExtra(Contact.HOME_PHONE),
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME));
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
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    deletePhotoBtn.setVisibility(View.VISIBLE);
                } else {
                    deletePhotoBtn.setVisibility(View.GONE);
                }
            } else {
                deletePhotoBtn.setVisibility(View.GONE);
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