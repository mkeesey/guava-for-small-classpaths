/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.reflect;

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link Types}.
 *
 * @author Ben Yu
 */
public class TypesTest extends TestCase {
  
  public void testNewParameterizedType_ownerTypeImplied() throws Exception {
    ParameterizedType jvmType = (ParameterizedType)
        new TypeCapture<Map.Entry<String, Integer>>() {}.capture();
    ParameterizedType ourType = Types.newParameterizedType(
        Map.Entry.class, String.class, Integer.class);
    assertEquals(jvmType, ourType);
    assertEquals(Map.class, ourType.getOwnerType());
  }

  public void testNewParameterizedType() {
    ParameterizedType jvmType = (ParameterizedType)
        new TypeCapture<HashMap<String, int[][]>>() {}.capture();
    ParameterizedType ourType = Types.newParameterizedType(
        HashMap.class, String.class, int[][].class);

    new EqualsTester()
        .addEqualityGroup(jvmType, ourType)
        .testEquals();
    assertEquals(jvmType.toString(), ourType.toString());
    assertEquals(jvmType.hashCode(), ourType.hashCode());
    assertEquals(HashMap.class, ourType.getRawType());
    ASSERT.that(ourType.getActualTypeArguments())
        .hasContentsInOrder(jvmType.getActualTypeArguments());
    assertEquals(Arrays.asList(
            String.class,
            Types.newArrayType(Types.newArrayType(int.class))),
        Arrays.asList(ourType.getActualTypeArguments()));
    assertEquals(null, ourType.getOwnerType());
  }

  public void testNewParameterizedType_nonStaticLocalClass() {
    class LocalClass<T> {}
    Type jvmType = new LocalClass<String>() {}.getClass().getGenericSuperclass();
    Type ourType = Types.newParameterizedType(LocalClass.class, String.class);
    assertEquals(jvmType, ourType);
  }

  public void testNewParameterizedType_staticLocalClass() {
    doTestNewParameterizedType_staticLocalClass();
  }

  private static void doTestNewParameterizedType_staticLocalClass() {
    class LocalClass<T> {}
    Type jvmType = new LocalClass<String>() {}.getClass().getGenericSuperclass();
    Type ourType = Types.newParameterizedType(LocalClass.class, String.class);
    assertEquals(jvmType, ourType);
  }

  public void testNewParameterizedTypeWithOwner() {
    ParameterizedType jvmType = (ParameterizedType)
        new TypeCapture<Map.Entry<String, int[][]>>() {}.capture();
    ParameterizedType ourType = Types.newParameterizedTypeWithOwner(
        Map.class, Map.Entry.class, String.class, int[][].class);

    new EqualsTester()
        .addEqualityGroup(jvmType, ourType)
        .addEqualityGroup(new TypeCapture<Map.Entry<String, String>>() {}.capture())
        .addEqualityGroup(new TypeCapture<Map<String, Integer>>() {}.capture())
        .testEquals();
    assertEquals(jvmType.toString(), ourType.toString());
    assertEquals(Map.class, ourType.getOwnerType());
    assertEquals(Map.Entry.class, ourType.getRawType());
    ASSERT.that(ourType.getActualTypeArguments())
        .hasContentsInOrder(jvmType.getActualTypeArguments());
  }
  
  public void testNewParameterizedType_serializable() {
    SerializableTester.reserializeAndAssert(Types.newParameterizedType(
        Map.Entry.class, String.class, Integer.class));
  }
  
