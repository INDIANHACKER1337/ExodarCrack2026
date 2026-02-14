/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.account.auth;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

public class MicrosoftLogin {

    private static final String MS_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String AUTH_XBL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String AUTH_XSTS = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MINECRAFT_AUTH = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MINECRAFT_PROFILE = "https://api.minecraftservices.com/minecraft/profile";

    private static final String CLIENT_ID = "907a248d-3eb5-4d01-99d2-ff72d79c5eb1";
    private static final String REDIRECT_PATH = "relogin";
    private static final String REDIRECT = "http://localhost:1337/" + REDIRECT_PATH;
    private static final String MS_AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
            + "?client_id=" + CLIENT_ID + "&response_type=code" + "&redirect_uri=" + REDIRECT
            + "&scope=XboxLive.signin%20offline_access" + "&prompt=select_account" + "&code_challenge_method=S256"
            + "&code_challenge=";

    private static final int PORT = 1337;

    private static HttpServer server;
    private static Consumer<MicrosoftAccount> loginCallback;
    private static String pkceVerifier;
    private static String pkceChallenge;
    private static boolean loginInProgress = false;

    public static boolean isLoginInProgress() {
        return loginInProgress;
    }

    public static void startLogin(Consumer<MicrosoftAccount> onLogin) {
        if (loginInProgress) {
            System.out.println("[MicrosoftLogin] Login already in progress");
            return;
        }

        loginInProgress = true;
        loginCallback = onLogin;

        generatePkce();
        startServer();

        openBrowser(MS_AUTH_URL + pkceChallenge);
        System.out.println("[MicrosoftLogin] Browser opened for authentication");
    }

    private static void startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/" + REDIRECT_PATH, exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = getQueryParam(query, "code");

                if (code == null) {
                    write(exchange, "<html><body><h3>Error: Missing code</h3></body></html>");
                    loginInProgress = false;
                    return;
                }

