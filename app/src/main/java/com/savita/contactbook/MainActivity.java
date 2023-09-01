package com.savita.contactbook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.savita.contactbook.adapters.ContactAdapter;
import com.savita.contactbook.controllers.ContactController;
import com.savita.contactbook.models.Contact;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<Contact> contacts;
    private ListView contactsListView;
    private TextView missedPermissions;
    private ImageButton addBtn;
    private ContactAdapter adapter;
    private final int REQUEST_PERMISSION_READ_CONTACTS = 1;
    private final int REQUEST_PERMISSION_WRITE_CONTACTS = 2;

    private final String LOG_TAG = "contacts_list_log";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contacts = new ArrayList<>();

        contactsListView = findViewById(R.id.contacts_list_view);
        missedPermissions = findViewById(R.id.contacts_missed_permissions);
        addBtn = findViewById(R.id.add_contact_btn);

        checkPermissions();
    }

    @Override
    protected void onResume() {
        refresh();
        super.onResume();
    }

    private void checkPermissions() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Requested for permission READ_CONTACTS");
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_CONTACTS }, REQUEST_PERMISSION_READ_CONTACTS);
        } else {
            Log.d(LOG_TAG, "Permission READ_CONTACTS is already set");
            if(adapter == null) {
                adapter = new ContactAdapter(this, R.layout.contact_item_view, contacts);
                contactsListView.setAdapter(adapter);
                adapter.setOnDataChanged(() -> refresh());
            }
            setContacts();
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Requested for permission WRITE_CONTACTS");
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_CONTACTS }, REQUEST_PERMISSION_WRITE_CONTACTS);
        } else {
            Log.d(LOG_TAG, "Permission WRITE_CONTACTS is already set");
            if(adapter != null) {
                adapter.setEditable(true);
            }
            addBtn.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void refresh() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Refresh contacts list");
            if(adapter == null) {
                adapter = new ContactAdapter(this, R.layout.contact_item_view, contacts);
                contactsListView.setAdapter(adapter);
                adapter.setOnDataChanged(() -> refresh());
            }
            setContacts();
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            if(adapter != null) {
                adapter.setEditable(true);
            }
            addBtn.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Permission READ_CONTACTS is granted");
                contactsListView.setVisibility(View.VISIBLE);
                missedPermissions.setVisibility(View.GONE);
                setContacts();
            } else {
                Log.d(LOG_TAG, "Permission READ_CONTACTS is denied");
                contactsListView.setVisibility(View.GONE);
                missedPermissions.setVisibility(View.VISIBLE);
            }
        }

        if (requestCode == REQUEST_PERMISSION_WRITE_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Permission WRITE_CONTACTS is granted");
                if (adapter != null) {
                    adapter.setEditable(true);
                    addBtn.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            } else {
                Log.d(LOG_TAG, "Permission WRITE_CONTACTS is denied");
                if (adapter != null) {
                    adapter.setEditable(false);
                }
                addBtn.setVisibility(View.GONE);
            }
        }
    }

    private void setContacts() {
        contacts.clear();
        contacts.addAll(ContactController.getContacts(getContentResolver()));
        try {
            Log.d(LOG_TAG, "Contacts count : " + contactsListView.getAdapter().getItem(0));
        } catch(Exception ex) {}
        adapter.notifyDataSetChanged();
    }
}