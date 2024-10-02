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

package org.apized.spring.processor;

import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apized.core.StringHelper;
import org.apized.core.behaviour.annotation.Behaviour;
import org.apized.core.federation.Federated;
import org.apized.core.model.Apized;
import org.apized.core.model.BaseModel;
import org.apized.core.model.Layer;
import org.apized.core.security.enricher.annotation.PermissionEnricher;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({ "unused", "rawtypes", "ResultOfMethodCallIgnored", "DataFlowIssue", "unchecked" })
@SupportedAnnotationTypes("org.apized.*")
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
      List<? extends Element> entities = processApis(roundEnv);
      processFederations(roundEnv);
      processInitializer(roundEnv, entities);
    }

    return false;
  }

  private void processInitializer(RoundEnvironment roundEnv, List<? extends Element> entities) {
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

    List<Map<String, Object>> enrichers = roundEnv.getElementsAnnotatedWith(PermissionEnricher.class).stream().map(it -> {
        if (it.getKind() != ElementKind.CLASS) {
          env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
          return null;
        } else {
          PermissionEnricher annotation = it.getAnnotation(PermissionEnricher.class);
          Element model = env.getTypeUtils().asElement(getModelFromPermissionEnricher(annotation));
          return Map.of(
            "module", ((PackageElement) it.getEnclosingElement()).getQualifiedName().toString(),
            "type", it.getSimpleName().toString(),
            "name", ((TypeElement) it).getQualifiedName().toString().replaceAll("\\.", "_"),
            "annotation", Map.of(
              "module", ((PackageElement) model.getEnclosingElement()).getQualifiedName().toString(),
              "model", model.getSimpleName().toString()
            )
          );
        }
      })
      .filter(Objects::nonNull)
      .toList();

    if (!behaviours.isEmpty() || !enrichers.isEmpty() || !entities.isEmpty()) {
      Map<String, Object> bindings = getDefaultBindings();
      bindings.putAll(Map.of(
        "entities", entities.stream().map(e -> ((TypeElement) e).getQualifiedName().toString()).toList(),
        "behaviours", behaviours,
        "enrichers", enrichers
      ));
      generateClassFor(
        "org.apized.core.init.ApizedSpringInitializer",
        "Initializer",
        bindings
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

  private TypeMirror getModelFromPermissionEnricher(PermissionEnricher annotation) {
    try {
      annotation.value();
    } catch (MirroredTypeException mte) {
      return mte.getTypeMirror();
    }
    return null;
  }

  private Map getActionDescription(Element it, List<String> actions) {
    HashMap<String, String> result = new HashMap<>();
    actions.forEach(action -> {
      String description = "";
      try {
        Path root = Path.of(processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "a").toUri()).getParent().getParent().getParent().getParent().getParent();
        Path source = Path.of(root.toString(), "src", "main", "resources", "docs", it.getSimpleName().toString().toLowerCase(), String.format("%s.md", action.toLowerCase()));
        if (Files.exists(source)) {
          description = Files.readString(source);
        }
      } catch (IOException e) {
        // Do nothing
      }
      result.put(action, description);
    });
    return result;
  }

  private List<? extends Element> processApis(RoundEnvironment roundEnv) {
    List<? extends Element> generated = roundEnv.getElementsAnnotatedWith(Apized.class).stream().filter(it -> {
      if (it.getKind() != ElementKind.CLASS) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
        return false;
      } else {
        Apized annotation = it.getAnnotation(Apized.class);
        Map<String, Object> bindings = getDefaultBindings();
        bindings.put("module", ((PackageElement) it.getEnclosingElement()).getQualifiedName().toString());
        bindings.put("type", it.getSimpleName().toString());
        bindings.put("actions", Arrays.stream(annotation.operations()).map(Enum::toString).collect(Collectors.toList()));
        bindings.put("maxPageSize", annotation.maxPageSize());
        bindings.put("descriptions", getActionDescription(it, Arrays.stream(annotation.operations()).map(Enum::toString).toList()));
        bindings.put("extension", Map.of(
          "repository", Map.of(
            "imports", new ArrayList<String>(),
            "implements", new ArrayList<String>()
          ),
          "service", Map.of(
            "imports", new HashSet<String>(),
            "injects", new ArrayList<String>(),
            "methods", new ArrayList<Map<String, String>>(),
            "actions", new HashMap<String, Map<String, String>>(),
            "excludes", new ArrayList<String>()
          ),
          "controller", Map.of(
            "imports", new HashSet<String>(),
            "injects", new ArrayList<String>(),
            "methods", new ArrayList<Map<String, String>>(),
            "actions", new HashMap<String, Map<String, String>>()
          )
        ));
        bindings.put("subModels", new ArrayList<>());
        bindings.put("manyToMany", new ArrayList<>());

        TypeMirror baseModelType = processingEnv.getElementUtils().getTypeElement(BaseModel.class.getName()).asType();
        TypeMirror collectionType = processingEnv.getTypeUtils().getDeclaredType(processingEnv.getElementUtils().getTypeElement(Collection.class.getName()), processingEnv.getTypeUtils().getWildcardType(baseModelType, null));
        TypeMirror optionalType = processingEnv.getTypeUtils().getDeclaredType(processingEnv.getElementUtils().getTypeElement(Optional.class.getName()), processingEnv.getTypeUtils().getWildcardType(baseModelType, null));
        it.getEnclosedElements().stream().filter(el -> el.getKind().equals(ElementKind.FIELD))
          .filter(el ->
            {
              TypeMirror elementType = el.asType();
              return el.getAnnotation(Transient.class) == null && (
                processingEnv.getTypeUtils().isAssignable(elementType, baseModelType) ||
                  processingEnv.getTypeUtils().isAssignable(elementType, collectionType)
              );
            }
          )
          .peek(el ->
            ((List<Map<String, Object>>) bindings.get("subModels")).add(
              Map.of(
                "Name", StringHelper.capitalize(el.getSimpleName().toString()),
                "name", el.getSimpleName().toString(),
                "type", ((TypeElement) processingEnv.getTypeUtils().asElement(el.asType())).getQualifiedName().toString(),
                "isCollection", processingEnv.getTypeUtils().isAssignable(el.asType(), collectionType),
                "typeParam", processingEnv.getTypeUtils().isAssignable(el.asType(), collectionType)
                  ? el.asType().toString().replaceAll(".*<(.*?)>.*", "$1")
                  : ""
              )
            )
          )
          .filter(el -> Arrays.stream(el.getAnnotationsByType(ManyToMany.class)).filter(a -> a.mappedBy().isBlank()).toList().size() > 0)
          .forEach(el ->
            ((List<Map<String, Object>>) bindings.get("manyToMany")).add(Map.of(
              "Name", StringHelper.capitalize(el.getSimpleName().toString()),
              "name", el.getSimpleName().toString(),
              "table", el.getAnnotation(JoinTable.class).name(),
              "self", el.getAnnotation(JoinTable.class).joinColumns()[0].name(),
              "other", el.getAnnotation(JoinTable.class).inverseJoinColumns()[0].name()
            ))
          );

        try {
          annotation.extensions();
        } catch (MirroredTypesException exc) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Extension for %s", it.getSimpleName()));

          //noinspection unchecked
          Map<String, Map<String, Collection<Object>>> extensionBindings = (Map<String, Map<String, Collection<Object>>>) bindings.get("extension");

          exc.getTypeMirrors()
            .stream()
            .map(e -> (TypeElement) processingEnv.getTypeUtils().asElement(e))
            .forEach(extension -> {
              Apized.Extension extensionAnnotation = extension.getAnnotation(Apized.Extension.class);
              List<ExecutableElement> methods = extension.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind().equals(ElementKind.METHOD))
                .map(e -> (ExecutableElement) e)
                .toList();
              extensionBindings.get("service").get("excludes").addAll(List.of(extensionAnnotation.exclude()));

              switch (extensionAnnotation.layer()) {
                case CONTROLLER -> {
                  String injectName = StringHelper.uncapitalize(extension.getSimpleName().toString());
                  extensionBindings.get("controller").get("injects").add(String.format("%s %s", extension.getQualifiedName().toString(), injectName));
                  extensionBindings.get("controller").get("methods").addAll(
                    methods.stream().filter(m -> m.getModifiers().contains(Modifier.PUBLIC) && m.getAnnotation(Apized.Extension.Action.class) == null).map(m ->
                      Map.of(
                        "returnType", m.getReturnType().toString(),
                        "isModel", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), baseModelType),
                        "isCollection", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), collectionType),
                        "isOptional", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), optionalType),
                        "returnTypeWrappedParameter", m.getReturnType().toString().replaceAll(".*<(.*?)>.*", "$1"),
                        "name", m.getSimpleName().toString(),
                        "parameters", m.getParameters().stream().map(p -> p.asType().toString() + " " + p.getSimpleName()).collect(Collectors.joining(", ")),
                        "callee", injectName,
                        "arguments", m.getParameters().stream().map(p -> p.getSimpleName().toString()).collect(Collectors.joining(", "))
                      )
                    ).toList()
                  );
                  ((Map) extensionBindings.get("controller").get("actions")).putAll(
                    methods.stream().filter(m -> m.getModifiers().contains(Modifier.PUBLIC) && m.getAnnotation(Apized.Extension.Action.class) != null).map(m ->
                      Map.of(
                        "returnType", m.getReturnType().toString(),
                        "isModel", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), baseModelType),
                        "isCollection", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), collectionType),
                        "isOptional", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), optionalType),
                        "returnTypeWrappedParameter", m.getReturnType().toString().replaceAll(".*<(.*?)>.*", "$1"),
                        "name", m.getSimpleName().toString(),
                        "parameters", m.getParameters().stream().map(p -> p.asType().toString() + " " + p.getSimpleName()).collect(Collectors.joining(", ")),
                        "callee", injectName,
                        "arguments", m.getParameters().stream().map(p -> p.getSimpleName().toString()).collect(Collectors.joining(", ")),
                        "action", m.getAnnotation(Apized.Extension.Action.class).value().toString()
                      )
                    ).collect(Collectors.toMap((v) -> v.get("action"), (v) -> v))
                  );
                }
                case SERVICE -> {
                  String injectName = StringHelper.uncapitalize(extension.getSimpleName().toString());
                  extensionBindings.get("service").get("injects").add(String.format("%s %s", extension.getQualifiedName().toString(), injectName));
                  extensionBindings.get("service").get("methods").addAll(
                    methods.stream()
                      .filter(m -> m.getModifiers().contains(Modifier.PUBLIC) && m.getAnnotation(Apized.Extension.Action.class) == null)
                      .map(m -> {
                        if (!List.of(extensionAnnotation.exclude()).contains(m.getSimpleName().toString())) {
                          return Map.of(
                            "returnType", m.getReturnType().toString(),
                            "isModel", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), baseModelType),
                            "isCollection", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), collectionType),
                            "isOptional", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), optionalType),
                            "returnTypeWrappedParameter", m.getReturnType().toString().replaceAll(".*<(.*?)>.*", "$1"),
                            "name", m.getSimpleName().toString(),
                            "parameters", m.getParameters().stream().map(p -> p.asType().toString() + " " + p.getSimpleName()).collect(Collectors.joining(", ")),
                            "callee", injectName,
                            "arguments", m.getParameters().stream().map(p -> p.getSimpleName().toString()).collect(Collectors.joining(", "))
                          );
                        }
                        return null;
                      })
                      .filter(Objects::nonNull)
                      .toList()
                  );
                  ((Map) extensionBindings.get("service").get("actions")).putAll(
                    methods.stream().filter(m -> m.getModifiers().contains(Modifier.PUBLIC) && m.getAnnotation(Apized.Extension.Action.class) != null).map(m ->
                      Map.of(
                        "returnType", m.getReturnType().toString(),
                        "isModel", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), baseModelType),
                        "isCollection", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), collectionType),
                        "isOptional", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), optionalType),
                        "returnTypeWrappedParameter", m.getReturnType().toString().replaceAll(".*<(.*?)>.*", "$1"),
                        "name", m.getSimpleName().toString(),
                        "parameters", m.getParameters().stream().map(p -> p.asType().toString() + " " + p.getSimpleName()).collect(Collectors.joining(", ")),
                        "callee", injectName,
                        "arguments", m.getParameters().stream().map(p -> p.getSimpleName().toString()).collect(Collectors.joining(", ")),
                        "action", m.getAnnotation(Apized.Extension.Action.class).value().toString()
                      )
                    ).collect(Collectors.toMap((v) -> v.get("action"), (v) -> v))
                  );
                }
                case REPOSITORY -> {
                  extensionBindings.get("repository").get("imports").add(extension.getQualifiedName().toString());
                  extensionBindings.get("repository").get("implements").add(extension.getSimpleName().toString());
                  extensionBindings.get("service").get("methods").addAll(
                    methods.stream()
                      .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                      .map(m -> {
                        if (!List.of(extensionAnnotation.exclude()).contains(m.getSimpleName().toString())) {
                          return Map.of(
                            "returnType", m.getReturnType().toString(),
                            "isModel", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), baseModelType),
                            "isCollection", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), collectionType),
                            "isOptional", processingEnv.getTypeUtils().isAssignable(m.getReturnType(), optionalType),
                            "returnTypeWrappedParameter", m.getReturnType().toString().replaceAll(".*<(.*?)>.*", "$1"),
                            "name", m.getSimpleName().toString(),
                            "parameters", m.getParameters().stream().map(p -> p.asType().toString() + " " + p.getSimpleName()).collect(Collectors.joining(", ")),
                            "callee", "repository",
                            "arguments", m.getParameters().stream().map(p -> p.getSimpleName().toString()).collect(Collectors.joining(", "))
                          );
                        }
                        return null;
                      })
                      .filter(Objects::nonNull)
                      .toList()
                  );
                }
                default -> {

                }
              }
            });
          processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("%s", bindings.get("extension")));
        }

