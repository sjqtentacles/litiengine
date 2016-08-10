package de.gurkenlabs.litiengine.sound;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import de.gurkenlabs.litiengine.Game;

public class SoundController {
  public static class SoundPlayThread extends Thread {
    private boolean isRunning = true;
    private final Map<Consumer<ISoundEngine>, Long> reqTime = new ConcurrentHashMap<>();
    private final Queue<Consumer<ISoundEngine>> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(final Consumer<ISoundEngine> consumer, final boolean force) {
      this.queue.add(consumer);
      if (!force) {
        this.reqTime.put(consumer, Game.getLoop().getTicks());
      }
    }

    public boolean isRunning() {
      return this.isRunning;
    }

    @Override
    public void run() {
      while (this.isRunning) {
        while (this.queue.peek() != null) {
          final Consumer<ISoundEngine> consumer = this.queue.poll();
          if (this.reqTime.containsKey(consumer)) {
            if (Game.getLoop().getDeltaTime(this.reqTime.get(consumer)) < 500) {
              consumer.accept(Game.getSoundEngine());
            }

            this.reqTime.remove(consumer);
          } else {
            consumer.accept(Game.getSoundEngine());
          }
        }
        try {
          Thread.sleep(20);
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    public void setRunning(final boolean isRunning) {
      this.isRunning = isRunning;
    }

  }

  private static final int LOCK_TIME = 50;

  private static long lastPlay;

  private static final SoundPlayThread soundPlayThread = new SoundPlayThread();

  public static void call(final Consumer<ISoundEngine> engine) {
    if (canPlay()) {
      soundPlayThread.enqueue(engine, false);
      lastPlay = Game.getLoop().getTicks();
    }
  }

  public static void callIgnoreTimeout(final Consumer<ISoundEngine> engine, final boolean force) {
    soundPlayThread.enqueue(engine, force);
    lastPlay = Game.getLoop().getTicks();
  }

  public static boolean canPlay() {
    if (Game.getLoop().getDeltaTime(lastPlay) > LOCK_TIME) {
      return true;
    }

    return false;
  }

  public static void start() {
    soundPlayThread.start();
  }

  public static void terminate() {
    soundPlayThread.setRunning(false);
  }
}
