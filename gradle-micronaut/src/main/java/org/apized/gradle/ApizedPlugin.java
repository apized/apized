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

package org.apized.gradle;

import io.micronaut.gradle.MicronautApplicationPlugin;
import io.micronaut.gradle.MicronautExtension;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor;
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.layout.MavenRepositoryLayout;
import org.gradle.api.plugins.JavaPlugin;

import java.io.*;
import java.util.Properties;

/**
 *
 */
public class ApizedPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    try {
      Properties myProperties = new Properties();
      Properties apizedProperties = new Properties();
      myProperties.load(getClass().getClassLoader().getResourceAsStream("gradle.properties"));
      String apizedVersion = myProperties.getProperty("version");
      String micronautVersion = myProperties.getProperty("micronautVersion");

      project.getExtensions().add("micronautVersion", micronautVersion);
      project.getPlugins().apply("groovy");

      project.getPlugins().apply(MicronautApplicationPlugin.class);
      project.getPlugins().withType(JavaPlugin.class, plugin -> {
        project.getDependencies().add("annotationProcessor", "org.projectlombok:lombok");
        project.getDependencies().add("compileOnly", "org.projectlombok:lombok");
      });

      project.getRepositories().maven(r -> {
        r.setName("Apized GitHub Repository");
        r.setUrl("https://maven.pkg.github.com/apized/apized");
      });

      MicronautExtension micronaut = project.getExtensions().getByType(MicronautExtension.class);
      micronaut.runtime("netty");
      micronaut.testRuntime("junit5");

      project.getDependencies().add("annotationProcessor", "io.micronaut:micronaut-http-validation");
      project.getDependencies().add("annotationProcessor", "io.micronaut.data:micronaut-data-processor");
      project.getDependencies().add("annotationProcessor", "io.micronaut.serde:micronaut-serde-processor");


      project.getDependencies().add("annotationProcessor", String.format("org.apized:micronaut:%s", apizedVersion));
      project.getDependencies().add("implementation", String.format("org.apized:micronaut:%s", apizedVersion));
      project.getDependencies().add("testImplementation", String.format("org.apized:micronaut-test:%s", apizedVersion));

      File file = project.file("apized.properties");
      if (!file.exists()) {
        throw new GradleException("Missing file apized.properties on the project root folder");
      }
      apizedProperties.load(new FileInputStream(file));
      switch (Dialect.valueOf(apizedProperties.getProperty("dialect"))) {
        case POSTGRES -> {
          project.getDependencies().add("implementation", "io.micronaut.data:micronaut-data-jdbc");
          project.getDependencies().add("runtimeOnly", "org.postgresql:postgresql");
          project.getDependencies().add("runtimeOnly", "io.micronaut.sql:micronaut-jdbc-hikari");
        }
        default -> throw new IOException("Invalid Dialect: " + apizedProperties.getProperty("dialect"));
      }
    } catch (IOException e) {
      throw new GradleException(e.getMessage());
    }
  }
}
