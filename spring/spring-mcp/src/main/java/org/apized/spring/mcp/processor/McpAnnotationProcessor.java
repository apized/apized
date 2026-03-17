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

package org.apized.spring.mcp.processor;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apized.core.model.Apized;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
@SupportedAnnotationTypes("org.apized.*")
public class McpAnnotationProcessor extends AbstractProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    SourceVersion sourceVersion = SourceVersion.latest();
    if (sourceVersion.ordinal() <= 21) {
      if (sourceVersion.ordinal() <= 17) {
        if (sourceVersion.ordinal() >= 8) {
          return sourceVersion;
        } else {
          return SourceVersion.RELEASE_8;
        }
      } else {
        return (SourceVersion.values())[17];
      }
    } else {
      return (SourceVersion.values())[21];
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (isInitialized()) {
      roundEnv.getElementsAnnotatedWith(Apized.class).forEach(it -> {
        if (it.getKind() != ElementKind.CLASS) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
          return;
        }
        Apized annotation = it.getAnnotation(Apized.class);
        String type = it.getSimpleName().toString();
        String module = ((PackageElement) it.getEnclosingElement()).getQualifiedName().toString();
        String snakeName = toSnakeCase(type);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("module", module);
        bindings.put("type", type);
        bindings.put("snakeName", snakeName);
        bindings.put("actions", Arrays.stream(annotation.operations()).map(Enum::toString).collect(Collectors.toList()));

        if (annotation.mcp()) {
          generateClassFor(module + "." + type + "McpTools", bindings);
          processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Generated McpTools for %s", type));
        }
      });
    }
    return false;
  }

  private String toSnakeCase(String name) {
    return name.replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("^_", "");
  }

  private void generateClassFor(String fullyQualifiedName, Map<String, Object> bindings) {
    if (!isInitialized()) {
      return;
    }
    try {
      JavaFileObject file = processingEnv.getFiler().createSourceFile(fullyQualifiedName);
      Writer w = file.openWriter();
      VelocityEngine engine = new VelocityEngine();
      engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
      engine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
      engine.init();
      Template vTemplate = engine.getTemplate("/templates/McpTools.ft");
      vTemplate.merge(new VelocityContext(bindings), w);
      w.flush();
      w.close();
    } catch (Exception e) {
      e.printStackTrace();
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    }
  }
}
