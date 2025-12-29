package com.howtogrow.backend.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DtoSchemaDescriptionTest {

  @Test
  void allPublicApiDtoRecordComponentsHaveSchemaDescriptions() throws Exception {
    var basePackages =
        List.of(
            "com.howtogrow.backend.controller.admin.dto",
            "com.howtogrow.backend.controller.miniprogram.dto");

    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

    var missing = new ArrayList<String>();
    var visited = new HashSet<Class<?>>();

    for (var basePackage : basePackages) {
      for (var candidate : scanner.findCandidateComponents(basePackage)) {
        var className = candidate.getBeanClassName();
        if (className == null || className.isBlank()) {
          continue;
        }
        inspectRecordDeep(Class.forName(className), visited, missing);
      }
    }

    assertTrue(missing.isEmpty(), "缺少 @Schema(description) 的 DTO 字段：\n" + String.join("\n", missing));
  }

  private static void inspectRecordDeep(Class<?> clazz, Set<Class<?>> visited, List<String> missing) {
    if (!visited.add(clazz)) {
      return;
    }
    if (clazz.isRecord()) {
      for (RecordComponent component : clazz.getRecordComponents()) {
        var schema = findSchema(clazz, component);
        if (schema == null || schema.description() == null || schema.description().isBlank()) {
          missing.add(clazz.getName() + "#" + component.getName());
        }
      }
    }
    for (Class<?> nested : clazz.getDeclaredClasses()) {
      inspectRecordDeep(nested, visited, missing);
    }
  }

  private static Schema findSchema(Class<?> recordClass, RecordComponent component) {
    var schema = component.getAnnotation(Schema.class);
    if (schema != null) {
      return schema;
    }

    schema = component.getAccessor().getAnnotation(Schema.class);
    if (schema != null) {
      return schema;
    }

    try {
      schema = recordClass.getDeclaredField(component.getName()).getAnnotation(Schema.class);
      if (schema != null) {
        return schema;
      }
    } catch (NoSuchFieldException ignored) {
      // ignore
    }

    try {
      var ctor =
          recordClass.getDeclaredConstructor(
              java.util.Arrays.stream(recordClass.getRecordComponents())
                  .map(RecordComponent::getType)
                  .toArray(Class[]::new));
      for (var i = 0; i < recordClass.getRecordComponents().length; i++) {
        if (recordClass.getRecordComponents()[i].getName().equals(component.getName())) {
          return ctor.getParameters()[i].getAnnotation(Schema.class);
        }
      }
    } catch (NoSuchMethodException ignored) {
      // ignore
    }
    return null;
  }
}
