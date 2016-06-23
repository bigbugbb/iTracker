package com.localytics.android.itracker.util;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class YouTubeExtractor {
    private static final int YOUTUBE_VIDEO_QUALITY_SMALL_240 = 36;
    private static final int YOUTUBE_VIDEO_QUALITY_MEDIUM_360 = 18;
    private static final int YOUTUBE_VIDEO_QUALITY_HD_720 = 22;
    private static final int YOUTUBE_VIDEO_QUALITY_HD_1080 = 37;
    private final String mVideoIdentifier;
    private HttpsURLConnection mConnection;
    private boolean mCancelled;

    public YouTubeExtractor(String videoIdentifier) {
        this.mVideoIdentifier = videoIdentifier;
    }

    public void extractAsync(final YouTubeExtractor.Callback listener) {
        Handler youtubeExtractorHandler;
        final HandlerThread youtubeExtractorThread = new HandlerThread("YouTubeExtractorThread", 10);
        youtubeExtractorThread.start();
        youtubeExtractorHandler = new Handler(youtubeExtractorThread.getLooper());
        youtubeExtractorHandler.post(new Runnable() {
            public void run() {
                extract(listener);
            }
        });
    }

    public YouTubeExtractor.Result extract(final YouTubeExtractor.Callback listener) {
        final Handler listenerHandler = new Handler(Looper.getMainLooper());
        try {
            String elField = "embedded";
            final String language = Locale.getDefault().getLanguage();
            final String link = String.format("https://www.youtube.com/get_video_info?video_id=%s&el=%s&ps=default&eurl=&gl=US&hl=%s", new Object[]{mVideoIdentifier, elField, language});

            mConnection = (HttpsURLConnection) (new URL(link)).openConnection();
            mConnection.setRequestProperty("Accept-Language", language);
            BufferedReader reader = new BufferedReader(new InputStreamReader(mConnection.getInputStream()));
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null && !mCancelled) {
                builder.append(line);
            }

            reader.close();
            if (!mCancelled) {
                final YouTubeExtractor.Result result = getYouTubeResult(builder.toString());
                listenerHandler.post(new Runnable() {
                    public void run() {
                        if (!mCancelled && listener != null) {
                            listener.onSuccess(result);
                        }

                    }
                });
                return result;
            }
        } catch (final Exception var8) {
            listenerHandler.post(new Runnable() {
                public void run() {
                    if (!mCancelled && listener != null) {
                        listener.onFailure(var8);
                    }

                }
            });
        } finally {
            if (mConnection != null) {
                mConnection.disconnect();
            }
        }

        return null;
    }

    public void cancel() {
        this.mCancelled = true;
    }

    private static HashMap<String, String> getQueryMap(String queryString, String charsetName) throws UnsupportedEncodingException {
        HashMap map = new HashMap();
        String[] fields = queryString.split("&");
        String[] arr$ = fields;
        int len$ = fields.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            String field = arr$[i$];
            String[] pair = field.split("=");
            if (pair.length == 2) {
                String key = pair[0];
                String value = URLDecoder.decode(pair[1], charsetName).replace('+', ' ');
                map.put(key, value);
            }
        }

        return map;
    }

    private YouTubeExtractor.Result getYouTubeResult(String html) throws UnsupportedEncodingException, YouTubeExtractor.YouTubeExtractorException {
        HashMap video = getQueryMap(html, "UTF-8");
        if (video.containsKey("url_encoded_fmt_stream_map")) {
            ArrayList streamQueries = new ArrayList(Arrays.asList(((String) video.get("url_encoded_fmt_stream_map")).split(",")));
            String adaptiveFmts = (String) video.get("adaptive_fmts");
            String[] split = adaptiveFmts.split(",");
            streamQueries.addAll(Arrays.asList(split));
            SparseArray streamLinks = new SparseArray();
            Iterator sd240VideoUri = streamQueries.iterator();

            while (sd240VideoUri.hasNext()) {
                String sd360VideoUri = (String) sd240VideoUri.next();
                HashMap hd720VideoUri = getQueryMap(sd360VideoUri, "UTF-8");
                String hd1080VideoUri = ((String) hd720VideoUri.get("type")).split(";")[0];
                String mediumThumbUri = (String) hd720VideoUri.get("url");
                if (mediumThumbUri != null && MimeTypeMap.getSingleton().hasMimeType(hd1080VideoUri)) {
                    String highThumbUri = (String) hd720VideoUri.get("sig");
                    if (highThumbUri != null) {
                        mediumThumbUri = mediumThumbUri + "&signature=" + highThumbUri;
                    }

                    if (getQueryMap(mediumThumbUri, "UTF-8").containsKey("signature")) {
                        streamLinks.put(Integer.parseInt((String) hd720VideoUri.get("itag")), mediumThumbUri);
                    }
                }
            }

            Uri sd240VideoUri1 = this.extractVideoUri(36, streamLinks);
            Uri sd360VideoUri1 = this.extractVideoUri(18, streamLinks);
            Uri hd720VideoUri1 = this.extractVideoUri(22, streamLinks);
            Uri hd1080VideoUri1 = this.extractVideoUri(37, streamLinks);
            Uri mediumThumbUri1 = video.containsKey("iurlmq") ? Uri.parse((String) video.get("iurlmq")) : null;
            Uri highThumbUri1 = video.containsKey("iurlhq") ? Uri.parse((String) video.get("iurlhq")) : null;
            Uri defaultThumbUri = video.containsKey("iurl") ? Uri.parse((String) video.get("iurl")) : null;
            Uri standardThumbUri = video.containsKey("iurlsd") ? Uri.parse((String) video.get("iurlsd")) : null;
            return new YouTubeExtractor.Result(sd240VideoUri1, sd360VideoUri1, hd720VideoUri1, hd1080VideoUri1, mediumThumbUri1, highThumbUri1, defaultThumbUri, standardThumbUri);
        } else {
            throw new YouTubeExtractor.YouTubeExtractorException("Status: " + (String) video.get("status") + "\nReason: " + (String) video.get("reason") + "\nError code: " + (String) video.get("errorcode"));
        }
    }

    @Nullable
    private Uri extractVideoUri(int quality, SparseArray<String> streamLinks) {
        Uri videoUri = null;
        if (streamLinks.get(quality, null) != null) {
            String streamLink = streamLinks.get(quality);
            videoUri = Uri.parse(streamLink);
        }

        return videoUri;
    }

    public interface Callback {
        void onSuccess(YouTubeExtractor.Result var1);

        void onFailure(Throwable var1);
    }

    public static final class YouTubeExtractorException extends Exception {
        public YouTubeExtractorException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class Result {
        private final Uri mSd240VideoUri;
        private final Uri mSd360VideoUri;
        private final Uri mHd720VideoUri;
        private final Uri mHd1080VideoUri;
        private final Uri mMediumThumbUri;
        private final Uri mHighThumbUri;
        private final Uri mDefaultThumbUri;
        private final Uri mStandardThumbUri;

        private Result(Uri sd240VideoUri, Uri sd360VideoUri, Uri hd720VideoUri, Uri hd1080VideoUri, Uri mediumThumbUri, Uri highThumbUri, Uri defaultThumbUri, Uri standardThumbUri) {
            this.mSd240VideoUri = sd240VideoUri;
            this.mSd360VideoUri = sd360VideoUri;
            this.mHd720VideoUri = hd720VideoUri;
            this.mHd1080VideoUri = hd1080VideoUri;
            this.mMediumThumbUri = mediumThumbUri;
            this.mHighThumbUri = highThumbUri;
            this.mDefaultThumbUri = defaultThumbUri;
            this.mStandardThumbUri = standardThumbUri;
        }

        @Nullable
        public Uri getSd240VideoUri() {
            return this.mSd240VideoUri;
        }

        @Nullable
        public Uri getSd360VideoUri() {
            return this.mSd360VideoUri;
        }

        @Nullable
        public Uri getHd720VideoUri() {
            return this.mHd720VideoUri;
        }

        @Nullable
        public Uri getHd1080VideoUri() {
            return this.mHd1080VideoUri;
        }

        @Nullable
        public Uri getBestAvaiableQualityVideoUri() {
            Uri uri = this.getHd1080VideoUri();
            if (uri != null) {
                return uri;
            } else {
                uri = this.getHd720VideoUri();
                if (uri != null) {
                    return uri;
                } else {
                    uri = this.getSd360VideoUri();
                    if (uri != null) {
                        return uri;
                    } else {
                        uri = this.getSd240VideoUri();
                        return uri != null ? uri : null;
                    }
                }
            }
        }

        @Nullable
        public Uri getWorstAvaiableQualityVideoUri() {
            Uri uri = this.getSd240VideoUri();
            if (uri != null) {
                return uri;
            } else {
                uri = this.getSd360VideoUri();
                if (uri != null) {
                    return uri;
                } else {
                    uri = this.getHd720VideoUri();
                    if (uri != null) {
                        return uri;
                    } else {
                        uri = this.getHd1080VideoUri();
                        return uri != null ? uri : null;
                    }
                }
            }
        }

        @Nullable
        public Uri getMediumThumbUri() {
            return this.mMediumThumbUri;
        }

        @Nullable
        public Uri getHighThumbUri() {
            return this.mHighThumbUri;
        }

        @Nullable
        public Uri getDefaultThumbUri() {
            return this.mDefaultThumbUri;
        }

        @Nullable
        public Uri getStandardThumbUri() {
            return this.mStandardThumbUri;
        }
    }
}