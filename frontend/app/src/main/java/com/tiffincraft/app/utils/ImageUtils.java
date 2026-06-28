package com.tiffincraft.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ImageUtils {

    /**
     * Compress image from Uri to a temporary File
     * @param context Application context
     * @param imageUri Uri of the image to compress
     * @return Compressed File or null if error
     */
    public static File compressImage(Context context, Uri imageUri) {
        try {
            // Open input stream from Uri
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            // Decode bitmap
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                return null;
            }

            // Resize if too large (max 800x800)
            bitmap = resizeBitmap(bitmap, 800, 800);

            // Compress to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

            // Write to temp file
            File tempFile = File.createTempFile("profile_", ".jpg", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();

            // Clean up
            baos.close();
            bitmap.recycle();

            return tempFile;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Resize bitmap maintaining aspect ratio
     * @param bitmap Original bitmap
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Resized bitmap
     */
    private static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // If already smaller, return as is
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        // Calculate ratio
        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);

        // Calculate new dimensions
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Convert File to MultipartBody.Part for Retrofit upload
     * @param partName Form field name
     * @param file File to upload
     * @return MultipartBody.Part ready for upload
     */
    public static MultipartBody.Part prepareFilePart(String partName, File file) {
        RequestBody requestFile = RequestBody.create(
                MediaType.parse("image/jpeg"),
                file
        );
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    /**
     * Get real file path from Uri (for older Android versions)
     * @param context Application context
     * @param uri Image Uri
     * @return File path as String
     */
    public static String getRealPathFromUri(Context context, Uri uri) {
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            android.database.Cursor cursor = context.getContentResolver().query(
                    uri, projection, null, null, null
            );

            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(columnIndex);
                cursor.close();
                return path;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri.getPath();
    }

    /**
     * Delete file safely
     * @param file File to delete
     */
    public static void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}
