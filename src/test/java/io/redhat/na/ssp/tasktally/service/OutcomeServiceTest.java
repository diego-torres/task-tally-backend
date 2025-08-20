package io.redhat.na.ssp.tasktally.service;

import io.redhat.na.ssp.tasktally.model.Outcome;
import io.redhat.na.ssp.tasktally.model.Template;
import io.redhat.na.ssp.tasktally.model.UserPreferences;
import io.redhat.na.ssp.tasktally.repo.TemplateRepository;
import io.redhat.na.ssp.tasktally.repo.UserPreferencesRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OutcomeServiceTest {

  @Inject
  OutcomeService outcomeService;

  @InjectMock
  GitYamlService gitYamlService;

  @InjectMock
  TemplateRepository templateRepo;

  @InjectMock
  UserPreferencesRepository userRepo;

  private UserPreferences userPrefs;
  private Template template;
  private Outcome outcome;

  @BeforeEach
  void setUp() {
    userPrefs = new UserPreferences();
    userPrefs.id = 1L;
    userPrefs.userId = "test-user";

    template = new Template();
    template.id = 1L;
    template.name = "Test Template";
    template.userPreferences = userPrefs;
    template.repositoryUrl = "git@github.com:test/repo.git";
    template.yamlPath = "outcomes.yml";

    outcome = new Outcome("Discovery", "Infrastructure", "OpenShift", "Production", "INFRA",
        "Infrastructure assessment completed", "Focus on security and scalability");
  }

  @Test
  void testListByTemplate_Success() {
    // Given
    when(userRepo.findByUserId("test-user")).thenReturn(Optional.of(userPrefs));
    when(templateRepo.findByUserAndId(1L, 1L)).thenReturn(Optional.of(template));
    when(gitYamlService.readOutcomes(template, null)).thenReturn(new ArrayList<>(Arrays.asList(outcome)));

    // When
    List<Outcome> result = outcomeService.listByTemplate("test-user", 1L);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(outcome, result.get(0));
    verify(gitYamlService).readOutcomes(template, null);
  }

  @Test
  void testListByTemplate_UserNotFound() {
    // Given
    when(userRepo.findByUserId("test-user")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(NotFoundException.class, () -> {
      outcomeService.listByTemplate("test-user", 1L);
    });
  }

  @Test
  void testListByTemplate_TemplateNotFound() {
    // Given
    when(userRepo.findByUserId("test-user")).thenReturn(Optional.of(userPrefs));
    when(templateRepo.findByUserAndId(1L, 1L)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(NotFoundException.class, () -> {
      outcomeService.listByTemplate("test-user", 1L);
    });
  }

  @Test
  void testCreate_Success() {
    // Given
    Outcome newOutcome = new Outcome("Implementation", "Security", "RHEL", "Development", "SEC",
        "Security baseline established", "Follow CIS benchmarks");

    when(userRepo.findByUserId("test-user")).thenReturn(Optional.of(userPrefs));
    when(templateRepo.findByUserAndId(1L, 1L)).thenReturn(Optional.of(template));
    when(gitYamlService.readOutcomes(template, null)).thenReturn(new ArrayList<>(Arrays.asList(outcome)));

    // When
    Outcome result = outcomeService.create("test-user", 1L, newOutcome);

    // Then
    assertNotNull(result);
    assertEquals("Implementation", result.phase.name);
    assertEquals("Security", result.phase.track);
    verify(gitYamlService).readOutcomes(template, null);
    verify(gitYamlService).writeOutcomes(template, Arrays.asList(outcome, newOutcome), null);
  }

  @Test
  void testUpdate_Success() {
    // Given
    Outcome updatedOutcome = new Outcome("Updated Phase", "Updated Track", "Updated Product", "Updated Environment",
        "UPD", "Updated outcome text", "Updated scoping notes");

    when(userRepo.findByUserId("test-user")).thenReturn(Optional.of(userPrefs));
    when(templateRepo.findByUserAndId(1L, 1L)).thenReturn(Optional.of(template));
    when(gitYamlService.readOutcomes(template, null)).thenReturn(new ArrayList<>(Arrays.asList(outcome)));

    // When
    Outcome result = outcomeService.update("test-user", 1L, 0L, updatedOutcome);

    // Then
    assertNotNull(result);
    assertEquals("Updated Phase", result.phase.name);
    assertEquals("Updated Track", result.phase.track);
    assertEquals("Updated Product", result.phase.product);
    assertEquals("Updated Environment", result.phase.environment);
    assertEquals("UPD", result.prefix);
    assertEquals("Updated outcome text", result.description);
    assertEquals("Updated scoping notes", result.notes);
    verify(gitYamlService).readOutcomes(template, null);
    verify(gitYamlService).writeOutcomes(template, Arrays.asList(updatedOutcome), null);
  }

  @Test
  void testUpdate_OutcomeNotFound() {
    // Given
    Outcome updatedOutcome = new Outcome("Updated Phase", "Updated Track", "Updated Product", "Updated Environment",
        "UPD", "Updated outcome text", "Updated scoping notes");
    when(userRepo.findByUserId("test-user")).thenReturn(Optional.of(userPrefs));
    when(templateRepo.findByUserAndId(1L, 1L)).thenReturn(Optional.of(template));
    when(gitYamlService.readOutcomes(template, null)).thenReturn(new ArrayList<>(Arrays.asList(outcome)));

    // When & Then
    assertThrows(NotFoundException.class, () -> {
      outcomeService.update("test-user", 1L, 5L, updatedOutcome); // Index out of bounds
    });
  }

  @Test
  void testDelete_Success() {
    // Given
    when(userRepo.findByUserId("test-user")).thenReturn(Optional.of(userPrefs));
    when(templateRepo.findByUserAndId(1L, 1L)).thenReturn(Optional.of(template));
    when(gitYamlService.readOutcomes(template, null)).thenReturn(new ArrayList<>(Arrays.asList(outcome)));

    // When
    outcomeService.delete("test-user", 1L, 0L);

    // Then
    verify(gitYamlService).readOutcomes(template, null);
    verify(gitYamlService).writeOutcomes(template, Arrays.asList(), null);
  }

  @Test
  void testDeleteAllByTemplate_Success() {
    // Given
    when(userRepo.findByUserId("test-user")).thenReturn(Optional.of(userPrefs));
    when(templateRepo.findByUserAndId(1L, 1L)).thenReturn(Optional.of(template));

    // When
    outcomeService.deleteAllByTemplate("test-user", 1L);

    // Then
    verify(gitYamlService).writeOutcomes(template, Arrays.asList(), null);
  }
}
