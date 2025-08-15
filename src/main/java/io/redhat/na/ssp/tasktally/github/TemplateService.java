package io.redhat.na.ssp.tasktally.github;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.service.PreferencesService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

@ApplicationScoped
public class TemplateService {

    @Inject
    PreferencesService preferencesService;

    @Inject
    GitCredentialProvider credentialProvider;

    @Inject
    GitHubClient gitHubClient;

    private final Yaml yaml = new Yaml();

    public List<ProjectTemplate> pullTemplates(String userId, TemplatePullRequest req) {
        CredentialRef cred = preferencesService.findCredential(userId, req.credentialName);
        String token = credentialProvider.provideToken(cred);
        Map<String, String> files = gitHubClient.getYamlFiles(req.owner, req.repo, req.path, req.branch, token);
        List<ProjectTemplate> templates = new ArrayList<>();
        for (String content : files.values()) {
            templates.add(yaml.loadAs(content, ProjectTemplate.class));
        }
        return templates;
    }
}
