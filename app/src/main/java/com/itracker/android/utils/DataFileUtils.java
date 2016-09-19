package com.itracker.android.utils;

import com.itracker.android.Config;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.itracker.android.utils.LogUtils.makeLogTag;

/**
 * Utilities and constants related to files
 */
public class DataFileUtils {
    private static final String TAG = makeLogTag(DataFileUtils.class);

    private static final int INVALID_DATA = -1;
    private static final int SUMMARY_COUNT = 24 * 60 * Config.MONITORING_DURATION_IN_SECONDS;

    public static void zip(String srcFile, String zipFile) {
        byte[] buffer = new byte[16 * 1024];
        try {
            FileInputStream in = new FileInputStream(srcFile);
            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(zipFile));

            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.finish();

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unzip(String zipFile, String dstFile) {
        byte[] buffer = new byte[8 * 1024];
        try {
            GZIPInputStream in = new GZIPInputStream(new FileInputStream(zipFile));
            FileOutputStream out = new FileOutputStream(dstFile);

            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String data, File file) throws IOException {
        writeFile(data.getBytes(Charset.forName("UTF-8")), file);
    }

    public static void writeFile(byte[] data, File file) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, false));
        bos.write(data);
        bos.close();
    }

    public static String readFileAsString(File file) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(bis, bos);
        byte[] contents = bos.toByteArray();
        bis.close();
        bos.close();
        return new String(contents, Charset.forName("UTF-8"));
    }
}