package com.maciejkucia.atlasbbplugin.repopullrequest.servlet;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.project.ProjectService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.maciejkucia.atlasbbplugin.repopullrequest.hook.PullRequestHookLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class WeblogServlet extends HttpServlet {
    private final PermissionService         permissionService;
    private final AuthenticationContext     authenticationContext;
    private final RepositoryService         repoService;
    private final ProjectService            projectService;
    private final PullRequestHookLogger logger;

    @Autowired
    public WeblogServlet(@ComponentImport PermissionService     permissionService,
                         @ComponentImport AuthenticationContext authenticationContext,
                         @ComponentImport RepositoryService     repoService,
                         @ComponentImport ProjectService        projectService) {
        this.permissionService     = permissionService;
        this.authenticationContext = authenticationContext;
        this.repoService           = repoService;
        this.projectService        = projectService;
        this.logger                = PullRequestHookLogger.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        Repository      repo            = extractRepository(req.getPathInfo());
        Project         project         = extractProject(req.getPathInfo());
        ApplicationUser user            = authenticationContext.getCurrentUser();
        boolean         hasAccessToRepo = (repo != null) &&
                                          permissionService.hasRepositoryPermission(user, repo, Permission.REPO_ADMIN);

        if ((user != null) && (repo == null)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if ((user == null) || (!hasAccessToRepo)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        ProduceSuccess(resp, repo, project);
    }

    private void ProduceSuccess(HttpServletResponse resp, Repository repo, Project project) throws IOException {
        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.write("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">");
        writer.write("<title>Repository Pull Request Plugin Log</title>");
        writer.write("</head><body style='margin:15px;font-family:monospace;'>");
        writer.write(String.format("<h1>Log for repo '%1s' in project '%2s' </h1>", repo.getName(), project.getName()));
        for (String line : logger.getLogs(repo)) {
            writer.write("<p>" +line + "</p>");
        }
        writer.write("</body></html>");
    }

    private Repository extractRepository(String pathInfo) {
        String[] elements = pathInfo.split("/");
        if (elements.length != 3) {
            return null;
        }
        String repoSlug   = elements[1];
        String projectKey = elements[2];
        return repoService.getBySlug(projectKey, repoSlug);
    }

    private Project extractProject(String pathInfo) {
        String[] elements = pathInfo.split("/");
        if (elements.length != 3) {
            return null;
        }
        String projectKey = elements[2];
        return projectService.getByKey(projectKey);
    }

}