package org.apized.micronaut.integration

import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import org.apized.core.event.ESBAdapter
import org.apized.micronaut.test.integration.mocks.AbstractServiceIntegrationMock

import java.time.LocalDateTime

@Singleton
class ESBAdapterMock extends AbstractServiceIntegrationMock implements ESBAdapter {
  @Override
  String getMockedServiceName() {
    'ESB'
  }

  ESBAdapterMock(ObjectMapper mapper) {
    super(mapper)
  }

  @Override
  void send(UUID messageId, LocalDateTime timestamp, String topic, Map<String, Object> headers, Object payload) {
    execute(
      'send',
      [
        messageId: messageId,
        timestamp: timestamp,
        topic: topic,
        headers: headers,
        payload: payload
      ],
      Void,
      () -> null
    )
  }
}
