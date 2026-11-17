package org.enigma.android;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.KeyEvent;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class EnigmaActivity extends SDLActivity {
    private String dataPath;
    private String preferencePath;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!SDLActivity.mBrokenLibraries &&
                event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            // SDL normally forwards Android BACK as its own key, which Enigma
            // does not interpret as Escape. Translate it before SDL sees it.
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE);
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE);
                return true;
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (SDLActivity.mBrokenLibraries) {
            super.onBackPressed();
            return;
        }

        // Fallback for Android back gestures that bypass dispatchKeyEvent().
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE);
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE);
    }

    @Override
    public void setOrientationBis(int w, int h, boolean resizable, String hint) {
        // SDL otherwise derives the orientation from its initial window size,
        // which can be portrait even though this app is landscape-only.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            installGameData(base);
        } catch (IOException error) {
            throw new IllegalStateException("Could not install offline Enigma data", error);
        }
    }

    @Override
    protected String[] getArguments() {
        return new String[] {
            "--data", dataPath,
            "--pref", preferencePath,
            "--robinson",
            "--log",
            "--redirect"
        };
    }

    private void installGameData(Context context) throws IOException {
        File files = context.getFilesDir();
        File destination = new File(files, "enigma-data");
        File marker = new File(destination, ".install-complete");

        dataPath = destination.getAbsolutePath();
        File[] mediaDirectories = context.getExternalMediaDirs();
        File mediaDirectory = null;
        if (mediaDirectories != null) {
            for (File candidate : mediaDirectories) {
                if (candidate != null) {
                    mediaDirectory = new File(candidate, "Enigma");
                    break;
                }
            }
        }
        if (mediaDirectory == null || (!mediaDirectory.mkdirs() && !mediaDirectory.isDirectory())) {
            throw new IOException("Cannot create shared Android/media/Enigma directory");
        }
        preferencePath = mediaDirectory.getAbsolutePath();

        if (marker.isFile()) {
            return;
        }

        File temporary = new File(files, "enigma-data.tmp");
        deleteTree(temporary);
        if (!temporary.mkdirs() && !temporary.isDirectory()) {
            throw new IOException("Cannot create " + temporary);
        }

        String root = temporary.getCanonicalPath() + File.separator;
        byte[] buffer = new byte[32768];

        try (InputStream asset = context.getAssets().open("enigma-data.zip");
             ZipInputStream zip = new ZipInputStream(asset)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File output = new File(temporary, entry.getName());
                String canonical = output.getCanonicalPath();
                if (!canonical.startsWith(root)) {
                    throw new IOException("Unsafe path in game data archive: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    if (!output.mkdirs() && !output.isDirectory()) {
                        throw new IOException("Cannot create directory " + output);
                    }
                } else {
                    File parent = output.getParentFile();
                    if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                        throw new IOException("Cannot create directory " + parent);
                    }
                    try (FileOutputStream stream = new FileOutputStream(output)) {
                        int count;
                        while ((count = zip.read(buffer)) != -1) {
                            stream.write(buffer, 0, count);
                        }
                    }
                }
                zip.closeEntry();
            }
        }

        File complete = new File(temporary, ".install-complete");
        if (!complete.createNewFile()) {
            throw new IOException("Cannot mark game data installation complete");
        }

        deleteTree(destination);
        if (!temporary.renameTo(destination)) {
            throw new IOException("Cannot move installed game data into place");
        }
    }

    private static void deleteTree(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        if (!file.delete()) {
            throw new IOException("Cannot delete " + file);
        }
    }
}
