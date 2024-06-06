package org.apized.core.security.annotation;

import org.apized.core.model.Action;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
  /**
   * Actions the permission is for.
   *
   * @return The action this permission is for
   */
  Action action();

  /**
   * Fields the user gets access to for this action
   * @return the list of fields.
   */
  String[] fields() default {};
}
