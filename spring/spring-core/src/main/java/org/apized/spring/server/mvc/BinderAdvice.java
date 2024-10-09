package org.apized.spring.server.mvc;

import org.apized.core.search.SearchHelper;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.List;

@ControllerAdvice
public class BinderAdvice {
  @InitBinder("search")
  public void initTermsBinder(WebDataBinder binder) {
    binder.registerCustomEditor(List.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        List<SearchTerm> searchTerms = Arrays.stream(text.split(",")).map(SearchHelper::convertTerm).toList();
        setValue(text.isBlank() ? List.of() : searchTerms);
      }
    });
  }

  @InitBinder("sort")
  public void initSortBinder(WebDataBinder binder) {
    binder.registerCustomEditor(List.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        List<SortTerm> sortTerms = Arrays.stream(text.split(",")).map(SearchHelper::convertSort).toList();
        setValue(text.isBlank() ? List.of() : sortTerms);
      }
    });
  }
}