  public void testNewParameterizedType_ownerMismatch() {
    try {
      Types.newParameterizedTypeWithOwner(
          Number.class, List.class, String.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }
  
  public void testNewParameterizedType_ownerMissing() {
    assertEquals(
        Types.newParameterizedType(Map.Entry.class, String.class, Integer.class),
        Types.newParameterizedTypeWithOwner(
            null, Map.Entry.class, String.class, Integer.class));
  }
  
  public void testNewParameterizedType_invalidTypeParameters() {
    try {
      Types.newParameterizedTypeWithOwner(
          Map.class, Map.Entry.class, String.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }
  
  public void testNewParameterizedType_primitiveTypeParameters() {
    try {
      Types.newParameterizedTypeWithOwner(
          Map.class, Map.Entry.class, int.class, int.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testNewArrayType() {
    Type jvmType1 = new TypeCapture<List<String>[]>() {}.capture();
    GenericArrayType ourType1 = (GenericArrayType) Types.newArrayType(
        Types.newParameterizedType(List.class, String.class));
    Type jvmType2 = new TypeCapture<List[]>() {}.capture();
    Type ourType2 = Types.newArrayType(List.class);
    new EqualsTester()
        .addEqualityGroup(jvmType1, ourType1)
        .addEqualityGroup(jvmType2, ourType2)
        .testEquals();
    assertEquals(new TypeCapture<List<String>>() {}.capture(),
        ourType1.getGenericComponentType());
    assertEquals(jvmType1.toString(), ourType1.toString());
    assertEquals(jvmType2.toString(), ourType2.toString());
  }
  
  public void testNewArrayTypeOfArray() {
    Type jvmType = new TypeCapture<int[][]>() {}.capture();
    Type ourType = Types.newArrayType(int[].class);
    assertEquals(jvmType.toString(), ourType.toString());
    new EqualsTester()
        .addEqualityGroup(jvmType, ourType)
        .testEquals();
  }
  
  public void testNewArrayType_primitive() {
    Type jvmType = new TypeCapture<int[]>() {}.capture();
    Type ourType = Types.newArrayType(int.class);
    assertEquals(jvmType.toString(), ourType.toString());
    new EqualsTester()
        .addEqualityGroup(jvmType, ourType)
        .testEquals();
  }
  
  public void testNewArrayType_upperBoundedWildcard() {
    Type wildcard = Types.subtypeOf(Number.class);
    assertEquals(Types.subtypeOf(Number[].class), Types.newArrayType(wildcard));
  }
  
  public void testNewArrayType_lowerBoundedWildcard() {
    Type wildcard = Types.supertypeOf(Number.class);
    assertEquals(Types.supertypeOf(Number[].class), Types.newArrayType(wildcard));
  }
  
  public void testNewArrayType_serializable() {
    SerializableTester.reserializeAndAssert(
        Types.newArrayType(int[].class));
  }

  private static class WithWildcardType {

    @SuppressWarnings("unused")
    void withoutBound(List<?> list) {}

    @SuppressWarnings("unused")
    void withObjectBound(List<? extends Object> list) {}

    @SuppressWarnings("unused")
    void withUpperBound(List<? extends int[][]> list) {}

    @SuppressWarnings("unused")
    void withLowerBound(List<? super String[][]> list) {}
    
    static WildcardType getWildcardType(String methodName) throws Exception {
      ParameterizedType parameterType = (ParameterizedType)
          WithWildcardType.class
              .getDeclaredMethod(methodName, List.class)
              .getGenericParameterTypes()[0];
      return (WildcardType) parameterType.getActualTypeArguments()[0];
    }
  }
  
  public void testNewWildcardType() throws Exception {
    WildcardType noBoundJvmType =
        WithWildcardType.getWildcardType("withoutBound");
    WildcardType objectBoundJvmType =
        WithWildcardType.getWildcardType("withObjectBound");
    WildcardType upperBoundJvmType =
        WithWildcardType.getWildcardType("withUpperBound");
    WildcardType lowerBoundJvmType =
        WithWildcardType.getWildcardType("withLowerBound");
    WildcardType objectBound =
        Types.subtypeOf(Object.class);
    WildcardType upperBound =
        Types.subtypeOf(int[][].class);
    WildcardType lowerBound =
        Types.supertypeOf(String[][].class);

    assertEqualWildcardType(noBoundJvmType, objectBound);
    assertEqualWildcardType(objectBoundJvmType, objectBound);
    assertEqualWildcardType(upperBoundJvmType, upperBound);
    assertEqualWildcardType(lowerBoundJvmType, lowerBound);
    
    new EqualsTester()
        .addEqualityGroup(
            noBoundJvmType, objectBoundJvmType, objectBound)
        .addEqualityGroup(upperBoundJvmType, upperBound)
        .addEqualityGroup(lowerBoundJvmType, lowerBound)
        .testEquals();
  }
  
  public void testNewWildcardType_primitiveTypeBound() {
    try {
      Types.subtypeOf(int.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }
  
  public void testNewWildcardType_serializable() {
    SerializableTester.reserializeAndAssert(
        Types.supertypeOf(String.class));
    SerializableTester.reserializeAndAssert(
        Types.subtypeOf(String.class));
    SerializableTester.reserializeAndAssert(
        Types.subtypeOf(Object.class));
  }
  
  private static void assertEqualWildcardType(
      WildcardType expected, WildcardType actual) {
    assertEquals(expected.toString(), actual.toString());
    assertEquals(actual.toString(), expected.hashCode(), actual.hashCode());
    ASSERT.that(actual.getLowerBounds())
        .hasContentsInOrder(expected.getLowerBounds());
    ASSERT.that(actual.getUpperBounds())
        .hasContentsInOrder(expected.getUpperBounds());
  }
  
  private static class WithTypeVariable {
    
    @SuppressWarnings("unused") 
    <T> void withoutBound(List<T> list) {}

    @SuppressWarnings("unused") 
    <T extends Object> void withObjectBound(List<T> list) {}

    @SuppressWarnings("unused") 
    <T extends Number & CharSequence> void withUpperBound(List<T> list) {}
    
    static TypeVariable<?> getTypeVariable(String methodName) throws Exception {
      ParameterizedType parameterType = (ParameterizedType)
          WithTypeVariable.class
              .getDeclaredMethod(methodName, List.class)
              .getGenericParameterTypes()[0];
      return (TypeVariable<?>) parameterType.getActualTypeArguments()[0];
    }
  }
  
  public void testNewTypeVariable() throws Exception {
    TypeVariable<?> noBoundJvmType =
        WithTypeVariable.getTypeVariable("withoutBound");
    TypeVariable<?> objectBoundJvmType =
        WithTypeVariable.getTypeVariable("withObjectBound");
    TypeVariable<?> upperBoundJvmType =
        WithTypeVariable.getTypeVariable("withUpperBound");
    TypeVariable<?> noBound = withBounds(noBoundJvmType);
    TypeVariable<?> objectBound = withBounds(objectBoundJvmType, Object.class);
    TypeVariable<?> upperBound = withBounds(
        upperBoundJvmType, Number.class, CharSequence.class);
    
    assertEqualTypeVariable(noBoundJvmType, noBound);
    assertEqualTypeVariable(noBoundJvmType,
        withBounds(noBoundJvmType, Object.class));
    assertEqualTypeVariable(objectBoundJvmType, objectBound);
    assertEqualTypeVariable(upperBoundJvmType, upperBound);
    
    new EqualsTester()
        .addEqualityGroup(noBoundJvmType, noBound)
        .addEqualityGroup(objectBoundJvmType, objectBound)
        .addEqualityGroup(
            upperBoundJvmType, upperBound,
            withBounds(upperBoundJvmType, CharSequence.class)) // bounds ignored
        .testEquals();
  }
  
  public void testNewTypeVariable_primitiveTypeBound() {
    try {
      Types.newTypeVariable(List.class, "E", int.class);
      fail();
    } catch (IllegalArgumentException expected) {}
  }
  
  public void testNewTypeVariable_serializable() throws Exception {
    try {
      SerializableTester.reserialize(Types.newTypeVariable(List.class, "E"));
      fail();
    } catch (RuntimeException expected) {}
  }
  
  private static <D extends GenericDeclaration> TypeVariable<D> withBounds(
      TypeVariable<D> typeVariable, Type... bounds) {
    return Types.newTypeVariable(
        typeVariable.getGenericDeclaration(), typeVariable.getName(), bounds);
  }
  
  private static void assertEqualTypeVariable(
      TypeVariable<?> expected, TypeVariable<?> actual) {
    assertEquals(expected.toString(), actual.toString());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(
        expected.getGenericDeclaration(), actual.getGenericDeclaration());
    assertEquals(actual.toString(), expected.hashCode(), actual.hashCode());
    ASSERT.that(actual.getBounds()).hasContentsInOrder(expected.getBounds());
  }

  /**
   * Working with arrays requires defensive code. Verify that we clone the
   * type array for both input and output.
   */
  public void testNewParameterizedTypeImmutability() {
    Type[] typesIn = { String.class, Integer.class };
    ParameterizedType parameterizedType
        = Types.newParameterizedType(Map.class, typesIn);
    typesIn[0] = null;
    typesIn[1] = null;

    Type[] typesOut = parameterizedType.getActualTypeArguments();
    typesOut[0] = null;
    typesOut[1] = null;

    assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
    assertEquals(Integer.class, parameterizedType.getActualTypeArguments()[1]);
  }

  public void testNewParameterizedTypeWithWrongNumberOfTypeArguments() {
    try {
      Types.newParameterizedType(
          Map.class, String.class, Integer.class, Long.class);
      fail();
    } catch(IllegalArgumentException expected) {}
  }
  
  public void testToString() {
    assertEquals(int[].class.getName(), Types.toString(int[].class));
    assertEquals(int[][].class.getName(), Types.toString(int[][].class));
    assertEquals(String[].class.getName(), Types.toString(String[].class));
    Type elementType = List.class.getTypeParameters()[0];
    assertEquals(elementType.toString(), Types.toString(elementType));
  }

  public void testNullPointers() throws Exception {
    new NullPointerTester()
        .setDefault(Type[].class, new Type[]{ Map.class })
        .setDefault(Type.class, String.class)
        .setDefault(GenericDeclaration.class, Types.class)
        .testStaticMethods(Types.class, Visibility.PACKAGE);
  }
}
