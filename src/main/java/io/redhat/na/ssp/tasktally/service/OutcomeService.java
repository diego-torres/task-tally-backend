package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.Outcome;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.repo.TemplateRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.redhat.na.ssp.tasktally.repo.CredentialRefRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.ArrayList;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OutcomeService {
  private static final Logger LOG = Logger.getLogger(OutcomeService.class);

  @Inject
  GitYamlService gitYamlService;

  @Inject
  TemplateRepository templateRepo;

  @Inject
  UserPreferencesRepository userRepo;

  @Inject
  CredentialRefRepository credentialRepo;

  @Transactional
  public List<Outcome> listByTemplate(String userId, Long templateId) {
    LOG.debugf("Listing outcomes for template %d for user %s", templateId, userId);

    // Verify template belongs to user
    Template template = verifyTemplateOwnership(userId, templateId);

    // Get credential for Git operations
    CredentialRef credential = getCredentialForTemplate(userId, template);

    try {
      List<Outcome> outcomes = gitYamlService.readOutcomes(template, credential);
      LOG.infof("Found %d outcomes for template %d for user %s", outcomes.size(), templateId, userId);
      return outcomes;
    } catch (RuntimeException e) {
      // Handle SSH key resolution failures gracefully
      if (e.getMessage() != null && e.getMessage().contains("Secret not found:")) {
        LOG.warnf("SSH key secret not found for template %d, returning empty list", templateId);
        return new ArrayList<>();
      }
      throw e;
    }
  }

  @Transactional
  public Outcome create(String userId, Long templateId, Outcome outcome) {
    LOG.debugf("Creating outcome for template %d for user %s", templateId, userId);

    // Verify template belongs to user
    Template template = verifyTemplateOwnership(userId, templateId);

    // Get credential for Git operations
    CredentialRef credential = getCredentialForTemplate(userId, template);

    try {
      // Read existing outcomes
      List<Outcome> outcomes = gitYamlService.readOutcomes(template, credential);

      // Add new outcome
      outcomes.add(outcome);

      // Write back to Git
      gitYamlService.writeOutcomes(template, outcomes, credential);

      LOG.infof("Created outcome for template %d for user %s", templateId, userId);
      return outcome;
    } catch (RuntimeException e) {
      // Handle SSH key resolution failures
      if (e.getMessage() != null && e.getMessage().contains("Secret not found:")) {
        LOG.errorf("SSH key secret not found for template %d, cannot create outcome", templateId);
        throw new RuntimeException("SSH key secret not found. Cannot create outcome", e);
      }
      throw e;
    }
  }

  @Transactional
  public Outcome update(String userId, Long templateId, Long outcomeId, Outcome incoming) {
    LOG.debugf("Updating outcome %d for template %d for user %s", outcomeId, templateId, userId);

    // Verify template belongs to user
    Template template = verifyTemplateOwnership(userId, templateId);

    // Get credential for Git operations
    CredentialRef credential = getCredentialForTemplate(userId, template);

    try {
      // Read existing outcomes
      List<Outcome> outcomes = gitYamlService.readOutcomes(template, credential);

      // Find and update the outcome
      if (outcomeId >= 0 && outcomeId < outcomes.size()) {
        outcomes.set(outcomeId.intValue(), incoming);

        // Write back to Git
        gitYamlService.writeOutcomes(template, outcomes, credential);

        LOG.infof("Updated outcome %d for template %d for user %s", outcomeId, templateId, userId);
        return incoming;
      } else {
        LOG.errorf("Outcome %d for template %d for user %s not found", outcomeId, templateId, userId);
        throw new NotFoundException();
      }
    } catch (RuntimeException e) {
      // Handle SSH key resolution failures
      if (e.getMessage() != null && e.getMessage().contains("Secret not found:")) {
        LOG.errorf("SSH key secret not found for template %d, cannot update outcome", templateId);
        throw new RuntimeException("SSH key secret not found. Cannot update outcome", e);
      }
      throw e;
    }
  }

  @Transactional
  public void delete(String userId, Long templateId, Long outcomeId) {
    LOG.debugf("Deleting outcome %d for template %d for user %s", outcomeId, templateId, userId);

    // Verify template belongs to user
    Template template = verifyTemplateOwnership(userId, templateId);

    // Get credential for Git operations
    CredentialRef credential = getCredentialForTemplate(userId, template);

    try {
      // Read existing outcomes
      List<Outcome> outcomes = gitYamlService.readOutcomes(template, credential);

      // Remove the outcome
      if (outcomeId >= 0 && outcomeId < outcomes.size()) {
        outcomes.remove(outcomeId.intValue());

        // Write back to Git
        gitYamlService.writeOutcomes(template, outcomes, credential);

        LOG.infof("Deleted outcome %d for template %d for user %s", outcomeId, templateId, userId);
      } else {
        LOG.errorf("Outcome %d for template %d for user %s not found", outcomeId, templateId, userId);
        throw new NotFoundException();
      }
    } catch (RuntimeException e) {
      // Handle SSH key resolution failures
      if (e.getMessage() != null && e.getMessage().contains("Secret not found:")) {
        LOG.errorf("SSH key secret not found for template %d, cannot delete outcome", templateId);
        throw new RuntimeException("SSH key secret not found. Cannot delete outcome", e);
      }
      throw e;
    }
  }

  @Transactional
  public void deleteAllByTemplate(String userId, Long templateId) {
    LOG.debugf("Deleting all outcomes for template %d for user %s", templateId, userId);

    // Verify template belongs to user
    Template template = verifyTemplateOwnership(userId, templateId);

    // Get credential for Git operations
    CredentialRef credential = getCredentialForTemplate(userId, template);

    try {
      // Write empty outcomes list to Git
      gitYamlService.writeOutcomes(template, new ArrayList<>(), credential);

      LOG.infof("Deleted all outcomes for template %d for user %s", templateId, userId);
    } catch (RuntimeException e) {
      // Handle SSH key resolution failures
      if (e.getMessage() != null && e.getMessage().contains("Secret not found:")) {
        LOG.errorf("SSH key secret not found for template %d, cannot delete all outcomes", templateId);
        throw new RuntimeException("SSH key secret not found. Cannot delete all outcomes", e);
      }
      throw e;
    }
  }

  private Template verifyTemplateOwnership(String userId, Long templateId) {
    var userPrefs = userRepo.findByUserId(userId).orElseThrow(() -> {
      LOG.errorf("User %s not found", userId);
      return new NotFoundException();
    });

    return templateRepo.findByUserAndId(userPrefs.id, templateId).orElseThrow(() -> {
      LOG.errorf("Template %d for user %s not found", templateId, userId);
      return new NotFoundException();
    });
  }

  private CredentialRef getCredentialForTemplate(String userId, Template template) {
    if (template.sshKeyName != null && !template.sshKeyName.trim().isEmpty()) {
      var userPrefs = userRepo.findByUserId(userId).orElseThrow(() -> {
        LOG.errorf("User %s not found", userId);
        return new NotFoundException();
      });

      return credentialRepo.findByUserAndName(userPrefs.id, template.sshKeyName).orElseThrow(() -> {
        LOG.errorf("SSH credential %s for user %s not found", template.sshKeyName, userId);
        return new NotFoundException();
      });
    }

    // Return null if no SSH key is configured (will use default SSH config)
    return null;
  }
}
