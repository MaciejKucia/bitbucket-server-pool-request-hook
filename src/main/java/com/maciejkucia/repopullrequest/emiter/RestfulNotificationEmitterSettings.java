package com.maciejkucia.repopullrequest.emiter;

import com.atlassian.bitbucket.setting.Settings;
import org.apache.commons.lang3.text.StrSubstitutor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maciej on 2016-04-01.
 */
public class RestfulNotificationEmitterSettings {
    private Settings settings;
    private final int CONNECT_TIMEOUT = 5000;

    public RestfulNotificationEmitterSettings(Settings settings) {
        this.settings = settings;
    }

    public URL getURL(Map<String, String> substitutionMap) {
        try {
            String textUrl = settings.getString("url").replace(" ", "");
            StrSubstitutor sub = new StrSubstitutor(substitutionMap, "${", "}");
            if (textUrl != null) {
                return new URL(sub.replace(textUrl));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public String getRequestMethod() {
        //todo: check if correct
        return settings.getString("restOperation");
    }

    public Boolean isPullRequestEventEnabled(Type type) {
        try {
            String[] name = type.getTypeName().split("\\.");
            Boolean result = settings.getBoolean(name[name.length - 1]);
            return result == null ? false : result;
        }
        catch (Exception e) {
            return false;
        }
    }

    public X509Certificate getCertificates() {
        CertificateFactory fact = null;
        X509Certificate cer = null;
        try {
            fact = CertificateFactory.getInstance("X.509");
            String certsText = settings.getString("certs");
            if (certsText != null) {
                InputStream is = new ByteArrayInputStream(certsText.getBytes(Charset.defaultCharset()));
                cer = (X509Certificate) fact.generateCertificate(is);
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return cer;
    }

    public HashMap<String, String> getHeaders() {
        HashMap<String, String> ret             = new HashMap<>();
        String                  colonHeaders    = settings.getString("headers");

        if (colonHeaders.isEmpty()) {
            return ret;
        }
        String[] lines = colonHeaders.split("\n");
        for(String line :lines){
            try {
                String[] splitLine = line.split(":");
                if (splitLine.length > 0) {
                    ret.put(splitLine[0].trim(), splitLine[1].trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public String getBody(Map<String, String> substitutionMap) {
        StrSubstitutor sub = new StrSubstitutor(substitutionMap, "${", "}");
        String templateString = settings.getString("messageBody");
        if (templateString.isEmpty()) {
            return "";
        }
        return sub.replace(templateString);
    }

    public int getConnectTimeout() {
        return CONNECT_TIMEOUT;
    }
}
