package com.savita.contactbook.controllers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.savita.contactbook.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Objects;

public class MediaController {
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, projection, null, null, null);

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static String getAbsolutePath(String filename) {
        File f = new File(Environment.getExternalStorageDirectory().toString());
        for (File temp : f.listFiles()) {
            if (temp.getName().equals(filename)) {
                f = temp;
                break;
            }
        }
        return f.getAbsolutePath();
    }

    public static byte[] getByteArray(Context context, String path) {
        File file = new File(path);

        ContentResolver resolver = context.getContentResolver();

        try (InputStream  iStream = resolver.openInputStream(FileProvider.getUriForFile(Objects.requireNonNull(context),
        BuildConfig.APPLICATION_ID + ".provider", file))) {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int length = 0;

            while ((length = iStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, length);
            }

            return byteBuffer.toByteArray();

        } catch (Exception e) {
            return null;
        }
    }
}
