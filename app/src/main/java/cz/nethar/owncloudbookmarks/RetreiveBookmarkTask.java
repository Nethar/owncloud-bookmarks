package cz.nethar.owncloudbookmarks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import static cz.nethar.owncloudbookmarks.TagList.loadedBookmarks;
import static cz.nethar.owncloudbookmarks.TagList.loadedBookmarksUpdated;


/**
 * Created by schabesbergerch64244 on 08.02.17.
 */

class RetreiveBookmarkTask extends AsyncTask<String, Void, String> {

    public static final String TAG = RetreiveBookmarkTask.class.toString();
    public static final String REST_PATH = "/index.php/apps/bookmarks/public/rest/v1/bookmark";

    private Uri ocUri;
    private String user;
    private String password;
    private boolean acceptCert;
    private Exception exception;
    ProgressDialog progressDialog;
    private Boolean background;
    private int httpsPort;
    private Context context;
    private BookmarkMaps receivedBookamrks = null;

    @Override
    protected void onPreExecute()
    {
        if (!background) {
            progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading_bookmarks));
        }
    }

    RetreiveBookmarkTask(String url, String user, String password, boolean acceptCert, Boolean background, int httpsPort, Context context) {
        this.ocUri = Uri.parse(url);
        this.user = user;
        this.password = password;
        this.acceptCert = acceptCert;
        exception = null;
        this.background = background;
        this.httpsPort = httpsPort;
        this.context = context;
    }

    protected String doInBackground(String... urls) {

        int responseCode;
        boolean success = false;
        for(int tries = 0; tries < 2 && !success; tries++) {

            try {
                Uri uri = new Uri.Builder()
                        .scheme(ocUri.getScheme())
                        .encodedAuthority(ocUri.getAuthority()+":"+Integer.toString(httpsPort))
                        .appendEncodedPath(ocUri.getPath().substring(1) + REST_PATH.substring(1))
                        .appendQueryParameter("user", user)
                        .appendQueryParameter("password", password)
                        .build();
                URL url = new URL(uri.toString());

                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;

                StringBuilder responseBuilder = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseBuilder.append(inputLine);
                }
                String result = responseBuilder.toString();

                if (isCancelled()) {
                    return "cancelled";
                }

                responseCode = con.getResponseCode();
                if (responseCode != 200) {
                    return "Cannot connect to server. Check hostname and username/password (response code  " + responseCode + ").";
                }

                JSONArray jsonArray = new JSONArray(result);
                receivedBookamrks = null;
            } catch (Exception e) {
                exception = e;
                receivedBookamrks = null;
            }
        }

        return "";
    }

    protected void onCancelled(String errorMessage) {
        if (!background && progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        //Toast.makeText(TagList.this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    protected void onPostExecute(String errorMessage) {
        if (!background && progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (exception != null) {
            if (exception.getClass() == SSLPeerUnverifiedException.class) {
                Toast.makeText(context, exception.getMessage() + " (import certificate to your Android trusted credentials storage or accept it in app settings)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, exception.getMessage() + " " + exception.getClass().toString(), Toast.LENGTH_LONG).show();
            }
        } else if (errorMessage != null && errorMessage.length() > 0) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        } else {
            if (!background) {
                loadedBookmarksUpdated = receivedBookamrks;
                loadedBookmarks = loadedBookmarksUpdated;
                loadedBookmarksUpdated = null;
                //fillListView();
            }
        }
    }
}