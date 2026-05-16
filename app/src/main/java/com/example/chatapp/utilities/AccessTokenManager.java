package com.example.chatapp.utilities;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Manages Firebase OAuth2 access tokens for FCM v1 API.
 *
 * - Reads service account credentials from assets/service_account.json
 * - Mints a signed JWT and exchanges it for an access token
 * - Caches the token and auto-refreshes it 5 minutes before expiry
 * - Thread-safe; all network work runs off the main thread
 *
 * Usage:
 *   AccessTokenManager.getInstance(context).getToken(token -> {
 *       // use token on main thread
 *   });
 */
public class AccessTokenManager {

    private static final String TAG = "AccessTokenManager";
    private static final String ASSET_FILE = "chatapp-334b8-firebase-adminsdk-fbsvc-029f2931af.json";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    // Refresh 5 minutes before actual expiry (tokens last 1 hour = 3600s)
    private static final long EXPIRY_BUFFER_MS = 5 * 60 * 1000L;

    public interface TokenCallback {
        void onToken(String accessToken);
        default void onError(Exception e) {
            Log.e("AccessTokenManager", "Token fetch failed", e);
        }
    }

    private static volatile AccessTokenManager instance;

    private final Context appContext;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private String cachedToken = null;
    private long tokenExpiryMs = 0L; // epoch millis when token expires

    // Service account fields (loaded once from JSON)
    private String clientEmail;
    private String privateKeyPem;
    private String projectId;
    private boolean credentialsLoaded = false;

    private AccessTokenManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static AccessTokenManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AccessTokenManager.class) {
                if (instance == null) {
                    instance = new AccessTokenManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Returns a valid access token via callback (always on calling thread via Handler,
     * or use runOnUiThread if you need main-thread delivery).
     * Fetches/refreshes in background automatically.
     */
    public void getToken(TokenCallback callback) {
        // Fast path: cached token still valid
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryMs - EXPIRY_BUFFER_MS) {
            callback.onToken(cachedToken);
            return;
        }
        // Slow path: fetch new token off main thread
        executor.execute(() -> {
            try {
                if (!credentialsLoaded) {
                    loadCredentials();
                }
                String newToken = fetchAccessToken();
                // Cache it (expires in 3600s per Google spec)
                cachedToken = newToken;
                tokenExpiryMs = System.currentTimeMillis() + 3600_000L;
                // Deliver on whatever thread the executor uses;
                // callers should use runOnUiThread if needed for UI work.
                callback.onToken(newToken);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getProjectId(TokenCallback callback) {
        executor.execute(() -> {
            try {
                if (!credentialsLoaded) {
                    loadCredentials();
                }
                callback.onToken(projectId);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Credentials loading
    // -------------------------------------------------------------------------

    private void loadCredentials() throws Exception {
        InputStream is = appContext.getAssets().open(ASSET_FILE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        JSONObject json = new JSONObject(sb.toString());
        clientEmail = json.getString("client_email");
        privateKeyPem = json.getString("private_key");
        projectId = json.getString("project_id");
        credentialsLoaded = true;
    }

    // -------------------------------------------------------------------------
    // JWT + token exchange
    // -------------------------------------------------------------------------

    private String fetchAccessToken() throws Exception {
        String jwt = buildJwt();
        return exchangeJwtForToken(jwt);
    }

    private String buildJwt() throws Exception {
        long nowSec = System.currentTimeMillis() / 1000;
        long expSec = nowSec + 3600;

        // Header
        JSONObject headerJson = new JSONObject();
        headerJson.put("alg", "RS256");
        headerJson.put("typ", "JWT");
        String headerB64 = base64UrlEncode(headerJson.toString().getBytes(StandardCharsets.UTF_8));

        // Claim set
        JSONObject claimJson = new JSONObject();
        claimJson.put("iss", clientEmail);
        claimJson.put("scope", SCOPE);
        claimJson.put("aud", TOKEN_URL);
        claimJson.put("iat", nowSec);
        claimJson.put("exp", expSec);
        String claimB64 = base64UrlEncode(claimJson.toString().getBytes(StandardCharsets.UTF_8));

        String signingInput = headerB64 + "." + claimB64;

        // Sign with RS256
        PrivateKey privateKey = loadPrivateKey(privateKeyPem);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String sigB64 = base64UrlEncode(sig.sign());

        return signingInput + "." + sigB64;
    }

    private String exchangeJwtForToken(String jwt) throws Exception {
        String postBody = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8")
                + "&assertion=" + URLEncoder.encode(jwt, "UTF-8");

        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream responseStream = responseCode == 200
                ? conn.getInputStream()
                : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        if (responseCode != 200) {
            throw new IOException("Token exchange failed (" + responseCode + "): " + sb);
        }

        JSONObject responseJson = new JSONObject(sb.toString());
        return responseJson.getString("access_token");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        // Strip PEM header/footer and whitespace
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private String base64UrlEncode(byte[] data) {
        return android.util.Base64.encodeToString(data,
                android.util.Base64.URL_SAFE |
                        android.util.Base64.NO_PADDING |
                        android.util.Base64.NO_WRAP);
    }
}
