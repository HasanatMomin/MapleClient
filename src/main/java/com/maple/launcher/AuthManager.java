package com.maple.launcher;

import fr.litarvan.openauth.microsoft.*;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import java.util.UUID;

public class AuthManager {
    public static AuthInfos loginMicrosoft() throws Exception {
        MicrosoftAuthenticator auth = new MicrosoftAuthenticator();
        MicrosoftAuthResult result = auth.loginWithWebview();
        AuthSessionManager.saveEncryptedSession(
            result.getProfile().getName(), 
            result.getProfile().getId(), 
            result.getAccessToken(), 
            result.getRefreshToken()
        );
        return new AuthInfos(result.getProfile().getName(), result.getAccessToken(), result.getProfile().getId());
    }

    public static AuthInfos loginCracked(String user) {
        String offlineUuid = UUID.nameUUIDFromBytes(("Offline:" + user).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        return new AuthInfos(user, "token", offlineUuid);
    }
}