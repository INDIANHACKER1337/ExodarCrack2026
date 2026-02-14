/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.account.auth;

public class MicrosoftAccount {
    public String mcToken;
    public String refreshToken;
    public String uuid;
    public String username;
    public long lastLogin;
    public boolean favorite;

    public MicrosoftAccount() {}

    public MicrosoftAccount(String mcToken, String refreshToken, String uuid, String username) {
        this.mcToken = mcToken;
        this.refreshToken = refreshToken;
        this.uuid = uuid;
        this.username = username;
        this.lastLogin = System.currentTimeMillis();
        this.favorite = false;
    }

    public String getDisplayName() {
        return username != null ? username : "Unknown";
    }

    public boolean isValid() {
        return refreshToken != null && !refreshToken.isEmpty();
    }
}
