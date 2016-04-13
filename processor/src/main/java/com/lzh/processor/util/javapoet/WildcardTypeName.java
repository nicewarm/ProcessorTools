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
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import static com.lzh.processor.util.javapoet.Util.checkArgument;

public final class WildcardTypeName extends com.lzh.processor.util.javapoet.TypeName {
  public final List<com.lzh.processor.util.javapoet.TypeName> upperBounds;
  public final List<com.lzh.processor.util.javapoet.TypeName> lowerBounds;

  private WildcardTypeName(List<com.lzh.processor.util.javapoet.TypeName> upperBounds, List<com.lzh.processor.util.javapoet.TypeName> lowerBounds) {
    this(upperBounds, lowerBounds, new ArrayList<com.lzh.processor.util.javapoet.AnnotationSpec>());
  }

  private WildcardTypeName(List<com.lzh.processor.util.javapoet.TypeName> upperBounds, List<com.lzh.processor.util.javapoet.TypeName> lowerBounds,
      List<com.lzh.processor.util.javapoet.AnnotationSpec> annotations) {
    super(annotations);
    this.upperBounds = com.lzh.processor.util.javapoet.Util.immutableList(upperBounds);
    this.lowerBounds = com.lzh.processor.util.javapoet.Util.immutableList(lowerBounds);

    checkArgument(this.upperBounds.size() == 1, "unexpected extends bounds: %s", upperBounds);
    for (com.lzh.processor.util.javapoet.TypeName upperBound : this.upperBounds) {
      checkArgument(!upperBound.isPrimitive() && upperBound != VOID,
          "invalid upper bound: %s", upperBound);
    }
    for (com.lzh.processor.util.javapoet.TypeName lowerBound : this.lowerBounds) {
      checkArgument(!lowerBound.isPrimitive() && lowerBound != VOID,
          "invalid lower bound: %s", lowerBound);
    }
  }

  @Override public WildcardTypeName annotated(com.lzh.processor.util.javapoet.AnnotationSpec... annotations) {
    return annotated(Arrays.asList(annotations));
  }

  @Override public WildcardTypeName annotated(List<AnnotationSpec> annotations) {
    return new WildcardTypeName(upperBounds, lowerBounds, annotations);
  }

  @Override public boolean equals(Object o) {
    return o instanceof WildcardTypeName
        && ((WildcardTypeName) o).upperBounds.equals(upperBounds)
        && ((WildcardTypeName) o).lowerBounds.equals(lowerBounds);
  }

  @Override public int hashCode() {
    return upperBounds.hashCode() ^ lowerBounds.hashCode();
  }

  @Override
  com.lzh.processor.util.javapoet.CodeWriter emit(com.lzh.processor.util.javapoet.CodeWriter out) throws IOException {
    emitAnnotations(out);
    if (lowerBounds.size() == 1) {
      return out.emit("? super $T", lowerBounds.get(0));
    }
    return upperBounds.get(0).equals(com.lzh.processor.util.javapoet.TypeName.OBJECT)
        ? out.emit("?")
        : out.emit("? extends $T", upperBounds.get(0));
  }

  /**
   * Returns a type that represents an unknown type that extends {@code bound}. For example, if
   * {@code bound} is {@code CharSequence.class}, this returns {@code ? extends CharSequence}. If
   * {@code bound} is {@code Object.class}, this returns {@code ?}, which is shorthand for {@code
   * ? extends Object}.
   */
  public static WildcardTypeName subtypeOf(com.lzh.processor.util.javapoet.TypeName upperBound) {
    return new WildcardTypeName(Arrays.asList(upperBound), Collections.<com.lzh.processor.util.javapoet.TypeName>emptyList());
  }

  public static WildcardTypeName subtypeOf(Type upperBound) {
    return subtypeOf(com.lzh.processor.util.javapoet.TypeName.get(upperBound));
  }

  /**
   * Returns a type that represents an unknown supertype of {@code bound}. For example, if {@code
   * bound} is {@code String.class}, this returns {@code ? super String}.
   */
  public static WildcardTypeName supertypeOf(com.lzh.processor.util.javapoet.TypeName lowerBound) {
    return new WildcardTypeName(Arrays.<com.lzh.processor.util.javapoet.TypeName>asList(OBJECT), Arrays.asList(lowerBound));
  }

  public static WildcardTypeName supertypeOf(Type lowerBound) {
    return supertypeOf(com.lzh.processor.util.javapoet.TypeName.get(lowerBound));
  }

  public static com.lzh.processor.util.javapoet.TypeName get(javax.lang.model.type.WildcardType mirror) {
    return get(mirror, new LinkedHashMap<TypeParameterElement, com.lzh.processor.util.javapoet.TypeVariableName>());
  }

  static com.lzh.processor.util.javapoet.TypeName get(
      javax.lang.model.type.WildcardType mirror,
      Map<TypeParameterElement, com.lzh.processor.util.javapoet.TypeVariableName> typeVariables) {
    TypeMirror extendsBound = mirror.getExtendsBound();
    if (extendsBound == null) {
      TypeMirror superBound = mirror.getSuperBound();
      if (superBound == null) {
        return subtypeOf(Object.class);
      } else {
        return supertypeOf(com.lzh.processor.util.javapoet.TypeName.get(superBound, typeVariables));
      }
    } else {
      return subtypeOf(com.lzh.processor.util.javapoet.TypeName.get(extendsBound, typeVariables));
    }
  }

  public static com.lzh.processor.util.javapoet.TypeName get(WildcardType wildcardName) {
    return get(wildcardName, new LinkedHashMap<Type, com.lzh.processor.util.javapoet.TypeVariableName>());
  }

  static TypeName get(WildcardType wildcardName, Map<Type, TypeVariableName> map) {
    return new WildcardTypeName(
        list(wildcardName.getUpperBounds(), map),
        list(wildcardName.getLowerBounds(), map));
  }
}
