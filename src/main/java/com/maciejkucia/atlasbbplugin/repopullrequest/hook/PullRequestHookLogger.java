package com.maciejkucia.atlasbbplugin.repopullrequest.hook;

import com.atlassian.bitbucket.repository.Repository;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import com.maciejkucia.atlasbbplugin.repopullrequest.Catalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

public class PullRequestHookLogger {

    private static PullRequestHookLogger singleton = null;

    private PullRequestHookLogger(){ }

    public static PullRequestHookLogger getInstance( ) {
        if (singleton == null) {
            singleton = new PullRequestHookLogger();
        }
        return singleton;
    }

    private final HashMap<String, Queue<String>> logQueues = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(Catalog.LOGGER_KEY);

    public void putLog(Repository repo, String message, Object... objects)
    {
        log.debug(message, objects);
        String        key   = buildKey(repo);
        Queue<String> queue = getRepositoryQueue(key);
        queue.add(buildLogMessage(message, objects));
    }

    @Nonnull
    private String buildLogMessage(String message, Object[] objects) {
        String longMessage = getIsoTime() + MessageFormat.format(message, objects);
        return longMessage.substring(0, Math.min(longMessage.length(), Catalog.MAX_LOG_LINE_LENGTH));
    }

    private Queue<String> getRepositoryQueue(String key) {
        Queue<String> queue;
        if (!logQueues.containsKey(key)) {
            queue = Queues.synchronizedQueue(EvictingQueue.create(Catalog.MAX_LOG_ELEMENTS));
            logQueues.put(key, queue);
        }
        else {
            queue = logQueues.get(key);
        }
        return queue;
    }

    private String buildKey(Repository repo) {
        return repo.getProject().getKey().toLowerCase() + ":" + repo.getSlug().toLowerCase();
    }

    public String[] getLogs(Repository repo)
    {
        String key = buildKey(repo);
        if (!logQueues.containsKey(key)) {
            return new String[] {"No logs for " + key};
        }
        else {
            List<String> ret = new ArrayList<>(Catalog.MAX_LOG_ELEMENTS);
            ret.addAll(logQueues.get(key));
            return ret.toArray(new String[ret.size()]);
        }
    }

    private String getIsoTime()
    {
       return ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT) + " ";
    }

    public void putLogError(Repository repo, String message, Object... objects) {
        log.error(message, objects);
        putLog(repo, "[error]" + message, objects);
    }
}
