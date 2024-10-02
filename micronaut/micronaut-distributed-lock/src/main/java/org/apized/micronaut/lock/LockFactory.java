package org.apized.micronaut.lock;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;

import java.time.Duration;

public interface LockFactory {
  void executeWithLock(String name, LockingTaskExecutor.Task task);

  void executeWithLockRetry(String name, Duration retryFor, LockingTaskExecutor.Task task);

  void executeWithLock(String name, boolean retry, Duration atLeastFor, Duration atMostFor, LockingTaskExecutor.Task task);
}
