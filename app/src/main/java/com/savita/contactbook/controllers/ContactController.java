package com.savita.contactbook.controllers;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;

import com.savita.contactbook.BuildConfig;
import com.savita.contactbook.models.Contact;
import com.savita.contactbook.models.Phone;

import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ContactController {
    private final static String LOG_TAG = "contact_controller";
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
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        };

        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[] { contactId };

        Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, selectionArgs, null);

        if(cursor.moveToFirst()) {
            do {
                Phone phone = new Phone(
                        getString(cursor, ContactsContract.CommonDataKinds.Phone._ID),
                        getString(cursor, ContactsContract.CommonDataKinds.Phone.NUMBER),
                        getInt(cursor, ContactsContract.CommonDataKinds.Phone.TYPE));
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

    public static boolean create(ContentResolver resolver, Contact contact, Context context) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        addAccountInsert(ops, null, null);
        addContactNameInsert(ops, contact);
        contact.getPhones().forEach(phone -> addContactPhoneInsert(ops, phone));
        addContactEmailInsert(ops, contact);
        addContactPhotoInsert(ops, contact, resolver, context);

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(LOG_TAG, e.getMessage());
        }

        return false;
    }

    public static boolean remove(ContentResolver resolver, Contact contact) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        removeEmail(ops, contact);
        removePhones(ops, contact);
        removePhoto(ops, contact);
        removeContact(ops, contact);

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            ops.clear();
        } catch (Exception ex) {
            Log.d(LOG_TAG, ex.getMessage());
            return false;
        }
        return true;
    }

    public static boolean update(Context context, Contact contact) {
        ContentResolver resolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        updateContact(ops, contact);
        updateEmail(ops, context, contact);
        updatePhones(ops, context, contact);
        updatePhoto(ops, context, contact);

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
            ops.clear();
        } catch (Exception ex) {
            Log.d(LOG_TAG, ex.getMessage());
            return false;
        }
        return true;
    }

    private static void updateContact(ArrayList<ContentProviderOperation> ops, Contact contact) {
        String selection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[] { contact.getId(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE };
        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        contact.getDisplayName())
                .build());
    }

    private static void updatePhones(ArrayList<ContentProviderOperation> ops, Context context, Contact contact) {
        List<Phone> oldPhones = getPhones(context.getContentResolver(), contact.getId());
        List<Phone> newPhones = contact.getPhones();

        for(Phone phone : oldPhones) {
            Optional<Phone> optional = newPhones.stream()
                    .filter(ph -> ph.getId() != null)
                    .filter(ph -> ph.getId().equals(phone.getId()))
                    .findFirst();
            if(optional.isPresent()) {
                updatePhone(ops, contact.getId(), phone);
            } else {
                removePhone(ops, phone.getId());
            }
        }

        newPhones.stream()
                .filter(phone -> phone.getId() == null)
                .forEach(phone -> insertPhone(ops, context, contact.getId(), phone));
    }

    private static void updatePhone(ArrayList<ContentProviderOperation> ops, String contactId, Phone phone) {
        if(phone.getNumber() != null) {
            String selection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[]{ contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE };
            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(selection, selectionArgs)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getNumber())
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.getType())
                    .build());
        }
    }

    private static void insertPhone(ArrayList<ContentProviderOperation> ops, Context context, String contactId, Phone phone) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, getRawContactId(context, contactId))
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getNumber())
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.getType())
                .build());
    }

    private static int getRawContactId(Context context, String contactId) {
        String[] projection = new String[] { ContactsContract.RawContacts._ID };
        String selection = ContactsContract.RawContacts.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[] { contactId };

        Cursor cursor = context
                .getContentResolver()
                .query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs , null);

        int rawId = -1;
        if (cursor.moveToFirst()) {
            rawId = getInt(cursor, ContactsContract.RawContacts._ID);
        }

        cursor.close();
        return rawId;
    }

    private static void updateEmail(ArrayList<ContentProviderOperation> ops, Context context, Contact contact) {
        if (contact.getEmail() != null) {
            String selection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[] { contact.getId(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE };

            if(getEmail(context.getContentResolver(), contact.getId()).isEmpty()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, getRawContactId(context, contact.getId()))
                        .withValue(
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                contact.getEmail())
                        .build());
            } else {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(selection, selectionArgs)
                        .withValue(
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                contact.getEmail())
                        .build());
            }

        } else {
            removeEmail(ops, contact);
        }
    }

    private static void updatePhoto(ArrayList<ContentProviderOperation> ops, Context context, Contact contact) {
        if (contact.getPhoto() != null) {
            String selection = ContactsContract.CommonDataKinds.Photo.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[] { contact.getId(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE };

            if(getPhoto(context.getContentResolver(), contact.getId()) == null) {
                try {
                    ops.add(ContentProviderOperation
                            .newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, getRawContactId(context, contact.getId()))
                            .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                            .withValue(ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(
                                    ContactsContract.CommonDataKinds.Photo.PHOTO,
                                    MediaController.getByteArray(context, contact.getPhoto()))
                            .build());
                } catch(Exception ex) {
                    Log.d(LOG_TAG, ex.getMessage());
                }
            } else {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(selection, selectionArgs)
                        .withValue(
                                ContactsContract.CommonDataKinds.Photo.PHOTO,
                                MediaController.getByteArray(context, contact.getPhoto()))
                        .build());
            }

        } else {
            removePhoto(ops, contact);
        }
    }

    private static void removeEmail(List<ContentProviderOperation> ops, Contact contact) {
        String selection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[] { contact.getId(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE };
        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .build());
    }

    private static void removePhoto(List<ContentProviderOperation> ops, Contact contact) {
        String selection = ContactsContract.CommonDataKinds.Photo.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[] { contact.getId(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE };
        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .build());
    }

    private static void removeContact(List<ContentProviderOperation> ops, Contact contact) {
        if (contact != null) {
            String selection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] selectionArgs = new String[] { contact.getId(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE };
            ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection(selection, selectionArgs)
                    .build());
        }
    }

    private static void removePhones(List<ContentProviderOperation> ops, Contact contact) {
        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[] { contact.getId(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE };
        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .build());
    }

    private static void removePhone(List<ContentProviderOperation> ops, String phoneId) {
        String selection = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[] { phoneId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE };
        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, selectionArgs)
                .build());
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

    private static void addContactPhotoInsert(List<ContentProviderOperation> ops, Contact contact, ContentResolver resolver, Context context) {
        if (contact.getPhoto() != null) {
            try {
//                File file = new File(contact.getPhoto());
//
//                InputStream iStream = resolver.openInputStream(FileProvider.getUriForFile(Objects.requireNonNull(context),
//                        BuildConfig.APPLICATION_ID + ".provider", file));
////                InputStream iStream = resolver.openInputStream(Uri.parse(contact.getPhoto()));
//
//                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
//                int bufferSize = 1024;
//                byte[] buffer = new byte[bufferSize];
//
//                int length = 0;
//
//                while ((length = iStream.read(buffer)) != -1) {
//                    byteBuffer.write(buffer, 0, length);
//                }

                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(
                                ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(
                                ContactsContract.CommonDataKinds.Photo.PHOTO,
                                MediaController.getByteArray(context, contact.getPhoto()))
                        .build());
//                                byteBuffer.toByteArray()).build());
            } catch(Exception ex) {
                Log.d(LOG_TAG, ex.getMessage());
            }
        }
    }

}
