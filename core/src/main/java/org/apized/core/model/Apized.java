/*
 * Copyright 2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apized.core.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Apized {
  /**
   * The name of the model in CamelCase.
   */
  String value() default "";

  /**
   * Set of components to generate. Generated Repositories are assumed to be for DB access and will not trigger behaviours.
   */
  Layer[] layers() default {Layer.CONTROLLER, Layer.SERVICE, Layer.REPOSITORY};

  /**
   * Indicates which operations are supported for this model.
   * This will guide which methods are generated on the Controller & Service.
   */
  Action[] operations() default {Action.LIST, Action.GET, Action.CREATE, Action.UPDATE, Action.DELETE};

  Class<? extends Model>[] scope() default {};

  boolean audit() default true;

  boolean event() default true;
}
