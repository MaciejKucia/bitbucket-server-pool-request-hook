package com.maciejkucia.atlasbbplugin.repopullrequest.emiter;

import com.atlassian.bitbucket.commit.MinimalCommit;
import com.atlassian.bitbucket.event.pull.*;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Ref;
import com.maciejkucia.atlasbbplugin.repopullrequest.Catalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ValueMapGenerator {
    private static final Logger log = LoggerFactory.getLogger(Catalog.LOGGER_KEY);

    public HashMap<String, String> extractIntoMap(PullRequestEvent pullRequestEvent)
    {
        HashMap<String, String> ret = new HashMap<>(15);
        try {
            extractGeneral(pullRequestEvent, ret);
            extractCommentEvent(pullRequestEvent, ret);
            extractMergedEvent(pullRequestEvent, ret);
            extractUpdateEvent(pullRequestEvent, ret);

            StringBuilder sBuilder = new StringBuilder();
            for (String key : ret.keySet()) {
                sBuilder.append(key);
                sBuilder.append(',');
            }
            ret.put("all_keys", sBuilder.toString());
        }
        catch (Exception e) {
            log.error(e.toString());
        }
        return ret;
    }

    private void extractGeneral(PullRequestEvent pullRequestEvent, HashMap<String, String> ret) {
        PullRequest pullRequest = pullRequestEvent.getPullRequest();
        ret.put("project",          pullRequest.getToRef().getRepository().getProject().getKey());
        ret.put("repo",             pullRequest.getToRef().getRepository().getSlug());
        ret.put("action",           pullRequestEvent.getAction().name());
        ret.put("pr_id",            Long.toString(pullRequest.getId()));
        ret.put("pr_title",         pullRequest.getTitle());
        ret.put("pr_author",        pullRequest.getAuthor().getUser().getDisplayName());
        ret.put("pr_version",       Integer.toString(pullRequest.getVersion()));
        ret.put("pr_description",   pullRequest.getDescription());
        ret.put("pr_tobranch",      pullRequest.getToRef().getId());
    }

    private void extractUpdateEvent(PullRequestEvent pullRequestEvent, HashMap<String, String> ret) {
        if (pullRequestEvent instanceof PullRequestUpdatedEvent) {
            PullRequestUpdatedEvent updatedEvent = (PullRequestUpdatedEvent) pullRequestEvent;
            ret.put("previous_description", updatedEvent.getPreviousDescription());
            ret.put("previous_title", updatedEvent.getPreviousTitle());
            Ref previousBranch =  updatedEvent.getPreviousToBranch();
            if (previousBranch != null) {
                ret.put("previous_tobranch",previousBranch.getId());
            }
        }
    }

    private void extractMergedEvent(PullRequestEvent pullRequestEvent, HashMap<String, String> ret) {
        if (pullRequestEvent instanceof PullRequestMergedEvent) {
            PullRequestMergedEvent mergedEvent = (PullRequestMergedEvent) pullRequestEvent;

            ret.put("merge_isremotely", Boolean.toString(mergedEvent.isMergedRemotely()));
            MinimalCommit commit = mergedEvent.getCommit();
            if (commit != null) {
                ret.put("merge_commit", commit.getId());
            }
        }
    }

    private void extractCommentEvent(PullRequestEvent pullRequestEvent, HashMap<String, String> ret) {
        if (pullRequestEvent instanceof PullRequestCommentEvent) {
            PullRequestCommentEvent commentEvent = (PullRequestCommentEvent) pullRequestEvent;

            ret.put("comment_action", commentEvent.getCommentAction().name());
            ret.put("comment_text",   commentEvent.getComment().getText());
            ret.put("comment_author", commentEvent.getComment().getAuthor().getDisplayName());
        }
    }

}
