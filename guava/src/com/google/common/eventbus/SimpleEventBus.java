/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.eventbus;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import com.google.common.annotations.Beta;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

@Beta
public class SimpleEventBus extends AbstractEventBus {
  private final SetMultimap<Class<?>, EventHandler> handlersByType = HashMultimap.create();
  private final WeakHashMap<Class<?>, Set<Class<?>>> flattenedHierarchies = new WeakHashMap<Class<?>, Set<Class<?>>>();
  private final FlattenedHierarchyCacheLoader flatHierarchyLoader = new FlattenedHierarchyCacheLoader();
  
  public SimpleEventBus() {
    super(new AnnotatedHandlerFinder());
  }

  @Override
  protected Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
    Set<Class<?>> hierarchy = flattenedHierarchies.get(concreteClass);
    if (hierarchy == null) {
      hierarchy = flatHierarchyLoader.load(concreteClass);
      flattenedHierarchies.put(concreteClass, hierarchy);
    }
    
    return hierarchy;
  }

  @Override
  protected SetMultimap<Class<?>, EventHandler> getHandlersByType() {
    return handlersByType;
  }

  @Override
  protected void enqueueEvent(Object event, EventHandler wrapper) {
    //no reason to queue them up in this implementation - just send now.
    dispatch(event, wrapper);
  }

  private void dispatch(Object event, EventHandler wrapper) {
    try {
      wrapper.handleEvent(event);
    } catch (InvocationTargetException e) {
      logger.log(Level.SEVERE,
          "Could not dispatch event: " + event + " to handler " + wrapper, e);
    }
  }

  @Override
  protected void dispatchQueuedEvents() {
    //pass
  }
}