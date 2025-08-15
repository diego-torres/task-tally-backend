package io.redhat.na.ssp.tasktally.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CredentialRequest {
    @NotBlank
    public String name;
    @NotBlank
    public String provider;
    @NotBlank
    public String scope;
    @NotBlank
    public String secretRef;
}
