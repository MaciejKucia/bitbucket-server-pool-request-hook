package com.maciejkucia.repopullrequest.emiter;

import com.atlassian.bitbucket.commit.MinimalCommit;
import com.atlassian.bitbucket.event.pull.*;
import java.util.HashMap;

/**
 * Created by Maciej on 2016-04-05.
 */
public class ValueMapGenerator {

    // Extract useful data and put it into a map
    //
    public HashMap<String, String> getMap(PullRequestEvent pullRequestEvent)
    {
        HashMap<String, String> ret = new HashMap<>(5);

        ret.put("project", pullRequestEvent.getPullRequest().getToRef().getRepository().getProject().getKey());
        ret.put("repo", pullRequestEvent.getPullRequest().getToRef().getRepository().getSlug());

        ret.put("action", pullRequestEvent.getAction().name());
        ret.put("pr_id", Long.toString(pullRequestEvent.getPullRequest().getId()));
        ret.put("pr_title", pullRequestEvent.getPullRequest().getTitle());
        ret.put("pr_author", pullRequestEvent.getPullRequest().getAuthor().getUser().getDisplayName());
        ret.put("pr_version", Integer.toString(pullRequestEvent.getPullRequest().getVersion()));

        if (pullRequestEvent instanceof PullRequestCommentEvent) {
            PullRequestCommentEvent commentAddedEvent = (PullRequestCommentEvent) pullRequestEvent;

            ret.put("comment", commentAddedEvent.getComment().getText());
            ret.put("comment_author", commentAddedEvent.getComment().getAuthor().getDisplayName());
        }

        if (pullRequestEvent instanceof PullRequestMergedEvent) {
            PullRequestMergedEvent mergedEvent = (PullRequestMergedEvent) pullRequestEvent;

            ret.put("merge_isremotely", Boolean.toString(mergedEvent.isMergedRemotely()));
            MinimalCommit commit = mergedEvent.getCommit();
            if (commit != null) {
                ret.put("merge_commit", commit.getId());
            }
        }

        StringBuilder sBuilder = new StringBuilder();
        for (String key : ret.keySet()) {
            sBuilder.append(key);
            sBuilder.append(',');
        }
        ret.put("all_keys", sBuilder.toString());
        return ret;
    }

}
