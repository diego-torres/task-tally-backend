package io.redhat.na.ssp.tasktally.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class PreferencesDto {
    @NotNull
    public Map<String, Object> ui = new HashMap<>();
    public String defaultGitProvider;
    public Integer version;
}
