package org.apized.core.context;

import io.micronaut.core.beans.BeanProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apized.core.model.Model;

@AllArgsConstructor
@Data
public class SerdeStackEntry {
  Model value;
  BeanProperty property;
}
