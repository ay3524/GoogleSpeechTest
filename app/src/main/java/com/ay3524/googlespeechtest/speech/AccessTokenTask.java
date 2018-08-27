package com.ay3524.googlespeechtest.speech;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.ay3524.googlespeechtest.R;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    /**
     * We reuse an access token if its expiration time is longer than this.
     */
    public static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes
    /**
     * We refresh the current access token before it expires.
     */
    public static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute

    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    public static final String HOSTNAME = "speech.googleapis.com";
    public static final int PORT = 443;

    private TokenListener tokenListener;
    private Context context;

    public AccessTokenTask(Context context, TokenListener tokenListener) {
        this.context = context;
        this.tokenListener = tokenListener;
    }

    @Override
    protected AccessToken doInBackground(Void... voids) {
        final SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
        long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

        // Check if the current token is still valid for a while
        if (tokenValue != null && expirationTime > 0) {
            if (expirationTime
                    > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                return new AccessToken(tokenValue, new Date(expirationTime));
            }
        }

        // ***** WARNING *****
        // In this sample, we load the credential from a JSON file stored in a raw resource
        // folder of this client app. You should never do this in your app. Instead, store
        // the file in your server and obtain an access token from there.
        // *******************
        final InputStream stream = context.getResources().openRawResource(R.raw.credential);
        try {
            final GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(SCOPE);
            final AccessToken token = credentials.refreshAccessToken();
            prefs.edit()
                    .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                    .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME,
                            token.getExpirationTime().getTime())
                    .apply();
            return token;
        } catch (IOException e) {
            tokenListener.onTokenFail("Failed to obtain access token. " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(AccessToken accessToken) {
        tokenListener.onTokenSuccess(accessToken);
    }
}
