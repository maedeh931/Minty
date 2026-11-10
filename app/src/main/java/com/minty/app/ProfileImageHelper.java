package com.minty.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Helper utility for profile photo operations:
 * - Downsamples picked images to max 512px
 * - Saves to private internal storage (getFilesDir()/profile_<uid>.jpg)
 * - Persists path and buddy info in SharedPreferences
 * - Renders images circularly on ImageViews using RoundedBitmapDrawable
 */
public class ProfileImageHelper {

    private static final String TAG = "ProfileImageHelper";

    public static final String PREFS_NAME = "minty_profile_prefs";
    public static final String KEY_PROFILE_PHOTO_PATH = "profile_photo_path";
    public static final String KEY_BUDDY_NAME = "buddy_name";
    public static final String KEY_BUDDY_PHONE = "buddy_phone";
    public static final int MAX_IMAGE_DIMENSION = 512;

    /**
     * Reads the picked image from ContentResolver, downsamples it to <= 512px,
     * and saves it in app's private storage (getFilesDir()/profile_<uid>.jpg).
     *
     * @param context Application or Activity context
     * @param uri     URI of the chosen image
     * @return Saved file path, or null on error
     */
    public static String saveProfilePhoto(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }

        try {
            // Step 1: Decode bounds only to calculate inSampleSize
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, boundsOptions);
            }

            int origWidth = boundsOptions.outWidth;
            int origHeight = boundsOptions.outHeight;
            int inSampleSize = 1;
            int maxDim = Math.max(origWidth, origHeight);
            if (maxDim > MAX_IMAGE_DIMENSION) {
                inSampleSize = Math.round((float) maxDim / (float) MAX_IMAGE_DIMENSION);
            }

            // Step 2: Decode downsampled bitmap
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = Math.max(1, inSampleSize);
            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, decodeOptions);
            }

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: " + uri);
                return null;
            }

            // Step 3: Save to internal private storage
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (user != null && user.getUid() != null) ? user.getUid() : "default";
            File photoFile = new File(context.getFilesDir(), "profile_" + uid + ".jpg");

            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }

            String savedPath = photoFile.getAbsolutePath();

            // Step 4: Persist file path in SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_PROFILE_PHOTO_PATH, savedPath).apply();

            Log.d(TAG, "Profile photo successfully saved to: " + savedPath);
            return savedPath;
        } catch (Exception e) {
            Log.e(TAG, "Error saving profile photo", e);
            return null;
        }
    }

    /**
     * Loads the saved photo (or default mascot) and applies it circularly to the ImageView.
     *
     * @param context   Application or Activity context
     * @param imageView ImageView to populate
     */
    public static void loadProfilePhotoCircular(Context context, ImageView imageView) {
        if (context == null || imageView == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String path = prefs.getString(KEY_PROFILE_PHOTO_PATH, null);

        Bitmap bitmap = null;
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        }

        // If no custom photo is found, load the mascot as default
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.mascot_minty);
            if (bitmap == null) {
                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.mascot_minty_small);
                if (drawable != null) {
                    int w = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 120;
                    int h = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 120;
                    bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                }
            }
        }

        if (bitmap != null) {
            setCircularBitmap(context.getResources(), imageView, bitmap);
        }
    }

    /**
     * Crops a bitmap to a centered square and sets it on the ImageView as a circular drawable.
     */
    public static void setCircularBitmap(Resources res, ImageView imageView, Bitmap bitmap) {
        if (bitmap == null || imageView == null || res == null) {
            return;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        Bitmap square = Bitmap.createBitmap(bitmap, x, y, size, size);

        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(res, square);
        roundedDrawable.setCircular(true);
        roundedDrawable.setAntiAlias(true);
        imageView.setImageDrawable(roundedDrawable);
    }
}
