package org.apized.spring.server.mvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.util.List;

@ControllerAdvice
public class BinderAdvice {
  private final ObjectMapper mapper;

  public BinderAdvice(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @InitBinder("search")
  public void initTermsBinder(WebDataBinder binder) {
    binder.registerCustomEditor(List.class, new JsonListPropertyEditor(mapper, new TypeReference<List<SearchTerm>>() {
    }));
  }

  @InitBinder("sort")
  public void initSortBinder(WebDataBinder binder) {
    binder.registerCustomEditor(List.class, new JsonListPropertyEditor(mapper, new TypeReference<List<SortTerm>>() {
    }));
  }
}

