package com.maple.launcher;

import fr.theshark34.openlauncherlib.minecraft.AuthInfos;

public class AuthManager {

    /**
     * Microsoft login called with zero arguments from AccountsController.
     */
    public static AuthInfos loginMicrosoft() {
        AuthInfos session = AuthSessionManager.getValidAccessToken();
        if (session != null) {
            return session;
        }
        return new AuthInfos("Player", "demo-token", "demo-uuid");
    }

    /**
     * Microsoft login called with parameters.
     */
    public static AuthInfos loginMicrosoft(String accessToken, String refreshToken, String username, String uuid, long expiresInSeconds) {
        AuthSessionManager.saveEncryptedSession(accessToken, refreshToken, username, uuid, expiresInSeconds);
        return new AuthInfos(username, accessToken, uuid);
    }

    /**
     * Offline / Cracked login mode.
     */
    public static AuthInfos loginCracked(String username) {
        return new AuthInfos(username, "offline-token", "offline-uuid");
    }

    public static AuthInfos loginOffline(String username) {
        return loginCracked(username);
    }

    /**
     * Checks if a valid session exists on app startup.
     */
    public static AuthInfos resolveSession() {
        return AuthSessionManager.getValidAccessToken();
    }
}
