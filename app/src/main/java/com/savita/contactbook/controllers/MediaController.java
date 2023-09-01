package com.savita.contactbook.controllers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;

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

    public static String getMedia(String filename) {
        File f = new File(Environment.getExternalStorageDirectory().toString());
        for (File temp : f.listFiles()) {
            if (temp.getName().equals(filename)) {
                f = temp;
                break;
            }
        }
        try {
//            Bitmap bitmap;
//            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            return f.getAbsolutePath();
//            return BitmapFactory.decodeFile(f.getAbsolutePath(), bitmapOptions);
//            viewImage.setImageBitmap(bitmap);
//            String path = android.os.Environment
//                    .getExternalStorageDirectory()
//                    + File.separator
//                    + "Phoenix" + File.separator + "default";
//            f.delete();
//            OutputStream outFile = null;
//            File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");
//            try {
//                outFile = new FileOutputStream(file);
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
//                outFile.flush();
//                outFile.close();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
