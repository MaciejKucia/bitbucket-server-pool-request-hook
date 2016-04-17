package com.maciejkucia.repopullrequest.hook;

import com.atlassian.bitbucket.event.hook.RepositoryHookSettingsChangedEvent;
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
import com.maciejkucia.repopullrequest.emiter.RestfulNotificationEmitter;
import com.maciejkucia.repopullrequest.emiter.RestfulNotificationEmitterSettings;
import com.maciejkucia.repopullrequest.emiter.ValueMapGenerator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collection;


@Component
public class PullRequestHook implements DisposableBean, AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private static final RestfulNotificationEmitter emitter = new RestfulNotificationEmitter();
    private static final ValueMapGenerator vmGenerator = new ValueMapGenerator();
    private static final String REPO_HOOK_KEY = "com.maciejkucia.atlasbbplugin.repopullrequest:pr-hook";

    private EventPublisher eventPublisher;
    private RepositoryHookService repositoryHookService;

    @Autowired
    public PullRequestHook(@ComponentImport EventPublisher eventPublisher,
                           @ComponentImport RepositoryHookService repositoryHookService) {
        this.eventPublisher = eventPublisher;
        this.repositoryHookService = repositoryHookService;
        eventPublisher.register(this);
    }

    //@EventListener public void onEvent(PullRequestActivityEvent e)            { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestCommentActivityEvent e)     { handleEvent(e); }
    @EventListener public void onEvent(PullRequestCommentAddedEvent e)        { handleEvent(e); }
    @EventListener public void onEvent(PullRequestCommentDeletedEvent e)      { handleEvent(e); }
    @EventListener public void onEvent(PullRequestCommentEditedEvent e)       { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestCommentEvent e)             { handleEvent(e); }
    @EventListener public void onEvent(PullRequestCommentRepliedEvent e)      { handleEvent(e); }
    @EventListener public void onEvent(PullRequestDeclinedEvent e)            { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestEvent e)                    { handleEvent(e); }
    //@EventListener public void onEvent(PullRequestMergeActivityEvent e)       { handleEvent(e); }
    @EventListener public void onEvent(PullRequestMergedEvent e)              { handleEvent(e); }
    @EventListener public void onEvent(PullRequestOpenedEvent e)              { handleEvent(e); }
    @EventListener public void onEvent(PullRequestOpenRequestedEvent e)       { handleEvent(e); }
    @EventListener public void onEvent(PullRequestParticipantsUpdatedEvent e) { handleEvent(e); }
    @EventListener public void onEvent(PullRequestReopenedEvent e)            { handleEvent(e); }
    @EventListener public void onEvent(PullRequestRescopeActivityEvent e)     { handleEvent(e); }
    @EventListener public void onEvent(PullRequestRescopedEvent e)            { handleEvent(e); }
    @EventListener public void onEvent(PullRequestUpdatedEvent e)             { handleEvent(e); }

//    DEPRECATED
//    public void onEvent(PullRequestRolesUpdatedEvent e) { handleEvent(e); }
//    public void onEvent(PullRequestUnapprovedEvent e)   { handleEvent(e); }
//    public void onEvent(PullRequestApprovalEvent e)     { handleEvent(e); }
//    public void onEvent(PullRequestApprovedEvent e)     { handleEvent(e); }

    private void handleEvent(final PullRequestEvent pullRequestEvent) {
        Repository repo = pullRequestEvent.getPullRequest().getToRef().getRepository();

        if (isHookEnabled(repo)) {
            Settings settings = loadSettings(repo);
            RestfulNotificationEmitterSettings emitterSettings = new RestfulNotificationEmitterSettings(settings);

            Boolean isEventEnabled = emitterSettings.isPullRequestEventEnabled(pullRequestEvent.getClass());
            if (isEventEnabled) {
                this.emitter.EmitAsync(emitterSettings, vmGenerator.getMap(pullRequestEvent));
            }
        }
    }

    private Boolean isHookEnabled(final Repository repo) {
        RepositoryHook hook = repositoryHookService.getByKey(repo, REPO_HOOK_KEY);
        return hook.isEnabled() && hook.isConfigured();
    }

    private Settings loadSettings(final Repository repo) {
        return repositoryHookService.getSettings(repo, REPO_HOOK_KEY);
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void postReceive(RepositoryHookContext repositoryHookContext, Collection<RefChange> collection) {
        // We don't really capture any commit events in this plugin
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors settingsValidationErrors, Repository repository) {
        // validation is not implemented
    }

    @EventListener
    public void onRepositoryHookSettingsChangedEvent(RepositoryHookSettingsChangedEvent event) {
//        if (event.getRepositoryHookKey() != "MYKEY") {
//            return;
//        }
    }

}