                new Thread(() -> handleCode(code)).start();
                write(exchange, "<html><body style='font-family:Arial;text-align:center;padding:50px;'><h2>Login Successful!</h2><p>You may close this tab and return to Minecraft.</p></body></html>");
            });
            server.start();
            System.out.println("[MicrosoftLogin] Server started on port " + PORT);
        } catch (Exception e) {
            System.out.println("[MicrosoftLogin] Failed to start server: " + e.getMessage());
            loginInProgress = false;
        }
    }

    private static void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        loginInProgress = false;
    }

    private static void handleCode(String code) {
        try {
            String res = Browser.postExternal(MS_TOKEN_URL, "client_id=" + CLIENT_ID + "&grant_type=authorization_code"
                    + "&code=" + code + "&redirect_uri=" + REDIRECT + "&code_verifier=" + pkceVerifier, false);

            if (res == null) {
                System.out.println("[MicrosoftLogin] Failed to get MS token");
                stopServer();
                return;
            }

            JsonObject obj = JsonParser.parseString(res).getAsJsonObject();
            String accessToken = obj.has("access_token") ? obj.get("access_token").getAsString() : null;
            String refreshToken = obj.has("refresh_token") ? obj.get("refresh_token").getAsString() : null;

            if (accessToken == null) {
                System.out.println("[MicrosoftLogin] No access token in response");
                stopServer();
                return;
            }

            MicrosoftAccount account = loginWithAccessToken(accessToken, refreshToken);
            if (account != null) {
                applySession(account);
                if (loginCallback != null) {
                    loginCallback.accept(account);
                }
                System.out.println("[MicrosoftLogin] Successfully logged in as " + account.username);
            }
        } catch (Exception e) {
            System.out.println("[MicrosoftLogin] Error handling code: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stopServer();
        }
    }

    public static MicrosoftAccount loginWithRefreshToken(String refreshToken) {
        try {
            String res = Browser.postExternal(MS_TOKEN_URL, "client_id=" + CLIENT_ID + "&grant_type=refresh_token"
                    + "&refresh_token=" + refreshToken + "&redirect_uri=" + REDIRECT, false);

            if (res == null) {
                System.out.println("[MicrosoftLogin] Failed to refresh token");
                return null;
            }

            JsonObject obj = JsonParser.parseString(res).getAsJsonObject();
            String accessToken = obj.has("access_token") ? obj.get("access_token").getAsString() : null;
            String newRefresh = obj.has("refresh_token") ? obj.get("refresh_token").getAsString() : null;

            if (accessToken == null) {
                System.out.println("[MicrosoftLogin] No access token from refresh");
                return null;
            }

            MicrosoftAccount account = loginWithAccessToken(accessToken, newRefresh);
            if (account != null) {
                applySession(account);
                System.out.println("[MicrosoftLogin] Successfully refreshed session for " + account.username);
            }

            return account;
        } catch (Exception e) {
            System.out.println("[MicrosoftLogin] Error refreshing token: " + e.getMessage());
            return null;
        }
    }

    private static MicrosoftAccount loginWithAccessToken(String accessToken, String refreshToken) {
        try {
            String xblRes = Browser.postExternal(AUTH_XBL,
                    "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
                            + accessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}",
                    true);

            if (xblRes == null) return null;

            JsonObject xbl = JsonParser.parseString(xblRes).getAsJsonObject();
            String xblToken = xbl.get("Token").getAsString();
            String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

            String xstsRes = Browser.postExternal(AUTH_XSTS,
                    "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken
                            + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}",
                    true);

            if (xstsRes == null) return null;

            JsonObject xsts = JsonParser.parseString(xstsRes).getAsJsonObject();
            String xstsToken = xsts.get("Token").getAsString();

            String mcRes = Browser.postExternal(MINECRAFT_AUTH,
                    "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}", true);

            if (mcRes == null) return null;

            JsonObject mc = JsonParser.parseString(mcRes).getAsJsonObject();
            String mcToken = mc.get("access_token").getAsString();

            String profileRes = Browser.getBearerResponse(MINECRAFT_PROFILE, mcToken);
            if (profileRes == null) return null;

            JsonObject profile = JsonParser.parseString(profileRes).getAsJsonObject();
            String uuid = profile.get("id").getAsString();
            String username = profile.get("name").getAsString();

            return new MicrosoftAccount(mcToken, refreshToken, uuid, username);
        } catch (Exception e) {
            System.out.println("[MicrosoftLogin] Error in auth flow: " + e.getMessage());
            return null;
        }
    }

    private static List<Field> sessionFields = null;

    private static void initSessionFields() {
        if (sessionFields != null) return;
        sessionFields = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getMinecraft();
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType() == Session.class) {
                    f.setAccessible(true);
                    sessionFields.add(f);
                }
            }
            Class<?> superClass = mc.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                for (Field f : superClass.getDeclaredFields()) {
                    if (f.getType() == Session.class) {
                        f.setAccessible(true);
                        sessionFields.add(f);
                    }
                }
                superClass = superClass.getSuperclass();
            }
        } catch (Exception e) {
            System.out.println("[MicrosoftLogin] Failed to init session fields: " + e.getMessage());
        }
    }

    public static void applySession(MicrosoftAccount account) {
        try {
            initSessionFields();
            Minecraft mc = Minecraft.getMinecraft();
            Session newSession = new Session(account.username, account.uuid, account.mcToken, "mojang");

            for (Field field : sessionFields) {
                try {
                    field.set(mc, newSession);
                } catch (Exception e) {
                    // Silent
                }
            }
            System.out.println("[MicrosoftLogin] Session applied: " + account.username);
        } catch (Exception e) {
            System.out.println("[MicrosoftLogin] Failed to apply session: " + e.getMessage());
        }
    }

    private static void generatePkce() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        pkceVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pkceChallenge = base64UrlSha256(pkceVerifier);
    }

    private static String base64UrlSha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }

    private static void write(HttpExchange ex, String html) {
        try {
            byte[] data = html.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
            System.out.println("[MicrosoftLogin] Could not open browser, URL copied to clipboard");
        }
    }

    private static String getQueryParam(String query, String key) {
        if (query == null) return null;
        try {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2 && kv[0].equals(key)) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