//        generateClassFor(bindings.get("module") + "." + bindings.get("type") + "$Proxy", "Proxy", bindings);

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
          bindings.put("security", bindings.getOrDefault("security", "bearerAuth"));
          generateClassFor(bindings.get("module") + "." + bindings.get("type") + "Controller", "Controller", bindings);
        }

        generateClassFor(bindings.get("module") + "." + bindings.get("type") + "Deserializer", "Deserializer", bindings);

        return true;
      }
    }).toList();

    if (generated.size() > 0) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Generated %d APIs", generated.size()));
      Map<String, Object> bindings = getDefaultBindings();
      bindings.put("module", "org.apized.spring.audit");
//      generateClassFor("org.apized.spring.audit.SpringAuditEntryRepository", "audit/Repository", bindings);
    }
    return generated;
  }

  private void processFederations(RoundEnvironment roundEnv) {
    roundEnv.getElementsAnnotatedWith(Federated.class).forEach(it -> {
      if (it.getKind() != ElementKind.CLASS) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Not a class", it);
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
    Properties prop = new Properties();
    try {
      prop.load(new FileInputStream(Paths.get(
        processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "apized.properties").toUri()
      ).toString().replace("/build/classes/java/main", "")));
    } catch (IOException e) {
      prop.setProperty("dialect", "ANSI");
    }
    prop.forEach((key, value) -> bindings.put(key.toString(), value));
    return bindings;
  }
}
