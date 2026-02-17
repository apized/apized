package org.apized.core.mvc;

import lombok.Data;
import org.apized.core.model.Model;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class ExecutionPlan {
  public interface CreateExecutor {
    Model execute(Model model);
  }

  private List<ExecutionStep> executionSteps = new ArrayList<>();
  private Set<Model> beforeProcessedModels = new HashSet<>();
  private Set<Model> afterProcessedModels = new HashSet<>();
  private Model response;

  public boolean isProcessed(Model model, boolean isBefore) {
    if (isBefore) {
      return beforeProcessedModels.stream().anyMatch(o -> o == model);
    } else {
      return afterProcessedModels.stream().anyMatch(o -> o == model);
    }
  }

  public void markProcessed(Model model, boolean isBefore) {
    if (isBefore) {
      beforeProcessedModels.add(model);
    } else {
      afterProcessedModels.add(model);
    }
  }

  public ExecutionPlan main(AbstractModelService<Model> modelService, Model model, String propertyName, CreateExecutor action) {
    executionSteps.add(
      new ExecutionStep(
        ExecutionStep.Kind.MAIN,
        modelService,
        List.of(model),
        propertyName,
        null,
        (step) -> setResponse(action.execute(step.getModels().getFirst()))
      )
    );
    return this;
  }

  public ExecutionPlan batchCreate(AbstractModelService<Model> modelService, List<Model> models, String propertyName) {
    executionSteps.add(
      new ExecutionStep(
        ExecutionStep.Kind.CREATE,
        modelService,
        models,
        propertyName,
        null,
        (step) -> step.getModelService().batchCreate(models)
      )
    );
    return this;
  }

  public ExecutionPlan batchUpdate(AbstractModelService<Model> modelService, List<Model> models, String propertyName) {
    executionSteps.add(
      new ExecutionStep(
        ExecutionStep.Kind.UPDATE,
        modelService,
        models,
        propertyName,
        null,
        (step) -> step.getModelService().batchUpdate(models)
      )
    );
    return this;
  }

  public ExecutionPlan batchDelete(AbstractModelService<Model> modelService, List<Model> models, String propertyName) {
    executionSteps.add(
      new ExecutionStep(
        ExecutionStep.Kind.DELETE,
        modelService,
        models,
        propertyName,
        null,
        (step) -> step.getModelService().batchDelete(models)
      )
    );
    return this;
  }

  public ExecutionPlan addManyToMany(AbstractModelService<Model> modelService, Model model, String propertyName, Model other) {
    executionSteps.add(
      new ExecutionStep(
        ExecutionStep.Kind.ADD_MANY_TO_MANY,
        modelService,
        List.of(model),
        propertyName,
        other,
        (step) -> step.getModelService().getRepository().add(step.getPropertyName(), step.getModels().getFirst().getId(), step.getOther().getId())
      )
    );
    return this;
  }

  public ExecutionPlan removeManyToMany(AbstractModelService<Model> modelService, Model model, String propertyName, Model other) {
    executionSteps.add(
      new ExecutionStep(
        ExecutionStep.Kind.REMOVE_MANY_TO_MANY,
        modelService,
        List.of(model),
        propertyName,
        other,
        (step) -> step.getModelService().getRepository().remove(step.getPropertyName(), step.getModels().getFirst().getId(), step.getOther().getId())
      )
    );
    return this;
  }

  public ExecutionPlan execute() {
    executionSteps.forEach(ExecutionStep::execute);
    return this;
  }

  @Override
  public String toString() {
    return "\n{" + executionSteps.stream().map(ExecutionStep::toString).collect(Collectors.joining("\n")) + "}\n";
  }
}
