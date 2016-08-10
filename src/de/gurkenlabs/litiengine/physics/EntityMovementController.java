package de.gurkenlabs.litiengine.physics;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import de.gurkenlabs.litiengine.IGameLoop;
import de.gurkenlabs.litiengine.entities.IMovableEntity;
import de.gurkenlabs.util.geom.GeometricUtilities;

public class EntityMovementController implements IEntityMovementController {
  private final List<Predicate<IEntityMovementController>> movementPredicates;
  private final List<Force> activeForces;
  private final IMovableEntity movableEntity;
  private final IPhysicsEngine engine;

  public EntityMovementController(final IGameLoop gameLoop, final IPhysicsEngine engine, final IMovableEntity movableEntity) {
    this.activeForces = new CopyOnWriteArrayList<>();
    this.movementPredicates = new CopyOnWriteArrayList<>();
    this.movableEntity = movableEntity;
    this.engine = engine;
    gameLoop.registerForUpdate(this);
  }

  @Override
  public void apply(final Force force) {
    if (!this.activeForces.contains(force)) {
      this.activeForces.add(force);
    }
  }

  @Override
  public List<Force> getActiceForces() {
    return this.activeForces;
  }

  @Override
  public IMovableEntity getControlledEntity() {
    return this.movableEntity;
  }

  protected IPhysicsEngine getPhysicsEngine() {
    return this.engine;
  }

  private void handleForces(final IGameLoop gameLoop) {
    final double ACCEPTABLE_DIST = 5;
    // clean up forces
    this.activeForces.forEach(x -> {
      if (x.hasEnded()) {
        this.activeForces.remove(x);
      }
    });

    // apply all forces
    // TODO: calculate the diff of all forces combined and only move the entity
    // once
    for (final Force force : this.activeForces) {
      if (force.cancelOnReached() && force.hasReached(this.getControlledEntity())) {
        force.end();
        continue;
      }

      final Point2D collisionBoxCenter = new Point2D.Double(this.getControlledEntity().getCollisionBox().getCenterX(), this.getControlledEntity().getCollisionBox().getCenterY());
      if (collisionBoxCenter.distance(force.getLocation()) < ACCEPTABLE_DIST) {
        final double yDelta = this.getControlledEntity().getHeight() - this.getControlledEntity().getCollisionBox().getHeight() + this.getControlledEntity().getCollisionBox().getHeight() / 2;
        final Point2D entityLocation = new Point2D.Double(force.getLocation().getX() - this.getControlledEntity().getWidth() / 2, force.getLocation().getY() - yDelta);
        this.getControlledEntity().setLocation(entityLocation);
      } else {
        final double angle = GeometricUtilities.calcRotationAngleInDegrees(collisionBoxCenter, force.getLocation());
        final boolean success = this.getPhysicsEngine().move(this.getControlledEntity(), (float) angle, gameLoop.getDeltaTime() * 0.001f * force.getStrength());
        if (force.cancelOnCollision() && !success) {
          force.end();
        }
      }
    }
  }

  protected boolean isMovementAllowed() {
    for (final Predicate<IEntityMovementController> predicate : this.movementPredicates) {
      if (!predicate.test(this)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void onMovementCheck(final Predicate<IEntityMovementController> predicate) {
    if (!this.movementPredicates.contains(predicate)) {
      this.movementPredicates.add(predicate);
    }
  }

  @Override
  public void update(final IGameLoop gameLoop) {
    this.handleForces(gameLoop);
  }
}
