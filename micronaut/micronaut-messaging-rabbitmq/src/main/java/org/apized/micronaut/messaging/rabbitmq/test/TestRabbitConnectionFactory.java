package org.apized.micronaut.messaging.rabbitmq.test;

import com.rabbitmq.client.*;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.rabbitmq.connect.RabbitConnectionFactoryConfig;
import io.micronaut.rabbitmq.connect.SingleRabbitConnectionFactoryConfig;
import jakarta.inject.Named;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

@Requires(env = Environment.TEST)
@ConfigurationProperties("rabbitmq")
@Named(SingleRabbitConnectionFactoryConfig.DEFAULT_NAME)
@Replaces(SingleRabbitConnectionFactoryConfig.class)
public class TestRabbitConnectionFactory extends RabbitConnectionFactoryConfig {
  public static final String DEFAULT_NAME = "default";

  TestRabbitConnectionFactory() {
    super(DEFAULT_NAME);
  }

  @Override
  public Connection newConnection(ExecutorService executor, AddressResolver addressResolver, String clientProvidedName)
    throws IOException, TimeoutException {
    return new Connection() {
      @Override
      public InetAddress getAddress() {
        return null;
      }

      @Override
      public int getPort() {
        return 0;
      }

      @Override
      public int getChannelMax() {
        return 0;
      }

      @Override
      public int getFrameMax() {
        return 0;
      }

      @Override
      public int getHeartbeat() {
        return 0;
      }

      @Override
      public Map<String, Object> getClientProperties() {
        return null;
      }

      @Override
      public String getClientProvidedName() {
        return null;
      }

      @Override
      public Map<String, Object> getServerProperties() {
        return null;
      }

      @Override
      public Channel createChannel() throws IOException {
        return null;
      }

      @Override
      public Channel createChannel(int channelNumber) throws IOException {
        return null;
      }

      @Override
      public void close() throws IOException {

      }

      @Override
      public void close(int closeCode, String closeMessage) throws IOException {

      }

      @Override
      public void close(int timeout) throws IOException {

      }

      @Override
      public void close(int closeCode, String closeMessage, int timeout) throws IOException {

      }

      @Override
      public void abort() {

      }

      @Override
      public void abort(int closeCode, String closeMessage) {

      }

      @Override
      public void abort(int timeout) {

      }

      @Override
      public void abort(int closeCode, String closeMessage, int timeout) {

      }

      @Override
      public void addBlockedListener(BlockedListener listener) {

      }

      @Override
      public BlockedListener addBlockedListener(BlockedCallback blockedCallback, UnblockedCallback unblockedCallback) {
        return null;
      }

      @Override
      public boolean removeBlockedListener(BlockedListener listener) {
        return false;
      }

      @Override
      public void clearBlockedListeners() {

      }

      @Override
      public ExceptionHandler getExceptionHandler() {
        return null;
      }

      @Override
      public String getId() {
        return null;
      }

      @Override
      public void setId(String id) {

      }

      @Override
      public void addShutdownListener(ShutdownListener listener) {

      }

      @Override
      public void removeShutdownListener(ShutdownListener listener) {

      }

      @Override
      public ShutdownSignalException getCloseReason() {
        return null;
      }

      @Override
      public void notifyListeners() {

      }

      @Override
      public boolean isOpen() {
        return false;
      }
    };
  }
}
