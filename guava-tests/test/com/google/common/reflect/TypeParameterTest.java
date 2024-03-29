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

import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * Unit test for {@link TypeParameter}.
 *
 * @author Ben Yu
 */
public class TypeParameterTest extends TestCase {

  public <T> void testCaptureTypeParameter() throws Exception {
    TypeVariable<?> variable = new TypeParameter<T>() {}.typeVariable;
    TypeVariable<?> expected = TypeParameterTest.class
        .getDeclaredMethod("testCaptureTypeParameter")
        .getTypeParameters()[0];
    assertEquals(expected, variable);
  }

  public void testConcreteTypeRejected() {
    try {
      new TypeParameter<String>() {};
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(TypeVariable.class, Types.newTypeVariable(List.class, "E"));
    tester.testAllPublicStaticMethods(TypeParameter.class);
  }
}
