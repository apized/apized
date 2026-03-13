package org.apized.spring.server.serde;

import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.context.ApizedContext;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PageContentBehaviour implements BehaviourHandler<Model> {
  public PageContentBehaviour(BehaviourManager manager) {
    manager.registerBehaviour(Model.class, Layer.CONTROLLER, List.of(When.BEFORE), List.of(Action.LIST), -Integer.MAX_VALUE, this);
  }

  @Override
  public void preList(Execution<Model> execution) {
    if (!ApizedContext.getRequest().getFields().containsKey("content")) {
      ApizedContext.getRequest().setFields(Map.of("*", Map.of(), "content", ApizedContext.getRequest().getFields().isEmpty() ? Map.of("*", Map.of()) : ApizedContext.getRequest().getFields()));
      ApizedContext.getRequest().setSearch(Map.of("content", ApizedContext.getRequest().getSearch()));
      ApizedContext.getRequest().setSort(Map.of("content", ApizedContext.getRequest().getSort()));
    }
  }
}
