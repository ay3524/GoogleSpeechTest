package com.ay3524.googlespeechtest.speech;

import com.google.auth.oauth2.AccessToken;

public interface TokenListener {
    void onTokenSuccess(AccessToken accessToken);
    void onTokenFail(String message);
}
