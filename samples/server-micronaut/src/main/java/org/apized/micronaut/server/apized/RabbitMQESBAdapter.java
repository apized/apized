/*
 * Copyright 2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apized.micronaut.server.apized;

import org.apized.core.event.ESBAdapter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import io.micronaut.rabbitmq.annotation.RabbitClient;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Map;

@RabbitClient
public class RabbitMQESBAdapter extends ChannelInitializer implements ESBAdapter {

  private static final String EXCHANGE = "micronaut";

  @Inject
  ChannelPool pool;

  @Inject
  ObjectMapper mapper;

  @Override
  public void initialize(Channel channel, String name) throws IOException {
    channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);
  }

  @Override
  public void send(String topic, Map<String, Object> payload) {
    try {
      pool.getChannel().basicPublish(
        EXCHANGE,
        topic,
        new AMQP.BasicProperties.Builder().build(),
        mapper.writeValueAsBytes(payload)
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
