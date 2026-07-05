package com.tiffincraft.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Helper class for image upload operations
 */
public class ImageUploadHelper {

    /**
     * Create MultipartBody.Part from image URI for Cloudinary upload
     *
     * @param context Application context
     * @param imageUri URI of the selected image
     * @param partName Name of the multipart field (e.g., "image", "document")
     * @return MultipartBody.Part ready for upload
     */
    public static MultipartBody.Part createImagePart(Context context, Uri imageUri, String partName) {
        try {
            // Get the actual file path
            File file = getFileFromUri(context, imageUri);
            if (file == null) {
                return null;
            }

            // Get MIME type
            String mimeType = getMimeType(context, imageUri);
            if (mimeType == null) {
                mimeType = "image/*";
            }

            // Create RequestBody
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse(mimeType),
                    file
            );

            // Create MultipartBody.Part
            return MultipartBody.Part.createFormData(
                    partName,
                    file.getName(),
                    requestBody
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create image part with default "image" field name
     */
    public static MultipartBody.Part createImagePart(Context context, Uri imageUri) {
        return createImagePart(context, imageUri, "image");
    }

    /**
     * Create document part with "document" field name
     */
    public static MultipartBody.Part createDocumentPart(Context context, Uri documentUri) {
        return createImagePart(context, documentUri, "document");
    }

    /**
     * Convert URI to File
     */
    private static File getFileFromUri(Context context, Uri uri) {
        try {
            // If it's already a file URI
            if ("file".equals(uri.getScheme())) {
                return new File(uri.getPath());
            }

            // For content:// URIs
            String fileName = getFileName(context, uri);
            if (fileName == null) {
                fileName = "temp_upload_" + System.currentTimeMillis();
            }

            // Create temp file in cache directory
            File tempFile = new File(context.getCacheDir(), fileName);

            // Copy content to temp file
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get file name from URI
     */
    private static String getFileName(Context context, Uri uri) {
        String fileName = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    null,
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }

        return fileName;
    }

    /**
     * Get MIME type from URI
     */
    private static String getMimeType(Context context, Uri uri) {
        String mimeType = null;

        if ("content".equals(uri.getScheme())) {
            mimeType = context.getContentResolver().getType(uri);
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }

        return mimeType;
    }

    /**
     * Validate if file is an image
     */
    public static boolean isImageFile(Context context, Uri uri) {
        String mimeType = getMimeType(context, uri);
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Validate file size (max 5MB for Cloudinary)
     */
    public static boolean isValidFileSize(Context context, Uri uri, long maxSizeInMB) {
        try {
            File file = getFileFromUri(context, uri);
            if (file == null) {
                return false;
            }
            long fileSizeInMB = file.length() / (1024 * 1024);
            return fileSizeInMB <= maxSizeInMB;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clean up temporary files
     */
    public static void cleanupTempFiles(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("temp_upload_")) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
