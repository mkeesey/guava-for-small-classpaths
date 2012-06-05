package com.google.common.eventbus;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Beta
public abstract class AbstractEventBus {
  /**
   * Strategy for finding handler methods in registered objects.  Currently,
   * only the {@link AnnotatedHandlerFinder} is supported, but this is
   * encapsulated for future expansion.
   */
  private final HandlerFindingStrategy finder;
  
  protected abstract Set<Class<?>> flattenHierarchy(Class<?> concreteClass);
  protected abstract SetMultimap<Class<?>, EventHandler> getHandlersByType();
  protected abstract void enqueueEvent(Object event, EventHandler handler);
  protected abstract void dispatchQueuedEvents();
  
  public AbstractEventBus(HandlerFindingStrategy findingStrategy) {
    finder = findingStrategy;
  }
  
  public void post(Object event) {
    Set<Class<?>> dispatchTypes = flattenHierarchy(event.getClass());

    boolean dispatched = false;
    for (Class<?> eventType : dispatchTypes) {
      Set<EventHandler> wrappers = getHandlersForEventType(eventType);

      if (wrappers != null && !wrappers.isEmpty()) {
        dispatched = true;
        for (EventHandler wrapper : wrappers) {
          enqueueEvent(event, wrapper);
        }
      }
    }

    if (!dispatched && !(event instanceof DeadEvent)) {
      post(new DeadEvent(this, event));
    }

    dispatchQueuedEvents();
  }
  
  public void register(Object object) {
    getHandlersByType().putAll(finder.findAllHandlers(object));
  }
  
  public void unregister(Object object) {
    Multimap<Class<?>, EventHandler> handlersForObject = finder.findAllHandlers(object);
    for (Entry<Class<?>, Collection<EventHandler>> entry : handlersForObject.asMap().entrySet()) {
      Set<EventHandler> currentHandlers = getHandlersForEventType(entry.getKey());
      Collection<EventHandler> eventMethodsInListener = entry.getValue();
      
      if (currentHandlers == null || !currentHandlers.containsAll(entry.getValue())) {
        throw new IllegalArgumentException(
            "missing event handler for an annotated method. Is " + object + " registered?");
      }
      currentHandlers.removeAll(eventMethodsInListener);
    }
  }
  
  private Set<EventHandler> getHandlersForEventType(Class<?> type) {
    return getHandlersByType().get(type);
  }
}
