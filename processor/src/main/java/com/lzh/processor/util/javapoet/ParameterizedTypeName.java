/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lzh.processor.util.javapoet;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lzh.processor.util.javapoet.Util.checkArgument;
import static com.lzh.processor.util.javapoet.Util.checkNotNull;

public final class ParameterizedTypeName extends TypeName {
  public final com.lzh.processor.util.javapoet.ClassName rawType;
  public final List<TypeName> typeArguments;

  ParameterizedTypeName(com.lzh.processor.util.javapoet.ClassName rawType, List<TypeName> typeArguments) {
    this(rawType, typeArguments, new ArrayList<com.lzh.processor.util.javapoet.AnnotationSpec>());
  }

  ParameterizedTypeName(com.lzh.processor.util.javapoet.ClassName rawType, List<TypeName> typeArguments,
      List<com.lzh.processor.util.javapoet.AnnotationSpec> annotations) {
    super(annotations);
    this.rawType = checkNotNull(rawType, "rawType == null");
    this.typeArguments = Util.immutableList(typeArguments);

    checkArgument(!this.typeArguments.isEmpty(), "no type arguments: %s", rawType);
    for (TypeName typeArgument : this.typeArguments) {
      checkArgument(!typeArgument.isPrimitive() && typeArgument != VOID,
          "invalid type parameter: %s", typeArgument);
    }
  }

  @Override public ParameterizedTypeName annotated(com.lzh.processor.util.javapoet.AnnotationSpec... annotations) {
    return annotated(Arrays.asList(annotations));
  }

  @Override public ParameterizedTypeName annotated(List<com.lzh.processor.util.javapoet.AnnotationSpec> annotations) {
    return new ParameterizedTypeName(rawType, typeArguments, annotations);
  }

  @Override public boolean equals(Object o) {
    return o instanceof ParameterizedTypeName
        && ((ParameterizedTypeName) o).rawType.equals(rawType)
        && ((ParameterizedTypeName) o).typeArguments.equals(typeArguments);
  }

  @Override public int hashCode() {
    return rawType.hashCode() + 31 * typeArguments.hashCode();
  }

  @Override
  com.lzh.processor.util.javapoet.CodeWriter emit(com.lzh.processor.util.javapoet.CodeWriter out) throws IOException {
    emitAnnotations(out);
    rawType.emit(out);
    out.emitAndIndent("<");
    boolean firstParameter = true;
    for (TypeName parameter : typeArguments) {
      if (!firstParameter) out.emitAndIndent(", ");
      parameter.emit(out);
      firstParameter = false;
    }
    return out.emitAndIndent(">");
  }

  /** Returns a parameterized type, applying {@code typeArguments} to {@code rawType}. */
  public static ParameterizedTypeName get(com.lzh.processor.util.javapoet.ClassName rawType, TypeName... typeArguments) {
    return new ParameterizedTypeName(rawType, Arrays.asList(typeArguments));
  }

  /** Returns a parameterized type, applying {@code typeArguments} to {@code rawType}. */
  public static ParameterizedTypeName get(Class<?> rawType, Type... typeArguments) {
    return new ParameterizedTypeName(com.lzh.processor.util.javapoet.ClassName.get(rawType), list(typeArguments));
  }

  /** Returns a parameterized type equivalent to {@code type}. */
  public static ParameterizedTypeName get(ParameterizedType type) {
    return get(type, new LinkedHashMap<Type, TypeVariableName>());
  }

  /** Returns a parameterized type equivalent to {@code type}. */
  static ParameterizedTypeName get(ParameterizedType type, Map<Type, TypeVariableName> map) {
    return new ParameterizedTypeName(ClassName.get((Class<?>) type.getRawType()),
        TypeName.list(type.getActualTypeArguments(), map));
  }
}
