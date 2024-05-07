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

package org.apized.micronaut.messaging.rabbitmq.processor;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apized.micronaut.messaging.rabbitmq.consumer.RabbitMQConsume;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@SupportedAnnotationTypes("org.apized.micronaut.messaging.rabbitmq.*")
public class AnnotationProcessor extends AbstractProcessor {
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
      processConsumers(roundEnv);
    }

    return false;
  }

  private List<? extends Element> processConsumers(RoundEnvironment roundEnv) {
    List<? extends Element> generated = roundEnv.getElementsAnnotatedWith(RabbitMQConsume.class).stream().filter(it -> {
      if (it.getKind() != ElementKind.CLASS) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
        return false;
      } else {
        RabbitMQConsume annotation = it.getAnnotation(RabbitMQConsume.class);

        String genericType = processingEnv.getElementUtils().getTypeElement(it.asType().toString()).getInterfaces().get(0).toString();

        Map<String, Object> bindings = getDefaultBindings();
        bindings.put("module", ((PackageElement) it.getEnclosingElement()).getQualifiedName().toString());
        bindings.put("type", it.getSimpleName().toString());
        bindings.put("generic", genericType.replaceAll("^[^<]+<(.+)>$", "$1"));
        bindings.put("argument", String.join(", ", Arrays.stream(genericType.replaceAll("^[^<]+<(.+)>$", "$1").split("[,<>]")).map(t -> t + ".class").toList()));
        bindings.put("exchange", annotation.exchange());
        bindings.put("queue", annotation.queue());
        bindings.put("bindings", Arrays.stream(annotation.bindings()).map(b -> "\"" + b + "\"").collect(Collectors.joining(", ")));

        generateClassFor(bindings.get("module") + "." + bindings.get("type") + "$Listener", "Listener", bindings);
        return true;
      }
    }).toList();

    if (generated.size() > 0) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Generated %d Consumers", generated.size()));
    }
    return generated;
  }

  private void generateClassFor(String fullyQualifiedName, String template, Map<String, Object> bindings) {
    if (!isInitialized()) {
      return;
    }
    try {
      JavaFileObject file = processingEnv.getFiler().createSourceFile(fullyQualifiedName);
      Writer w = file.openWriter();
      VelocityEngine engine = new VelocityEngine();
      engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
      engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
      engine.init();
      Template vTemplate = engine.getTemplate("/templates/" + template + ".ft");
      vTemplate.merge(new VelocityContext(bindings), w);
      w.flush();
      w.close();
    } catch (Exception e) {
      e.printStackTrace();
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    }
  }

  private Map<String, Object> getDefaultBindings() {
    Map<String, Object> bindings = new HashMap<>();
    try {
      Properties prop = new Properties();
      prop.load(new FileInputStream(Paths.get(
        processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "apized.properties").toUri()
      ).toString().replace("/build/classes/java/main", "")));
      prop.entrySet().stream().forEach(e -> bindings.put(e.getKey().toString(), e.getValue()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bindings;
  }
}
