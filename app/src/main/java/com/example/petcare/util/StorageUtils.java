package com.example.petcare.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public final class StorageUtils {
    private StorageUtils() {
    }

    public static Uri createCameraImageUri(Context context, String prefix) throws Exception {
        File dir = new File(context.getCacheDir(), "camera");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, prefix + "_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    public static String copyImageToAppStorage(Context context, Uri sourceUri, String prefix) throws Exception {
        File dir = new File(context.getFilesDir(), "images");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outFile = new File(dir, prefix + "_" + System.currentTimeMillis() + ".jpg");

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            BitmapFactory.decodeStream(is, null, bounds);
        }

        int sampleSize = 1;
        double pixels = Math.max(1, bounds.outWidth) * Math.max(1, bounds.outHeight);
        while (pixels / (sampleSize * sampleSize) > 1_000_000d) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = Math.max(1, sampleSize);
        Bitmap bitmap;
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            bitmap = BitmapFactory.decodeStream(is, null, decode);
        }
        if (bitmap == null) {
            throw new IllegalStateException("Unable to decode selected image");
        }

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        }
        bitmap.recycle();
        return Uri.fromFile(outFile).toString();
    }

    public static String copyDocumentToAppStorage(Context context, Uri sourceUri, String subDir, String prefix, long maxBytes) throws Exception {
        long size = getSize(context, sourceUri);
        if (size > 0 && size > maxBytes) {
            throw new IllegalStateException("Selected file is too large");
        }

        File dir = new File(context.getFilesDir(), subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String ext = getExtension(context, sourceUri);
        if (ext.isEmpty()) {
            ext = ".bin";
        }
        File outFile = new File(dir, prefix + "_" + UUID.randomUUID() + ext);

        try (InputStream input = context.getContentResolver().openInputStream(sourceUri);
             OutputStream output = new FileOutputStream(outFile)) {
            if (input == null) {
                throw new IllegalStateException("Could not read selected file");
            }
            byte[] buffer = new byte[8_192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        return Uri.fromFile(outFile).toString();
    }

    public static void persistReadPermission(Context context, Uri uri) {
        try {
            context.getContentResolver().takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }
    }

    public static String getDisplayName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String path = uri.getLastPathSegment();
        return path == null ? "selected_file" : path;
    }

    public static void copyUriToUri(Context context, Uri source, Uri target) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        try (InputStream input = resolver.openInputStream(source);
             OutputStream output = resolver.openOutputStream(target, "w")) {
            if (input == null || output == null) {
                throw new IllegalStateException("Unable to open SAF stream");
            }
            byte[] buffer = new byte[8_192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    public static Uri toOpenableUri(Context context, String uriString) {
        Uri uri = Uri.parse(uriString);
        if ("file".equals(uri.getScheme())) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", new File(uri.getPath()));
        }
        return uri;
    }

    public static String getMimeType(Context context, Uri uri) {
        String type = context.getContentResolver().getType(uri);
        if (type != null) return type;
        String ext = getExtension(context, uri);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        String fromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return fromExt != null ? fromExt : "*/*";
    }

    private static String getExtension(Context context, Uri uri) {
        String mime = context.getContentResolver().getType(uri);
        if (mime != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null && !ext.isEmpty()) {
                return "." + ext.toLowerCase(Locale.ROOT);
            }
        }
        String name = getDisplayName(context, uri);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private static long getSize(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) {
                    return cursor.getLong(idx);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return -1L;
    }
}
