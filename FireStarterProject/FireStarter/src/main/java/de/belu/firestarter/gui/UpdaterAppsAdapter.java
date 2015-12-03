package de.belu.firestarter.gui;

import android.app.Activity;
import android.content.Context;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.belu.firestarter.R;
import de.belu.firestarter.tools.FireStarterUpdater;
import de.belu.firestarter.tools.KodiUpdater;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.UlangoTVUpdater;
import de.belu.firestarter.tools.Updater;

/**
 * Adapter that lists all installed apps
 */
public class UpdaterAppsAdapter extends BaseAdapter {
    Activity mActivity;
    private List<Updater> mUpdaterList;
    String androidPackagesUrl = "https://ulango.tv/android.json";
    private List<String> ulangoPackages = null;
    UlangoUpdaterRunnable updateRunnable = null;
    class UlangoUpdaterRunnable implements Runnable {

        Thread t;

        UlangoUpdaterRunnable() {
            t = new Thread(this);
            t.start();
        }

        @Override
        public void run() {
            try {
                ulangoPackages = getPackagesFromUlangoAndroidUserAppStore();
                Iterator<String> iterator = ulangoPackages.iterator();
                while (iterator.hasNext()) {
                    String names = iterator.next();
                    String[] separated = names.split("\\|");
                    mUpdaterList.add(new UlangoTVUpdater(separated[0], separated[1]));
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Create new UpdaterAppsadapter
     */
    public UpdaterAppsAdapter(Activity activity) {
        // Set context
        mActivity = activity;

        // Set list of updaters
        mUpdaterList = new ArrayList<>();
        //mUpdaterList.add(new FireStarterUpdater());
        mUpdaterList.add(new KodiUpdater(activity));
        updateRunnable = new UlangoUpdaterRunnable();
    }

    /**
     * @return Count of installed apps
     */

    public int getCount() {
        try {
            updateRunnable.t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mUpdaterList.size();
    }

    /**
     * @param position Position of item to be returned
     * @return Item on position
     */

    public Object getItem(int position) {
        try {
            updateRunnable.t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mUpdaterList.get(position);
    }

    /**
     * Currently not used..
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * @return View of the given position
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get act updater
        try {
            updateRunnable.t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Updater actUpdater = mUpdaterList.get(position);

        // Inflate layout
        View rootView;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = new View(mActivity);
            rootView = inflater.inflate(R.layout.appupdateritemlayout, parent, false);

        } else {
            rootView = (View) convertView;
        }

        // Set title
        TextView textViewTitle = (TextView) rootView.findViewById(R.id.title);
        textViewTitle.setText(actUpdater.getAppName());

        // Set current version
        TextView textViewCurrentVersion = (TextView) rootView.findViewById(R.id.currentVersion);
        textViewCurrentVersion.setText(actUpdater.getCurrentVersion(mActivity));

        // Set latest version
        final TextView textViewLatestVersion = (TextView) rootView.findViewById(R.id.latestVersion);
        String latestVersion = actUpdater.getLatestVersion();
        if (latestVersion == null) {
            latestVersion = mActivity.getResources().getString(R.string.update_hitcheckfor);
        }
        textViewLatestVersion.setText(latestVersion);

        // Create an UpdaterDialogHandler
        final UpdaterDialogHandler updaterDialogHandler = new UpdaterDialogHandler(mActivity, actUpdater);
        actUpdater.DialogHandler = updaterDialogHandler;
        updaterDialogHandler.setCheckForUpdateFinishedListener(new Updater.OnCheckForUpdateFinishedListener() {
            @Override
            public void onCheckForUpdateFinished(String message) {
                if (actUpdater.getLatestVersion() != null) {
                    if (actUpdater.isVersionNewer(actUpdater.getCurrentVersion(mActivity), actUpdater.getLatestVersion())) {
                        textViewLatestVersion.setText(actUpdater.getLatestVersion() + " - " + mActivity.getResources().getString(R.string.update_foundnew));

                        if (actUpdater instanceof FireStarterUpdater) {
                            AppActivity.LATEST_APP_VERSION = actUpdater.getLatestVersion();
                        }
                    } else {
                        textViewLatestVersion.setText(actUpdater.getLatestVersion() + " - " + mActivity.getResources().getString(R.string.update_foundnotnew));
                    }
                } else {
                    textViewLatestVersion.setText(message);
                }
            }
        });

        // Set the button onclicks
        Button checkUpdateButton = (Button) rootView.findViewById(R.id.buttonCheckForUpdate);
        checkUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updaterDialogHandler.checkForUpdate();
            }
        });

        Button updateButton = (Button) rootView.findViewById(R.id.buttonUpdate);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updaterDialogHandler.performUpdate();

                if (actUpdater instanceof FireStarterUpdater) {
                    SettingsProvider settings = SettingsProvider.getInstance(mActivity);
                    settings.setHaveUpdateSeen(false);
                }
            }
        });

        return rootView;
    }

    private List<String> getPackagesFromUlangoAndroidUserAppStore() {
        ArrayList<String> result = new ArrayList<String>();
        InputStream is = null;
        try {
            URL url = new URL(androidPackagesUrl);
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
                    String package_name = null;
                    String app_name = null;
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("app_name")) {
                            app_name = reader.nextString();
                        } else if (name.equals("package_name")) {
                            package_name = reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                    }
                    if (package_name != null) {
                        result.add(app_name + "|" + package_name);
                    }
                    reader.endObject();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}

