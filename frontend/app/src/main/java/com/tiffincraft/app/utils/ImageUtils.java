package com.tiffincraft.app.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ImageUtils {

    /**
     * Compress image from Uri to a temporary File.
     *
     * Decoding goes through Glide rather than BitmapFactory. Glide auto-corrects
     * standard EXIF rotation tags, which fixes most camera photos. It cannot
     * help when the source image has no usable orientation metadata at all
     * (e.g. downloaded/forwarded images, screenshots re-saved by another app) —
     * in that case the pixel data itself is stored sideways and no decoder can
     * know to rotate it. That case is handled separately: see
     * {@link #rotateAndCompress(Context, Uri, int)} for user-driven correction.
     *
     * Must be called off the main thread — this blocks on Glide's synchronous
     * FutureTarget (both current callers already run this inside a background
     * Thread).
     *
     * @param context Application context
     * @param imageUri Uri of the image to compress
     * @return Compressed File or null if error
     */
    public static File compressImage(Context context, Uri imageUri) {
        return rotateAndCompress(context, imageUri, 0);
    }

    /**
     * Same as {@link #compressImage(Context, Uri)}, but applies an additional
     * user-chosen rotation on top of whatever Glide's own EXIF handling
     * produces. Used by the profile screens' manual "Rotate" control, which
     * exists specifically for source images with no orientation metadata to
     * auto-detect from.
     *
     * @param extraDegrees clockwise rotation to apply (0/90/180/270)
     */
    public static File rotateAndCompress(Context context, Uri imageUri, int extraDegrees) {
        try {
            FutureTarget<Bitmap> futureTarget = Glide.with(context)
                    .asBitmap()
                    .load(imageUri)
                    .apply(RequestOptions.fitCenterTransform())
                    .submit(800, 800);

            Bitmap bitmap = futureTarget.get();
            return finishRotateAndCompress(context, bitmap, extraDegrees);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches an already-uploaded remote photo, rotates it, and re-saves it as
     * a local temp file ready for re-upload. Lets a user fix a profile photo
     * that saved sideways without having to re-pick it from their gallery.
     *
     * @param imageUrl full URL of the currently saved profile photo
     * @param degrees clockwise rotation to apply (0/90/180/270)
     */
    public static File rotateRemoteImage(Context context, String imageUrl, int degrees) {
        try {
            FutureTarget<Bitmap> futureTarget = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .apply(RequestOptions.fitCenterTransform())
                    .submit(800, 800);

            Bitmap bitmap = futureTarget.get();
            return finishRotateAndCompress(context, bitmap, degrees);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static File finishRotateAndCompress(Context context, Bitmap bitmap, int degrees) {
        if (bitmap == null) {
            return null;
        }
        try {
            Bitmap output = bitmap;
            int normalizedDegrees = ((degrees % 360) + 360) % 360;
            if (normalizedDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(normalizedDegrees);
                output = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            output.compress(Bitmap.CompressFormat.JPEG, 80, baos);

            File tempFile = File.createTempFile("profile_", ".jpg", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
            baos.close();

            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
     * Saves a bitmap into the device's Pictures/TiffinCraft gallery folder via MediaStore.
     * On API 29+ this needs no permission (scoped storage); on API 26-28 the caller must
     * hold WRITE_EXTERNAL_STORAGE first (see AndroidManifest's maxSdkVersion="28" grant).
     *
     * @return true if the image was written successfully
     */
    public static boolean saveBitmapToGallery(Context context, Bitmap bitmap, String displayName) {
        if (bitmap == null) return false;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TiffinCraft");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri itemUri = null;
        try {
            itemUri = context.getContentResolver().insert(collection, values);
            if (itemUri == null) return false;

            try (java.io.OutputStream out = context.getContentResolver().openOutputStream(itemUri)) {
                if (out == null) return false;
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(itemUri, values, null, null);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            if (itemUri != null) {
                context.getContentResolver().delete(itemUri, null, null);
            }
            return false;
        }
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
