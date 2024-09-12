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

import io.micronaut.serde.annotation.Serdeable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Serdeable
public @interface Apized {
  /**
   * Set of components to generate. Generated Repositories are assumed to be for DB access and will not trigger behaviours.
   */
  Layer[] layers() default {Layer.CONTROLLER, Layer.SERVICE, Layer.REPOSITORY};

  /**
   * Indicates which operations are supported for this model.
   * This will guide which methods are generated on the Controller & Service.
   */
  Action[] operations() default {Action.LIST, Action.GET, Action.CREATE, Action.UPDATE, Action.DELETE};

  /**
   * List of Service and Repository extensions for this model.
   * @return The classes that are Service or Repository extensions for this model.
   */
  Class<?>[] extensions() default {};

  /**
   * The scope of this model.
   * @return The parent scope.
   */
  Class<? extends Model>[] scope() default {};

  /**
   * Should this model generate and store an audit trail
   * @return If this model generate and store an audit trail
   */
  boolean audit() default true;

  /**
   * Should this model generate and publish events
   * @return If this model should generate and publish events
   */
  boolean event() default true;

  /**
   * Maximum allowed pageSize for List operations.
   * @return The max number of elements to be allowed to be retrieved in a single list request.
   */
  int maxPageSize() default 50;

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Extension {
    /**
     * The layer this is extending.
     * todo do controllers actually need this or can I simply declare a new controller
     */
    Layer layer();

    /**
     * For Repository extensions (an interface) the exclusions list the methods that we shouldn't create a Service layer
     * "proxy" for.
     * For Service extensions (a Singleton) the exclusions list the methods that shouldn't be present in the service itself.
     */
    String[] exclude() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Action {
      org.apized.core.model.Action value();
    }
  }
}
