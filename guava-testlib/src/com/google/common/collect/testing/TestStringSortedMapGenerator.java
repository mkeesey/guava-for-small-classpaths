/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing;

import static com.google.common.collect.testing.Helpers.orderEntriesByKey;

import java.util.List;
import java.util.Map.Entry;

/**
 * Implementation helper for {@link TestMapGenerator} for use with sorted maps of strings.
 *
 * <p>This class is GWT compatible.
 *
 * @author Chris Povirk
 */
public abstract class TestStringSortedMapGenerator extends TestStringMapGenerator {
  @Override
  public Iterable<Entry<String, String>> order(List<Entry<String, String>> insertionOrder) {
    return orderEntriesByKey(insertionOrder);
  }
}
