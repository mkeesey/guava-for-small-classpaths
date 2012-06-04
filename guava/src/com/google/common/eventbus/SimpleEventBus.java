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
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.common.annotations.Beta;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Beta
public class SimpleEventBus {
  private final HandlerFindingStrategy finder;
  private final SetMultimap<Class<?>, EventHandler> handlersByType;
  //private LoadingCache<Class<?>, Set<Class<?>>> flattenHierarchyCache;
  private WeakHashMap<Class<?>, Set<Class<?>>> flattenedHierarchies;
  FlattenedHierarchyCacheLoader flatHierarchyLoader;
  
  public SimpleEventBus() {
    finder = new AnnotatedHandlerFinder();
    handlersByType = HashMultimap.create();
    /*flattenHierarchyCache = CacheBuilder.newBuilder()
        .weakKeys()
        .build(new FlattenedHierarchyCacheLoader());*/
    flattenedHierarchies = new WeakHashMap<Class<?>, Set<Class<?>>>();
    flatHierarchyLoader = new FlattenedHierarchyCacheLoader();
  }
  
  public void post(Object event) {
    Set<Class<?>> dispatchTypes = flattenHierarchy(event.getClass());
    boolean dispatched = false;
    for (Class<?> eventType : dispatchTypes) {
      Set<EventHandler> wrappers = getHandlersForEventType(eventType);

      if (wrappers != null && !wrappers.isEmpty()) {
        dispatched = true;
        for (EventHandler wrapper : wrappers) {
          dispatch(event, wrapper);
        }
      }
    }

    if (!dispatched && !(event instanceof DeadEvent)) {
      post(new DeadEvent(this, event));
    }
  }
  
  public void register(Object toRegister) {
    handlersByType.putAll(finder.findAllHandlers(toRegister));
  }
  
  public void unregister(Object toUnregister) {
    Multimap<Class<?>, EventHandler> handlersForObject = finder.findAllHandlers(toUnregister);
    for (Entry<Class<?>, Collection<EventHandler>> entry : handlersForObject.asMap().entrySet()) {
      Set<EventHandler> currentHandlers = getHandlersForEventType(entry.getKey());
      Collection<EventHandler> eventMethodsInListener = entry.getValue();
      
      if (currentHandlers == null || !currentHandlers.containsAll(entry.getValue())) {
        throw new IllegalArgumentException(
            "missing event handler for an annotated method. Is " + toUnregister + " registered?");
      }
      currentHandlers.removeAll(eventMethodsInListener);
    }
  }
  
  Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
    Set<Class<?>> hierarchy = flattenedHierarchies.get(concreteClass);
    if (hierarchy == null) {
      hierarchy = flatHierarchyLoader.load(concreteClass);
      flattenedHierarchies.put(concreteClass, hierarchy);
    }
    
    return hierarchy;
  }
  
  Set<EventHandler> getHandlersForEventType(Class<?> type) {
    return handlersByType.get(type);
  }
  
  protected void dispatch(Object event, EventHandler wrapper) {
    try {
      wrapper.handleEvent(event);
    } catch (InvocationTargetException e) {
      //TODO
      //logger.log(Level.SEVERE,
      //    "Could not dispatch event: " + event + " to handler " + wrapper, e);
    }
  }
}
