package de.belu.firestarter.tools;

import android.content.Context;
import android.util.JsonReader;

import org.json.JSONException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Update class for FireStarter
 */
public class UlangoTVUpdater extends Updater {
    /**
     * Update URL where updated versions are found
     */
    private String mUpdateUrl;
    private String mPackageName;
    private String mAppName;

    @Override
    public String getAppName() {
        return mAppName;
    }
    /** Constructor to get Context */
    public UlangoTVUpdater(String app_name, String package_name)
    {
        mPackageName = package_name;
        mAppName = app_name;
        mUpdateUrl = "https://ulango.tv/android/" + package_name + "/releases.json";
    }

    @Override
    public String getPackageName(Context context) {
        return mPackageName;
    }

    @Override
    public Boolean isVersionNewer(String oldVersion, String newVersion) {
        // Use standard check
        return isVersionNewerStandardCheck(oldVersion, newVersion);
    }

    @Override
    protected void updateLatestVersionAndApkDownloadUrl() throws Exception {
        InputStream is = null;

        try {
            URL url = new URL(mUpdateUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
                JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("version")) {
                            mLatestVersion = "v" + reader.nextString();
                        } else if (name.equals("url")) {
                            mApkDownloadUrl = reader.nextString();
                            break;
                        } else {
                            reader.skipValue();
                        }
                    }
                }
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }

        if (mApkDownloadUrl == null) {
            throw new JSONException("No .apk download URL found");
        }
    }
}
