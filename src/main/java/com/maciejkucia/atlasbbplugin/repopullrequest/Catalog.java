package com.maciejkucia.atlasbbplugin.repopullrequest;

public final class Catalog {

    private Catalog() { }

    public static final int MAX_LOG_ELEMENTS = 10;

    public static final int MAX_LOG_LINE_LENGTH = 1024;

    public static final String REPO_HOOK_KEY =
            "com.maciejkucia.atlasbbplugin.repopullrequest:repopullrequest-hook";

    public static final int CONNECT_TIMEOUT = 5000;

    public static final String LOGGER_KEY = "atlassian.plugin.pullrequesthook";
}
