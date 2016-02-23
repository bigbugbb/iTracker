package com.localytics.android.itracker.monitor.writer;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 10/10/15.
 */
public class SensorDataWriter {
    private static final String TAG = makeLogTag(SensorDataWriter.class);

    private String[] mHeader;

    public SensorDataWriter() {
    }

    public SensorDataWriter(String[] header) {
        setHeader(header);
    }

    public void setHeader(String[] header) {
        mHeader = header;
    }

    public void write(String dirPath, String fileName, String[] line) {
        if (line == null || line.length == 0) {
            return;
        }

        CSVWriter writer = null;
        try {
            writer = getCSVWriter(dirPath, fileName);
            writer.writeNext(line);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void write(String dirPath, String fileName, List<String[]> rows) {
        if (rows == null || rows.size() == 0) {
            return;
        }

        CSVWriter writer = null;
        try {
            writer = getCSVWriter(dirPath, fileName);
            for (String[] line : rows) {
                writer.writeNext(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private CSVWriter getCSVWriter(String dirPath, String fileName) throws IOException {
        File dir = new File(dirPath);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException(String.format("Could not create directory for %s", dirPath));
        }

        CSVWriter writer;
        File fileToWrite = new File(dirPath, fileName);
        if (!fileToWrite.exists()) {
            writer = new CSVWriter(new FileWriter(fileToWrite), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
            writer.writeNext(mHeader);
        } else {
            writer = new CSVWriter(new FileWriter(fileToWrite, true), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        }
        return writer;
    }

}