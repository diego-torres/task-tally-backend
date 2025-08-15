package io.redhat.na.ssp.tasktally.github;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class TemplatePushRequest {
    @NotBlank
    public String owner;
    @NotBlank
    public String repo;
    @NotBlank
    public String path;
    @NotBlank
    public String branch;
    @NotBlank
    public String message;
    @NotBlank
    public String credentialName;
    @NotNull
    @Valid
    public List<TemplateFile> files = new ArrayList<>();

    public static class TemplateFile {
        @NotBlank
        public String name;
        @NotBlank
        public String yaml;
    }
}
