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

package org.apized.micronaut.processor;

import io.micronaut.serde.annotation.Serdeable;
import org.apized.core.StringHelper;
import org.apized.core.behaviour.annotation.Behaviour;
import org.apized.core.federation.Federated;
import org.apized.core.model.Apized;
import org.apized.core.model.Layer;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@SupportedAnnotationTypes("org.apized.*")
public class AnnotationProcessor extends AbstractProcessor {
  @Override
  public SourceVersion getSupportedSourceVersion() {
    SourceVersion sourceVersion = SourceVersion.latest();
    if (sourceVersion.ordinal() <= 17) {
      if (sourceVersion.ordinal() >= 8) {
        return sourceVersion;
      } else {
        return SourceVersion.RELEASE_8;
      }
    } else {
      return (SourceVersion.values())[17];
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (isInitialized()) {
      processApis(roundEnv);
      processFederations(roundEnv);
      processBehaviours(roundEnv);
    }

    return false;
  }

  private void processBehaviours(RoundEnvironment roundEnv) {
    ProcessingEnvironment env = processingEnv;
    List<Map<String, Object>> behaviours = roundEnv.getElementsAnnotatedWith(Behaviour.class).stream().map(it -> {
        if (it.getKind() != ElementKind.CLASS) {
          env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
          return null;
        } else {
          Behaviour annotation = it.getAnnotation(Behaviour.class);
          Element model = env.getTypeUtils().asElement(getModelFromBehaviour(annotation));
          return Map.of(
            "module", ((PackageElement) it.getEnclosingElement()).getQualifiedName().toString(),
            "type", it.getSimpleName().toString(),
            "name", ((TypeElement) it).getQualifiedName().toString().replaceAll("\\.", "_"),
            "annotation", Map.of(
              "module", ((PackageElement) model.getEnclosingElement()).getQualifiedName().toString(),
              "model", model.getSimpleName().toString(),
              "layer", annotation.layer().toString(),
              "when", Arrays.stream(annotation.when()).map(Objects::toString).toList(),
              "actions", Arrays.stream(annotation.actions()).map(Objects::toString).toList(),
              "order", annotation.order()
            )
          );
        }
      })
      .filter(Objects::nonNull)
      .toList();

    if (behaviours.size() > 0) {
      generateClassFor(
        "org.apized.core.behaviour.BehaviourRegistration",
        "RegisterBehaviours",
        new HashMap<>(
          Map.of(
            "behaviours", behaviours
          )
        )
      );
      env.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Registered %d Behaviours", behaviours.size()));
    }
  }

  private TypeMirror getModelFromBehaviour(Behaviour annotation) {
    try {
      annotation.model();
    } catch (MirroredTypeException mte) {
      return mte.getTypeMirror();
    }
    return null;
  }

  private void processApis(RoundEnvironment roundEnv) {
    AtomicBoolean generateAudit = new AtomicBoolean(false);
    AtomicBoolean generateEvent = new AtomicBoolean(false);

    List<? extends Element> generated = roundEnv.getElementsAnnotatedWith(Apized.class).stream().filter(it -> {
      if (it.getKind() != ElementKind.CLASS) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
        return false;
      } else {
        Apized annotation = it.getAnnotation(Apized.class);
        generateAudit.set(generateAudit.get() || annotation.audit());
        generateEvent.set(generateEvent.get() || annotation.event());

        Map<String, Object> bindings = getDefaultBindings();
        bindings.put("module", ((PackageElement) it.getEnclosingElement()).getQualifiedName().toString());
        bindings.put("type", it.getSimpleName().toString());
        bindings.put("actions", Arrays.stream(annotation.operations()).map(Enum::toString).collect(Collectors.toList()));

        if (Arrays.stream(annotation.layers()).toList().contains(Layer.REPOSITORY)) {
          generateClassFor(bindings.get("module") + "." + bindings.get("type") + "Repository", "Repository", bindings);
        }

        if (Arrays.stream(annotation.layers()).toList().contains(Layer.SERVICE)) {
          generateClassFor(bindings.get("module") + "." + bindings.get("type") + "Service", "Service", bindings);
        }

        if (Arrays.stream(annotation.layers()).toList().contains(Layer.CONTROLLER)) {
          bindings.put("path", getPaths(
            "/" + StringHelper.pluralize(StringHelper.uncapitalize(it.getSimpleName().toString())),
            getScopeFromApi(annotation)
          ));
          generateClassFor(bindings.get("module") + "." + bindings.get("type") + "Controller", "Controller", bindings);
        }

        generateClassFor(bindings.get("module") + "." + bindings.get("type") + "Deserializer", "Deserializer", bindings);

        return true;
      }
    }).toList();

    if (generated.size() > 0) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Generated %d APIs", generated.size()));
    }


    Map<String, Object> bindings = getDefaultBindings();
    bindings.put("module", "org.apized.micronaut.audit");

    if (generateAudit.get()) {
      generateClassFor(bindings.get("module") + "." + "AuditBehaviour", "audit/AuditBehaviour", bindings);
      generateClassFor(bindings.get("module") + "." + "AuditController", "audit/AuditController", bindings);
      generateClassFor(bindings.get("module") + "." + "AuditEntryRepository", "audit/AuditEntryRepository", bindings);
      generateClassFor(bindings.get("module") + "." + "PersistAuditsOnCommit", "audit/PersistAuditsOnCommit", bindings);
    }

    if (generateEvent.get()) {
      generateClassFor(bindings.get("module") + "." + "EventBehaviour", "event/EventBehaviour", bindings);
      generateClassFor(bindings.get("module") + "." + "SendEventsOnCommit", "event/SendEventsOnCommit", bindings);
    }
  }

  private void processFederations(RoundEnvironment roundEnv) {
    roundEnv.getElementsAnnotatedWith(Serdeable.class).stream().forEach(it -> {
      if (it.getKind() != ElementKind.CLASS) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
        return;
      }

      boolean assignable = processingEnv.getTypeUtils().isAssignable(
        it.asType(),
        processingEnv.getElementUtils().getTypeElement(Federated.class.getName()).asType()
      );

      if (!assignable) {
        return;
      }

      Map<String, Object> bindings = getDefaultBindings();
      bindings.put("module", ((PackageElement) it.getEnclosingElement()).getQualifiedName().toString());
      bindings.put("type", it.getSimpleName().toString());

      generateClassFor(bindings.get("module") + "." + bindings.get("type") + "Deserializer", "Deserializer", bindings);
    });
  }

  private List<String> getPaths(String path, List<? extends TypeMirror> scopeTypes) {
    List<String> paths = new ArrayList<>();
    for (TypeMirror typeMirror : scopeTypes) {
      Element scope = processingEnv.getTypeUtils().asElement(typeMirror);
      String scopeString = StringHelper.uncapitalize(scope.getSimpleName().toString());
      Apized subAnnotation = scope.getAnnotation(Apized.class);
      List<? extends TypeMirror> scopeFromApi = getScopeFromApi(subAnnotation);

      if (scopeFromApi.size() > 0) {
        paths.addAll(
          getPaths("/" + StringHelper.pluralize(scopeString) + "/{" + scopeString + "Id}" + path, scopeFromApi)
        );
      } else {
        paths.add("/" + StringHelper.pluralize(scopeString) + "/{" + scopeString + "Id}" + path);
      }
    }
    return paths.size() > 0 ? paths : List.of(path);
  }

  private List<? extends TypeMirror> getScopeFromApi(Apized annotation) {
    try {
      annotation.scope();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors();
    }
    return null;
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
