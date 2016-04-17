package com.maciejkucia.repopullrequest.emiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RestfulNotificationEmitter {
    private static final Logger log = LoggerFactory.getLogger("atlassian.plugin.pullrequesthook");

    public static void EmitAsync(RestfulNotificationEmitterSettings settings,
                                 HashMap<String, String> substitutionMap) {
        new Thread(() -> {
            try {
                URL                     url             = settings.getURL(substitutionMap);
                String                  body            = settings.getBody(substitutionMap);
                String                  requestMethod   = settings.getRequestMethod();
                HashMap<String, String> headers         = settings.getHeaders();
                int                     contentLength   = body.length();
                int                     connectTimeout  = settings.getConnectTimeout();

                log.debug("Emitting notification {0} {1}", requestMethod, url);
                HttpURLConnection conn = buildConnection(url, body, requestMethod, headers, contentLength, connectTimeout);

                int responseCode = conn.getResponseCode();
                if (responseCode < 400) {
                    log.debug("Notification response from {0} {1}", url, responseCode);
                }
                else {
                    log.error("Notification response from {0} {1}", url, responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                log.error(e.toString());
            }
        }).start();
    }

    private static HttpURLConnection buildConnection(URL url, String body, String requestMethod, HashMap<String, String> headers, int contentLength, int connectTimeout) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(requestMethod);
        conn.setConnectTimeout(connectTimeout);
        conn.setInstanceFollowRedirects(false);
        for(Map.Entry<String, String> header : headers.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }

        if (requestMethod.equals("POST") || requestMethod.equals("PUT")) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Length", Integer.toString(contentLength));
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes(Charset.forName("UTF-8")));
            os.flush();
        }
        return conn;
    }
}
