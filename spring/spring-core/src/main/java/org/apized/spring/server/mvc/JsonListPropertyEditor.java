package org.apized.spring.server.mvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.beans.PropertyEditorSupport;

public class JsonListPropertyEditor extends PropertyEditorSupport {
  ObjectMapper objectMapper;
  TypeReference type;

  public JsonListPropertyEditor(ObjectMapper objectMapper, TypeReference type) {
    this.objectMapper = objectMapper;
    this.type = type;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (text==null) {
      setValue(null);
    } else {
      try {
        Object parsed = objectMapper.readValue(text, type);
        setValue(parsed);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }
}
