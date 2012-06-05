package com.google.common.eventbus;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

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
  
  /**
   * Logger for event dispatch failures.  Named by the identifier in the 
   * constructor, or "default" if none is provided.
   */
  protected final Logger logger;
  
  protected abstract Set<Class<?>> flattenHierarchy(Class<?> concreteClass);
  protected abstract SetMultimap<Class<?>, EventHandler> getHandlersByType();
  protected abstract void enqueueEvent(Object event, EventHandler handler);
  protected abstract void dispatchQueuedEvents();
  
  public AbstractEventBus(HandlerFindingStrategy findingStrategy) {
    this(findingStrategy, AbstractEventBus.class.getName() + "." + "default");
  }
  
  public AbstractEventBus(HandlerFindingStrategy findingStrategy, String identifier) {
    finder = findingStrategy;
    logger = Logger.getLogger(identifier);
  }
  
  /**
   * Posts an event to all registered handlers.  This method will return
   * successfully after the event has been posted to all handlers, and
   * regardless of any exceptions thrown by handlers.
   *
   * <p>If no handlers have been subscribed for {@code event}'s class, and
   * {@code event} is not already a {@link DeadEvent}, it will be wrapped in a
   * DeadEvent and reposted.
   *
   * @param event  event to post.
   */
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
  
  /**
   * Registers all handler methods on {@code object} to receive events.
   * Handler methods are selected and classified using this EventBus's
   * {@link HandlerFindingStrategy}; the default strategy is the
   * {@link AnnotatedHandlerFinder}.
   *
   * @param object  object whose handler methods should be registered.
   */
  public void register(Object object) {
    getHandlersByType().putAll(finder.findAllHandlers(object));
  }
  
  /**
   * Unregisters all handler methods on a registered {@code object}.
   *
   * @param object  object whose handler methods should be unregistered.
   * @throws IllegalArgumentException if the object was not previously registered.
   */
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
  
  /**
   * Retrieves a mutable set of the currently registered handlers for
   * {@code type}.  If no handlers are currently registered for {@code type},
   * this method may either return {@code null} or an empty set.
   *
   * @param type  type of handlers to retrieve.
   * @return currently registered handlers, or {@code null}.
   */
  private Set<EventHandler> getHandlersForEventType(Class<?> type) {
    return getHandlersByType().get(type);
  }
}
