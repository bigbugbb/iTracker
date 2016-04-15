package com.localytics.android.itracker.util;

import android.location.Location;

import com.localytics.android.itracker.Config;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static com.localytics.android.itracker.util.LogUtils.LOGE;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Utilities and constants related to files
 */
public class DataFileUtils {
    private static final String TAG = makeLogTag(DataFileUtils.class);

    private static final int INVALID_DATA = -1;
    private static final int SUMMARY_COUNT = 24 * 60 * Config.MONITORING_DURATION_IN_SECONDS;

    public static void zip(String sourceFile, String zipFile) {
        try {
            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(zipFile));
            FileInputStream in = new FileInputStream(sourceFile);

            int len;
            byte[] buffer = new byte[4096];
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            in.close();

            out.finish();
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

    public static int[] loadSummaryData(final String dataDirPath) {
        int[] data = new int[SUMMARY_COUNT];
        Arrays.fill(data, INVALID_DATA);
        Calendar calendar = Calendar.getInstance();
        Collection<File> files = FileUtils.listFiles(new File(dataDirPath), new SuffixFileFilter(".summary.csv"), TrueFileFilter.TRUE);
        for (File file : files) {
            CSVReader reader = null;
            try {
                reader = new CSVReader(new FileReader(file), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, 1);
                List<String[]> lines = reader.readAll();
                for (String[] line : lines) {
                    calendar.setTimeInMillis(Long.parseLong(line[0]));
                    int offset = (calendar.get(Calendar.HOUR_OF_DAY) * 60 +
                            calendar.get(Calendar.MINUTE)) * Config.MONITORING_DURATION_IN_SECONDS +
                            calendar.get(Calendar.SECOND);
                    data[offset] = Integer.parseInt(line[1]);
                }
            } catch (IOException e) {
                LOGE(TAG, "load summary data - " + e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return data;
    }

    public static List<Location> loadLocationData(final String dataDirPath) {
        List<Location> locations = new ArrayList<>();
        Collection<File> files = FileUtils.listFiles(new File(dataDirPath), new PrefixFileFilter("location."), TrueFileFilter.TRUE);
        for (File file : files) {
            CSVReader reader = null;
            try {
                reader = new CSVReader(new FileReader(file), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, 1);
                List<String[]> lines = reader.readAll();
                for (String[] line : lines) {
                    Location location = new Location("iTracker");
                    location.setTime(Long.parseLong(line[0]));
                    location.setLatitude(Double.parseDouble(line[1]));
                    location.setLongitude(Double.parseDouble(line[2]));
                    location.setAltitude(Double.parseDouble(line[3]));
                    location.setAccuracy(Float.parseFloat(line[4]));
                    location.setBearing(Float.parseFloat(line[5]));
                    location.setSpeed(Float.parseFloat(line[6]));
                    locations.add(location);
                }
            } catch (IOException e) {
                LOGE(TAG, "load location data - " + e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return locations;
    }

    public static List<TrackerDetectedActivity> loadActivityData(final String dataDirPath) {
        List<TrackerDetectedActivity> activities = new ArrayList<>();
        Collection<File> files = FileUtils.listFiles(new File(dataDirPath), new PrefixFileFilter("activity."), TrueFileFilter.TRUE);
        for (File file : files) {
            CSVReader reader = null;
            try {
                reader = new CSVReader(new FileReader(file), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, 1);
                List<String[]> lines = reader.readAll();
                for (String[] line : lines) {
                    TrackerDetectedActivity activity = new TrackerDetectedActivity(Long.parseLong(line[0]), line[1], Integer.parseInt(line[2]));
                    activities.add(activity);
                }
            } catch (IOException e) {
                LOGE(TAG, "load activity data - " + e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return activities;
    }

    public static class TrackerDetectedActivity {
        public long mTimestamp;
        public String mActivityType;
        public int mConfidence;

        public TrackerDetectedActivity(long timestamp, String activityType, int confidence) {
            mTimestamp = timestamp;
            mActivityType = activityType;
            mConfidence = confidence;
        }

        public long getTimestamp() {
            return mTimestamp;
        }

        public String getType() {
            return mActivityType;
        }

        public int getConfidence() {
            return mConfidence;
        }
    }
}