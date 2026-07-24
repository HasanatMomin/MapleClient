package com.maple.launcher.services;

import com.maple.launcher.AccountManager;
import com.maple.launcher.AuthSessionManager;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;

public final class PremiumTokenService {
    private PremiumTokenService() {}

    public static String ensureMcToken(String username) throws Exception {
        AccountManager.AccountRecord rec = AccountManager.find(username);

        // Check if account exists and has the PREMIUM role
        if (rec == null || !rec.role().equals("PREMIUM")) {
            return null;
        }

        // Use the cached token if it expires more than 5 minutes from now
        if (rec.mcToken() != null && rec.expiresAt() != null && rec.expiresAt() > (System.currentTimeMillis() + 300000)) {
            return rec.mcToken();
        }

        // If expired or missing, refresh via AuthSessionManager
        AuthInfos refreshed = AuthSessionManager.getValidAccessToken();
        if (refreshed != null) {
            AccountManager.saveAccount(refreshed, true);
            return refreshed.getAccessToken();
        }

        return null;
    }
}