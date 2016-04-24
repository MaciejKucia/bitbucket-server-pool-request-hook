package com.maciejkucia.atlasbbplugin.repopullrequest.emiter;

import com.atlassian.bitbucket.setting.Settings;
import com.maciejkucia.atlasbbplugin.repopullrequest.Catalog;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RestfulNotificationEmitterSettings {
    private final Settings settings;

    public RestfulNotificationEmitterSettings(Settings settings) {
        this.settings = settings;
    }

    URL getURL(Map<String, String> substitutionMap) {
        try {
            String url = settings.getString("url");
            if (url != null) {
                String textUrl = url.replace(" ", "");
                StrSubstitutor sub = new StrSubstitutor(substitutionMap, "${", "}");
                if (textUrl != null) {
                    return new URL(sub.replace(textUrl));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    String getRequestMethod() {
        String value = settings.getString("restOperation");
        if (value != null) {
            switch (value.toUpperCase()) {
                case "POST":   return "POST";
                case "GET":    return "GET";
                case "PUT":    return "PUT";
                case "DELETE": return "DELETE";
                default:       return "POST";
            }
        }
        return "POST";
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

    HashMap<String, String> getHeaders() {
        HashMap<String, String> ret             = new HashMap<>();
        String                  colonHeaders    = settings.getString("headers");

        if (colonHeaders != null && colonHeaders.isEmpty()) {
            return ret;
        }
        String[] lines = colonHeaders != null ? colonHeaders.split("\n") : new String[0];
        for (String line : lines) {
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

    String getBody(Map<String, String> substitutionMap) {
        StrSubstitutor sub = new StrSubstitutor(substitutionMap, "${", "}");
        String templateString = settings.getString("messageBody");
        if (templateString != null && templateString.isEmpty()) {
            return "";
        }
        return sub.replace(templateString);
    }

    int getConnectTimeout() {
        return Catalog.CONNECT_TIMEOUT;
    }
}
