package org.apized.spring.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.apized.core.event.ESBAdapter
import org.apized.spring.test.integration.mocks.AbstractServiceIntegrationMock
import org.springframework.stereotype.Component

import java.time.LocalDateTime

@Component
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
