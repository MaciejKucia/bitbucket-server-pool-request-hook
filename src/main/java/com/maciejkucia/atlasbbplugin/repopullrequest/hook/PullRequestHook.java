package com.maciejkucia.atlasbbplugin.repopullrequest.hook;

import com.atlassian.bitbucket.event.pull.*;
import com.atlassian.bitbucket.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.RepositorySettingsValidator;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.maciejkucia.atlasbbplugin.repopullrequest.Catalog;
import com.maciejkucia.atlasbbplugin.repopullrequest.emiter.RestfulNotificationEmitter;
import com.maciejkucia.atlasbbplugin.repopullrequest.emiter.RestfulNotificationEmitterSettings;
import com.maciejkucia.atlasbbplugin.repopullrequest.emiter.ValueMapGenerator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;


@Component
public class PullRequestHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private final RestfulNotificationEmitter emitter     = new RestfulNotificationEmitter();
    private final ValueMapGenerator          vmGenerator = new ValueMapGenerator();
    private final RepositoryHookService repositoryHookService;
    private PullRequestHookLogger logger = null;

    @Autowired
    public PullRequestHook(@ComponentImport RepositoryHookService repositoryHookService) {
        this.repositoryHookService = repositoryHookService;
        this.logger                = PullRequestHookLogger.getInstance();
    }

    // Events that are too general
    //@EventListener public void onEvent(PullRequestActivityEvent e)            { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestCommentActivityEvent e)     { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestCommentEvent e)             { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestEvent e)                    { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestMergeActivityEvent e)       { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestRescopeActivityEvent e)     { handleEvent(e); }

    @EventListener public void onEvent(PullRequestCommentAddedEvent e)        { handleEvent(e); }
    @EventListener public void onEvent(PullRequestCommentDeletedEvent e)      { handleEvent(e); }
    @EventListener public void onEvent(PullRequestCommentEditedEvent e)       { handleEvent(e); }
    @EventListener public void onEvent(PullRequestCommentRepliedEvent e)      { handleEvent(e); }
    @EventListener public void onEvent(PullRequestDeclinedEvent e)            { handleEvent(e); }
    @EventListener public void onEvent(PullRequestMergedEvent e)              { handleEvent(e); }
    @EventListener public void onEvent(PullRequestOpenedEvent e)              { handleEvent(e); }
    @EventListener public void onEvent(PullRequestOpenRequestedEvent e)       { handleEvent(e); }
    @EventListener public void onEvent(PullRequestParticipantsUpdatedEvent e) { handleEvent(e); }
    @EventListener public void onEvent(PullRequestReopenedEvent e)            { handleEvent(e); }
    @EventListener public void onEvent(PullRequestRescopedEvent e)            { handleEvent(e); }
    @EventListener public void onEvent(PullRequestUpdatedEvent e)             { handleEvent(e); }

    // Deprecated events
    //public void onEvent(PullRequestRolesUpdatedEvent e) { handleEvent(e); }
    //public void onEvent(PullRequestUnapprovedEvent e)   { handleEvent(e); }
    //public void onEvent(PullRequestApprovalEvent e)     { handleEvent(e); }
    //public void onEvent(PullRequestApprovedEvent e)     { handleEvent(e); }

    private void handleEvent(final PullRequestEvent pullRequestEvent) {
        Repository repo = pullRequestEvent.getPullRequest().getToRef().getRepository();

        if (isHookEnabled(repo)) {
            Settings repoSettings    = getSettings(repo);
            RestfulNotificationEmitterSettings emitterSettings = new RestfulNotificationEmitterSettings(repoSettings);
            Boolean  isEventEnabled  = emitterSettings.isPullRequestEventEnabled(pullRequestEvent.getClass());

            if (isEventEnabled) {
                this.emitter.EmitAsync(repo, emitterSettings, vmGenerator.extractIntoMap(pullRequestEvent));
            }
            else {
                logger.putLog(repo, "Ignoring {0} for #{1}", pullRequestEvent.getAction().toString(),
                        pullRequestEvent.getPullRequest().getId());
            }
        }
    }

    private Boolean isHookEnabled(final Repository repo) {
        RepositoryHook hook = repositoryHookService.getByKey(repo, Catalog.REPO_HOOK_KEY);
        return (hook != null) && hook.isEnabled() && hook.isConfigured();
    }

    private Settings getSettings(final Repository repo) {
        return repositoryHookService.getSettings(repo, Catalog.REPO_HOOK_KEY);
    }

    @Override
    public void postReceive(@Nonnull RepositoryHookContext repositoryHookContext, @Nonnull Collection<RefChange> collection) {
        // We don't really capture any commit events in this plugin
    }

    @Override
    public void validate(@Nonnull Settings                  settings,
                         @Nonnull SettingsValidationErrors  settingsValidationErrors,
                         @Nonnull Repository                repository) {
        validateUrl(settings, settingsValidationErrors);
        validateHeaders(settings, settingsValidationErrors);
    }

    private void validateUrl(@Nonnull Settings settings,
                             @Nonnull SettingsValidationErrors settingsValidationErrors) {
        try {
            String url = settings.getString("url");

            if (url == null) {
                throw new MalformedURLException("URL must be set");
            }

            URL newURL = new URL(url);

            if (url.contains(" ")) {
                throw new MalformedURLException("Spaces in the URL are not allowed.");
            }

            if (!newURL.getProtocol().equalsIgnoreCase("http")) {
                throw new MalformedURLException("Only http protocol is supported.");
            }
        }
        catch (MalformedURLException urlException) {
            settingsValidationErrors.addFieldError("url", "Url is incorrect! " + urlException.getMessage());
        }
    }

    private void validateHeaders(@Nonnull Settings settings,
                                 @Nonnull SettingsValidationErrors settingsValidationErrors) {
        String headers = settings.getString("headers");

        if (headers.isEmpty()) {
            return;
        }

        for (String line : headers.split("\n")) {
            if (StringUtils.countOccurrencesOf(line, ":") != 1) {
                settingsValidationErrors.addFieldError("headers", "Use single : per line!");
                return;
            }
        }
    }

}