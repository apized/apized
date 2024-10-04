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

import com.bmuschko.gradle.docker.DockerSpringBootApplicationPlugin;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.springframework.boot.gradle.plugin.SpringBootPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * Apized Plugin
 */
@SuppressWarnings("unused")
public class ApizedSpringPlugin implements Plugin<Project> {

  /**
   * Constructor
   */
  public ApizedSpringPlugin() {
  }

  @Override
  public void apply(Project project) {
    try {
      Properties myProperties = new Properties();
      myProperties.load(getClass().getClassLoader().getResourceAsStream("apized.properties"));
      String apizedVersion = myProperties.getProperty("version");
      String micronautVersion = myProperties.getProperty("micronautVersion");

      project.getExtensions().add("micronautVersion", micronautVersion);
      project.getPlugins().apply("groovy");

      project.getPlugins().apply(SpringBootPlugin.class);
      project.getPlugins().apply(DependencyManagementPlugin.class);
      project.getPlugins().apply(DockerSpringBootApplicationPlugin.class);
      project.getPlugins().withType(JavaPlugin.class, plugin -> {
        project.getDependencies().add("annotationProcessor", "org.projectlombok:lombok:1.18.30");
        project.getDependencies().add("compileOnly", "org.projectlombok:lombok:1.18.30");
      });

      //replace with either jitpack or simply remove (and add on the docs that the packages are available via jitpack
      project.getRepositories().maven(r -> {
        r.setName("Apized GitHub Repository");
        r.setUrl("https://maven.pkg.github.com/apized/apized");
        r.credentials(c -> {
          c.setUsername("apized-bot");
          c.setPassword("gh" +
            "p_x9oqph1jm" +
            "81lnHg4QMQA" +
            "o3M1XkxcM72X" +
            "1nAx");
        });
      });

      project.getDependencies().add("annotationProcessor", String.format("org.apized:spring-core:%s", apizedVersion));
      project.getDependencies().add("implementation", String.format("org.apized:spring-core:%s", apizedVersion));

      project.getDependencies().add("annotationProcessor", String.format("io.micronaut:micronaut-inject-java:%s", micronautVersion));
      project.getDependencies().add("annotationProcessor", "io.micronaut.serde:micronaut-serde-processor:2.11.0");
      project.getDependencies().add("compileOnly", String.format("io.micronaut.data:micronaut-data-model:%s", micronautVersion));

      project.getDependencies().add("testImplementation", String.format("org.apized:spring-test:%s", apizedVersion));

      Properties apizedProperties = new Properties();
      File file = project.file("apized.properties");
      if (file.exists()) {
        apizedProperties.load(new FileInputStream(file));
      } else {
        System.out.println("Missing file apized.properties. Using ANSI as default dialect");
      }

      switch (Dialect.valueOf(apizedProperties.getProperty("dialect", "ANSI"))) {
        case H2 -> project.getDependencies().add("runtimeOnly", "com.h2database:h2");
        case MYSQL -> {
          project.getDependencies().add("runtimeOnly", "mysql:mysql-connector-java");
          project.getDependencies().add("runtimeOnly", "org.flywaydb:flyway-mysql");
        }
        case POSTGRES -> {
          project.getDependencies().add("runtimeOnly", "org.postgresql:postgresql");
          project.getDependencies().add("runtimeOnly", "org.flywaydb:flyway-database-postgresql");
        }
        case ORACLE -> {
          project.getDependencies().add("runtimeOnly", "com.oracle.database.jdbc:ojdbc8");
        }
        case SQL_SERVER -> {
          project.getDependencies().add("implementation", "com.microsoft.sqlserver:mssql-jdbc");
          project.getDependencies().add("runtimeOnly", "org.flywaydb:flyway-sqlserver");
        }
      }
    } catch (IOException e) {
      throw new GradleException(e.getMessage());
    }
  }
}
