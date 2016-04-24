package com.maciejkucia.atlasbbplugin.repopullrequest.emiter;

import com.atlassian.bitbucket.repository.Repository;
import com.maciejkucia.atlasbbplugin.repopullrequest.hook.PullRequestHookLogger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class RestfulNotificationEmitter {

    private PullRequestHookLogger logger = null;

    public RestfulNotificationEmitter()
    {
        this.logger = PullRequestHookLogger.getInstance();
    }

    public void EmitAsync(Repository                         repo,
                          RestfulNotificationEmitterSettings settings,
                          HashMap<String, String>            substitutionMap) {
        new Thread(() -> {
            try {
                URL                     url             = settings.getURL(substitutionMap);
                String                  body            = settings.getBody(substitutionMap);
                String                  requestMethod   = settings.getRequestMethod();
                HashMap<String, String> headers         = settings.getHeaders();
                int                     contentLength   = body.length();
                int                     connectTimeout  = settings.getConnectTimeout();

                logger.putLog(repo, "Sending {0} {2} #{3} {1} ",
                        requestMethod, url, substitutionMap.get("action"), substitutionMap.get("pr_id"));
                if (contentLength > 0) {
                    logger.putLog(repo, "With body {0}", body.replace("\n", "<newline>"));
                }

                HttpURLConnection conn = buildConnection(url, body, requestMethod,
                        headers, contentLength, connectTimeout);

                int responseCode = conn.getResponseCode();
                if (responseCode < HttpServletResponse.SC_BAD_REQUEST) {
                    logger.putLog(repo, "Response {1} from {0}",
                            url, Integer.toString(responseCode));
                }
                else {
                    logger.putLogError(repo, "Response {1} from {0}",
                            url, Integer.toString(responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                logger.putLogError(repo, e.toString());
            }
        }).start();
    }

    private static HttpURLConnection buildConnection(URL                        url,
                                                     String                     body,
                                                     String                     requestMethod,
                                                     HashMap<String, String>    headers,
                                                     int                        contentLength,
                                                     int                        connectTimeout) throws IOException {
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
