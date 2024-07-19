package org.apized.micronaut.lock;

import io.micronaut.transaction.TransactionOperations;
import jakarta.inject.Singleton;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.micronaut.MicronautJdbcLockProvider;
import org.apized.core.error.exception.ServerException;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Singleton
public class ShedLockLockFactory implements LockFactory {
  private final DefaultLockingTaskExecutor executor;

  public ShedLockLockFactory(TransactionOperations<Connection> transactionManager) {
    executor = new DefaultLockingTaskExecutor(new MicronautJdbcLockProvider(transactionManager));
  }

  @Override
  public void executeWithLock(String name, LockingTaskExecutor.Task task) {
    this.executeWithLock(name, false, Duration.ZERO, Duration.ZERO, task);
  }

  @Override
  public void executeWithLockRetry(String name, Duration retryFor, LockingTaskExecutor.Task task) {
    this.executeWithLock(name, true, Duration.ZERO, retryFor, task);
  }

  @Override
  @SuppressWarnings("BusyWait")
  public void executeWithLock(String name, boolean retry, Duration atLeastFor, Duration atMostFor, LockingTaskExecutor.Task task) {
    LockConfiguration lockConfig = new LockConfiguration(
      Instant.now(),
      name,
      atMostFor,
      atLeastFor
    );

    Optional<LockingTaskExecutor.TaskResult<?>> taskResult = Optional.empty();
    try {
      do {
        if (taskResult.isPresent()) {
          Thread.sleep(10);
        }
        taskResult = Optional.of(executor.executeWithLock(() -> {
          task.call();
          return true;
        }, lockConfig));
      } while (!taskResult.get().wasExecuted() && retry && Instant.now().isBefore(lockConfig.getLockAtMostUntil()));

      if (!taskResult.get().wasExecuted() && retry) {
        throw new ServerException(String.format("Unable to obtain lock '%s'", name));
      }
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
