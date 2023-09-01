package com.savita.contactbook.controllers;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.ContactsContract;

import com.savita.contactbook.models.Contact;
import com.savita.contactbook.models.Phone;

import android.database.Cursor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ContactController {
    public static List<Contact> getContacts(ContentResolver resolver) {
        List<Contact> contacts = new ArrayList<>();

        String[] projection = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        };

        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);

        if(cursor.moveToFirst()) {
            do {
                Contact contact = getContact(resolver, cursor);
                if(contact != null) {
                    contacts.add(contact);
                }
            } while(cursor.moveToNext());
        }

        cursor.close();

        return contacts;
    }

    private static String getPhoto(ContentResolver resolver, String contactId) {
        String photo = null;
        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
        };

        String selection = ContactsContract.CommonDataKinds.Photo.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[] { contactId };

        Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);

        if(cursor.moveToFirst()) {
            int columnIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI);
            if(columnIdx >= 0) {
                photo = cursor.getString(columnIdx);
            }
        }

        cursor.close();

        return photo;
    }

    private static Contact getContact(ContentResolver resolver, Cursor cursor) {
        Contact contact = new Contact();
        contact.setId(getString(cursor, ContactsContract.Contacts._ID));
        contact.setDisplayName(getString(cursor, ContactsContract.Contacts.DISPLAY_NAME));

        List<Phone> phones = getPhones(resolver, contact.getId());
        contact.getPhones().addAll(phones);

        contact.setEmail(getEmail(resolver, contact.getId()));
        contact.setPhoto(getPhoto(resolver, contact.getId()));

        return contact;
    }

    private static String getEmail(ContentResolver resolver, String contactId) {
        String email = "";

        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Email.ADDRESS
        };

        String selection = ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[] { contactId };
        Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection, selection, selectionArgs, null);

        if(cursor.moveToFirst()) {
            email = getString(cursor, ContactsContract.CommonDataKinds.Email.ADDRESS);
        }

        cursor.close();

        return email;
    }


    private static List<Phone> getPhones(ContentResolver resolver, String contactId) {
        List<Phone> phones = new ArrayList<>();

        String[] projection = new String[] {
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        };

        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[] { contactId };

        Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, selectionArgs, null);

        if(cursor.moveToFirst()) {
            do {
                Phone phone = new Phone(getString(cursor, ContactsContract.CommonDataKinds.Phone.NUMBER), getInt(cursor, ContactsContract.CommonDataKinds.Phone.TYPE));
                phones.add(phone);
            } while(cursor.moveToNext());
        }

        cursor.close();

        return phones;
    }

    private static String getString(Cursor cursor, String column) {
        int columnIdx = cursor.getColumnIndex(column);
        if(columnIdx >= 0) {
            return cursor.getString(columnIdx);
        }
        return "";
    }

    private static int getInt(Cursor cursor, String column) {
        int columnIdx = cursor.getColumnIndex(column);
        if(columnIdx >= 0) {
            return cursor.getInt(columnIdx);
        }
        return -1;
    }

    public static void create(ContentResolver resolver, Contact contact) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        addAccountInsert(ops, null, null);
        addContactNameInsert(ops, contact);
        contact.getPhones().forEach(phone -> addContactPhoneInsert(ops, phone));
        addContactEmailInsert(ops, contact);
        addContactPhotoInsert(ops, contact, resolver);

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addAccountInsert(List<ContentProviderOperation> ops, Object accountType, Object accountName) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .build());
    }

    private static void addContactNameInsert(List<ContentProviderOperation> ops, Contact contact) {
        if (contact.getDisplayName() != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        contact.getDisplayName())
                .build());
        }
    }

    private static void addContactPhoneInsert(List<ContentProviderOperation> ops, Phone phone) {
        if(phone != null && phone.getNumber() != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getNumber())
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.getType())
                .build());
        }
    }

    private static void addContactEmailInsert(List<ContentProviderOperation> ops, Contact contact) {
        if (contact.getEmail() != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(
                        ContactsContract.CommonDataKinds.Email.ADDRESS,
                        contact.getEmail())
                .build());
        }
    }

    private static void addContactPhotoInsert(List<ContentProviderOperation> ops, Contact contact, ContentResolver resolver) {
        if (contact.getPhoto() != null) {
            try {
                InputStream iStream = resolver.openInputStream(Uri.parse(contact.getPhoto()));

                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                int length = 0;

                while ((length = iStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, length);
                }

                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(
                                ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(
                                ContactsContract.CommonDataKinds.Photo.PHOTO,
                                byteBuffer.toByteArray()).build());
            } catch(Exception ex) {}
        }
    }
}
