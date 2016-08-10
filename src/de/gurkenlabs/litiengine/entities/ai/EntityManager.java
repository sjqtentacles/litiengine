package de.gurkenlabs.litiengine.entities.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.gurkenlabs.litiengine.entities.IEntity;

public class EntityManager {
  private final Map<IEntity, IEntityController<? extends IEntity>> entityControllers;

  public EntityManager() {
    this.entityControllers = new ConcurrentHashMap<>();
  }

  public <T extends IEntity> void addController(final T entity, final IEntityController<T> controller) {
    this.entityControllers.put(entity, controller);
  }

  public IEntityController<? extends IEntity> getController(final IEntity entity) {
    if (this.entityControllers.containsKey(entity)) {
      return this.entityControllers.get(entity);
    }

    return null;
  }
}