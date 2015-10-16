package org.openhds.mobile.utilities;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Dumping grounds for miscellaneous sync-related functions.
 */
public class SyncUtils {

    private static final String TAG = SyncUtils.class.getName();

    public static String hashFilename(String entityName) {
        return String.format("%s.etag", entityName);
    }

    public static String entityFilename(String entityName) {
        return String.format("%s.xml", entityName);
    }

    public static String loadHash(File hashFile) {
        String contentHash = null;
        if (hashFile.exists() && hashFile.canRead()) {
            try {
                InputStream in = new FileInputStream(hashFile);
                BufferedReader buf = new BufferedReader(new InputStreamReader(in));
                try {
                    contentHash = buf.readLine();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.w(TAG, "failed to close hash file", e);
                    }
                }
            } catch (FileNotFoundException e) {
                Log.w(TAG, "hash file not found", e);
            } catch (IOException e) {
                Log.w(TAG, "failed to read hash file", e);
            }
        }
        return contentHash;
    }

    public static void storeHash(File hashFile, String hash) {
        if (!hashFile.exists() || (hashFile.exists() && hashFile.canWrite())) {
            try {
                OutputStream out = new FileOutputStream(hashFile);
                try {
                    PrintWriter writer = new PrintWriter(out);
                    writer.println(hash == null? "": hash);
                    writer.flush();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.w(TAG, "failed to close hash file", e);
                    }
                }
            } catch (FileNotFoundException e) {
                Log.w(TAG, "hash file not found", e);
            } catch (IOException e) {
                Log.w(TAG, "failed to read hash file", e);
            }
        }
    }

    public static void streamToFile(InputStream in, File f) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        try {
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) >= 0) {
                out.write(buf, 0, read);
            }
        } finally {
            out.close();
        }
    }
}