/*
 * Copyright (C) 2008 Google Inc.
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

package com.google.common.collect;

import static java.util.Arrays.asList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Unit test for {@link FluentIterable}.
 *
 * @author Marcin Mikosik
 */
@GwtCompatible(emulated = true)
public class FluentIterableTest extends TestCase {

  @GwtIncompatible("NullPointerTester")
  public void testNullPointerExceptions() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(FluentIterable.class);
  }

  public void testFrom() {
    assertEquals(ImmutableList.of(1, 2, 3, 4),
        Lists.newArrayList(FluentIterable.from(ImmutableList.of(1, 2, 3, 4))));
  }

  public void testFrom_alreadyFluentIterable() {
    FluentIterable<Integer> iterable = FluentIterable.from(asList(1));
    assertSame(iterable, FluentIterable.from(iterable));
  }

  public void testSize1Collection() {
    assertEquals(1, FluentIterable.from(asList("a")).size());
  }

  public void testSize2NonCollection() {
    Iterable<Integer> iterable = new Iterable<Integer>() {
      @Override
      public Iterator<Integer> iterator() {
        return asList(0, 1).iterator();
      }
    };
    assertEquals(2, FluentIterable.from(iterable).size());
  }

  public void testSize_collectionDoesntIterate() {
    List<Integer> nums = asList(1, 2, 3, 4, 5);
    List<Integer> collection = new ArrayList<Integer>(nums) {
      @Override public Iterator<Integer> iterator() {
        fail("Don't iterate me!");
        return null;
      }
    };
    assertEquals(5, FluentIterable.from(collection).size());
  }

  public void testContains_nullSetYes() {
    Iterable<String> set = Sets.newHashSet("a", null, "b");
    assertTrue(FluentIterable.from(set).contains(null));
  }

  public void testContains_nullSetNo() {
    Iterable<String> set = ImmutableSortedSet.of("a", "b");
    assertFalse(FluentIterable.from(set).contains(null));
  }

  public void testContains_nullIterableYes() {
    Iterable<String> iterable = iterable("a", null, "b");
    assertTrue(FluentIterable.from(iterable).contains(null));
  }

  public void testContains_nullIterableNo() {
    Iterable<String> iterable = iterable("a", "b");
    assertFalse(FluentIterable.from(iterable).contains(null));
  }

  public void testContains_nonNullSetYes() {
    Iterable<String> set = Sets.newHashSet("a", null, "b");
    assertTrue(FluentIterable.from(set).contains("b"));
  }

  public void testContains_nonNullSetNo() {
    Iterable<String> set = Sets.newHashSet("a", "b");
    assertFalse(FluentIterable.from(set).contains("c"));
  }

  public void testContains_nonNullIterableYes() {
    Iterable<String> set = iterable("a", null, "b");
    assertTrue(FluentIterable.from(set).contains("b"));
  }

  public void testContains_nonNullIterableNo() {
    Iterable<String> iterable = iterable("a", "b");
    assertFalse(FluentIterable.from(iterable).contains("c"));
  }

  public void testCycle() {
    FluentIterable<String> cycle = FluentIterable.from(asList("a", "b")).cycle();

    int howManyChecked = 0;
    for (String string : cycle) {
      String expected = (howManyChecked % 2 == 0) ? "a" : "b";
      assertEquals(expected, string);
      if (howManyChecked++ == 5) {
        break;
      }
    }

    // We left the last iterator pointing to "b". But a new iterator should
    // always point to "a".
    for (String string : cycle) {
      assertEquals("a", string);
      break;
    }
  }

  public void testCycle_removingAllElementsStopsCycle() {
    FluentIterable<Integer> cycle = fluent(1, 2).cycle();
    Iterator<Integer> iterator = cycle.iterator();
    iterator.next();
    iterator.remove();
    iterator.next();
    iterator.remove();
    assertFalse(iterator.hasNext());
    assertFalse(cycle.iterator().hasNext());
  }

  /*
   * Tests for partition(int size) method.
   */

  /*
   * Tests for partitionWithPadding(int size) method.
   */

  public void testFilter() {
    FluentIterable<String> filtered =
        FluentIterable.from(asList("foo", "bar")).filter(Predicates.equalTo("foo"));

    List<String> expected = Collections.singletonList("foo");
    List<String> actual = Lists.newArrayList(filtered);
    assertEquals(expected, actual);
    assertCanIterateAgain(filtered);
    assertEquals("[foo]", filtered.toString());
  }

  private static class TypeA {}
  private interface TypeB {}
  private static class HasBoth extends TypeA implements TypeB {}

  @GwtIncompatible("Iterables.filter(Iterable, Class)")
  public void testFilterByType() throws Exception {
    HasBoth hasBoth = new HasBoth();
    FluentIterable<TypeA> alist =
        FluentIterable.from(asList(new TypeA(), new TypeA(), hasBoth, new TypeA()));
    Iterable<TypeB> blist = alist.filter(TypeB.class);
    ASSERT.that(blist).hasContentsInOrder(hasBoth);
  }

  public void testAnyMatch() {
    ArrayList<String> list = Lists.newArrayList();
    FluentIterable<String> iterable = FluentIterable.<String>from(list);
    Predicate<String> predicate = Predicates.equalTo("pants");

    assertFalse(iterable.anyMatch(predicate));
    list.add("cool");
    assertFalse(iterable.anyMatch(predicate));
    list.add("pants");
    assertTrue(iterable.anyMatch(predicate));
  }

  public void testAllMatch() {
    List<String> list = Lists.newArrayList();
    FluentIterable<String> iterable = FluentIterable.<String>from(list);
    Predicate<String> predicate = Predicates.equalTo("cool");

    assertTrue(iterable.allMatch(predicate));
    list.add("cool");
    assertTrue(iterable.allMatch(predicate));
    list.add("pants");
    assertFalse(iterable.allMatch(predicate));
  }

  public void testFirstMatch() {
    FluentIterable<String> iterable = FluentIterable.from(Lists.newArrayList("cool", "pants"));
    assertEquals(Optional.of("cool"), iterable.firstMatch(Predicates.equalTo("cool")));
    assertEquals(Optional.of("pants"), iterable.firstMatch(Predicates.equalTo("pants")));
    assertEquals(Optional.absent(), iterable.firstMatch(Predicates.alwaysFalse()));
    assertEquals(Optional.of("cool"), iterable.firstMatch(Predicates.alwaysTrue()));
  }

  private static final class IntegerValueOfFunction implements Function<String, Integer> {
    @Override
    public Integer apply(String from) {
      return Integer.valueOf(from);
    }
  }

  public void testTransformWith() {
    List<String> input = asList("1", "2", "3");
    Iterable<Integer> iterable =
        FluentIterable.from(input).transform(new IntegerValueOfFunction());

    assertEquals(asList(1, 2, 3), Lists.newArrayList(iterable));
    assertCanIterateAgain(iterable);
    assertEquals("[1, 2, 3]", iterable.toString());
  }

  public void testTransformWith_poorlyBehavedTransform() {
    List<String> input = asList("1", null, "3");
    Iterable<Integer> iterable =
        FluentIterable.from(input).transform(new IntegerValueOfFunction());

    Iterator<Integer> resultIterator = iterable.iterator();
    resultIterator.next();

    try {
      resultIterator.next();
      fail("Transforming null to int should throw NumberFormatException");
    } catch (NumberFormatException expected) {
    }
  }

  private static final class StringValueOfFunction implements Function<Integer, String> {
    @Override
    public String apply(Integer from) {
      return String.valueOf(from);
    }
  }

  public void testTransformWith_nullFriendlyTransform() {
    List<Integer> input = asList(1, 2, null, 3);
    Iterable<String> result = FluentIterable.from(input).transform(new StringValueOfFunction());

    assertEquals(asList("1", "2", "null", "3"), Lists.newArrayList(result));
  }

  public void testFirst_list() {
    List<String> list = Lists.newArrayList("a", "b", "c");
    assertEquals("a", FluentIterable.from(list).first().get());
  }

  public void testFirst_null() {
    List<String> list = Lists.newArrayList(null, "a", "b");
    try {
      FluentIterable.from(list).first();
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testFirst_emptyList() {
    List<String> list = Collections.emptyList();
    assertEquals(Optional.absent(), FluentIterable.from(list).first());
  }

  public void testFirst_sortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of("b", "c", "a");
    assertEquals("a", FluentIterable.from(sortedSet).first().get());
  }

  public void testFirst_emptySortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of();
    assertEquals(Optional.absent(), FluentIterable.from(sortedSet).first());
  }

  public void testFirst_iterable() {
    Set<String> set = ImmutableSet.of("a", "b", "c");
    assertEquals("a", FluentIterable.from(set).first().get());
  }

  public void testFirst_emptyIterable() {
    Set<String> set = Sets.newHashSet();
    assertEquals(Optional.absent(), FluentIterable.from(set).first());
  }

  public void testLast_list() {
    List<String> list = Lists.newArrayList("a", "b", "c");
    assertEquals("c", FluentIterable.from(list).last().get());
  }

  public void testLast_null() {
    List<String> list = Lists.newArrayList("a", "b", null);
    try {
      FluentIterable.from(list).last();
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testLast_emptyList() {
    List<String> list = Collections.emptyList();
    assertEquals(Optional.absent(), FluentIterable.from(list).last());
  }

  public void testLast_sortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of("b", "c", "a");
    assertEquals("c", FluentIterable.from(sortedSet).last().get());
  }

  public void testLast_emptySortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of();
    assertEquals(Optional.absent(), FluentIterable.from(sortedSet).last());
  }

  public void testLast_iterable() {
    Set<String> set = ImmutableSet.of("a", "b", "c");
    assertEquals("c", FluentIterable.from(set).last().get());
  }

  public void testLast_emptyIterable() {
    Set<String> set = Sets.newHashSet();
    assertEquals(Optional.absent(), FluentIterable.from(set).last());
  }

  public void testSkip_simple() {
    Collection<String> set = ImmutableSet.of("a", "b", "c", "d", "e");
    assertEquals(Lists.newArrayList("c", "d", "e"),
        Lists.newArrayList(FluentIterable.from(set).skip(2)));
    assertEquals("[c, d, e]", FluentIterable.from(set).skip(2).toString());
  }

  public void testSkip_simpleList() {
    Collection<String> list = Lists.newArrayList("a", "b", "c", "d", "e");
    assertEquals(Lists.newArrayList("c", "d", "e"),
        Lists.newArrayList(FluentIterable.from(list).skip(2)));
    assertEquals("[c, d, e]", FluentIterable.from(list).skip(2).toString());
  }

  public void testSkip_pastEnd() {
    Collection<String> set = ImmutableSet.of("a", "b");
    assertEquals(Collections.emptyList(), Lists.newArrayList(FluentIterable.from(set).skip(20)));
  }

  public void testSkip_pastEndList() {
    Collection<String> list = Lists.newArrayList("a", "b");
    assertEquals(Collections.emptyList(), Lists.newArrayList(FluentIterable.from(list).skip(20)));
  }

  public void testSkip_skipNone() {
    Collection<String> set = ImmutableSet.of("a", "b");
    assertEquals(Lists.newArrayList("a", "b"),
        Lists.newArrayList(FluentIterable.from(set).skip(0)));
  }

  public void testSkip_skipNoneList() {
    Collection<String> list = Lists.newArrayList("a", "b");
    assertEquals(Lists.newArrayList("a", "b"),
        Lists.newArrayList(FluentIterable.from(list).skip(0)));
  }

  public void testSkip_iterator() throws Exception {
    new IteratorTester<Integer>(5, IteratorFeature.MODIFIABLE, Lists.newArrayList(2, 3),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override protected Iterator<Integer> newTargetIterator() {
        Collection<Integer> collection = Sets.newLinkedHashSet();
        Collections.addAll(collection, 1, 2, 3);
        return FluentIterable.from(collection).skip(1).iterator();
      }
    }.test();
  }

  public void testSkip_iteratorList() throws Exception {
    new IteratorTester<Integer>(5, IteratorFeature.MODIFIABLE, Lists.newArrayList(2, 3),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override protected Iterator<Integer> newTargetIterator() {
        return FluentIterable.from(Lists.newArrayList(1, 2, 3)).skip(1).iterator();
      }
    }.test();
  }

  public void testSkip_nonStructurallyModifiedList() throws Exception {
    List<String> list = Lists.newArrayList("a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(list).skip(1);
    Iterator<String> tailIterator = tail.iterator();
    list.set(2, "c2");
    assertEquals("b", tailIterator.next());
    assertEquals("c2", tailIterator.next());
    assertFalse(tailIterator.hasNext());
  }

  public void testSkip_structurallyModifiedSkipSome() throws Exception {
    Collection<String> set = Sets.newLinkedHashSet();
    Collections.addAll(set, "a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(set).skip(1);
    set.remove("b");
    set.addAll(Lists.newArrayList("X", "Y", "Z"));
    ASSERT.that(tail).hasContentsInOrder("c", "X", "Y", "Z");
  }

  public void testSkip_structurallyModifiedSkipSomeList() throws Exception {
    List<String> list = Lists.newArrayList("a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(list).skip(1);
    list.subList(1, 3).clear();
    list.addAll(0, Lists.newArrayList("X", "Y", "Z"));
    ASSERT.that(tail).hasContentsInOrder("Y", "Z", "a");
  }

  public void testSkip_structurallyModifiedSkipAll() throws Exception {
    Collection<String> set = Sets.newLinkedHashSet();
    Collections.addAll(set, "a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(set).skip(2);
    set.remove("a");
    set.remove("b");
    assertFalse(tail.iterator().hasNext());
  }

  public void testSkip_structurallyModifiedSkipAllList() throws Exception {
    List<String> list = Lists.newArrayList("a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(list).skip(2);
    list.subList(0, 2).clear();
    ASSERT.that(tail).isEmpty();
  }

  public void testSkip_illegalArgument() {
    try {
      FluentIterable.from(asList("a", "b", "c")).skip(-1);
      fail("Skipping negative number of elements should throw IllegalArgumentException.");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testLimit() {
    Iterable<String> iterable = Lists.newArrayList("foo", "bar", "baz");
    FluentIterable<String> limited = FluentIterable.from(iterable).limit(2);

    assertEquals(ImmutableList.of("foo", "bar"), Lists.newArrayList(limited));
    assertCanIterateAgain(limited);
    assertEquals("[foo, bar]", limited.toString());
  }

  public void testLimit_illegalArgument() {
    try {
      FluentIterable.from(Lists.newArrayList("a", "b", "c")).limit(-1);
      fail("Passing negative number to limit(...) method should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testIsEmpty() {
    assertTrue(FluentIterable.<String>from(Collections.<String>emptyList()).isEmpty());
    assertFalse(FluentIterable.<String>from(Lists.newArrayList("foo")).isEmpty());
  }

  public void testToImmutableList() {
    assertEquals(Lists.newArrayList(1, 2, 3, 4), fluent(1, 2, 3, 4).toImmutableList());
  }

  public void testToImmutableList_empty() {
    assertTrue(fluent().toImmutableList().isEmpty());
  }

  public void testToImmutableSet() {
    ASSERT.that(fluent(1, 2, 3, 4).toImmutableSet()).hasContentsInOrder(1, 2, 3, 4);
  }

  public void testToImmutableSet_removeDuplicates() {
    ASSERT.that(fluent(1, 2, 1, 2).toImmutableSet()).hasContentsInOrder(1, 2);
  }

  public void testToImmutableSet_empty() {
    assertTrue(fluent().toImmutableSet().isEmpty());
  }

  public void testToImmutableSortedSet() {
    ASSERT.that(fluent(1, 4, 2, 3).toImmutableSortedSet(Ordering.<Integer>natural().reverse()))
        .hasContentsInOrder(4, 3, 2, 1);
  }

  public void testToImmutableSortedSet_removeDuplicates() {
    ASSERT.that(fluent(1, 4, 1, 3).toImmutableSortedSet(Ordering.<Integer>natural().reverse()))
        .hasContentsInOrder(4, 3, 1);
  }

  public void testGet() {
    assertEquals("a", FluentIterable
        .from(Lists.newArrayList("a", "b", "c")).get(0));
    assertEquals("b", FluentIterable
        .from(Lists.newArrayList("a", "b", "c")).get(1));
    assertEquals("c", FluentIterable
        .from(Lists.newArrayList("a", "b", "c")).get(2));
  }

  public void testGet_outOfBounds() {
    try {
      FluentIterable.from(Lists.newArrayList("a", "b", "c")).get(-1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      FluentIterable.from(Lists.newArrayList("a", "b", "c")).get(3);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  private static void assertCanIterateAgain(Iterable<?> iterable) {
    for (@SuppressWarnings("unused") Object obj : iterable) {
    }
  }

  private static FluentIterable<Integer> fluent(Integer... elements) {
    return FluentIterable.from(Lists.newArrayList(elements));
  }

  private static Iterable<String> iterable(String... elements) {
    final List<String> list = asList(elements);
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return list.iterator();
      }
    };
  }
}
