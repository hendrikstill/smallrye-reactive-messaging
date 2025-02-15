package io.smallrye.reactive.messaging.kafka.conflict;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.Merge;
import io.smallrye.reactive.messaging.kafka.base.KafkaMapBasedConfig;
import io.smallrye.reactive.messaging.kafka.base.KafkaTestBase;

/**
 * Reproducer for https://github.com/smallrye/smallrye-reactive-messaging/issues/373.
 */
public class ChannelNameConflictTest extends KafkaTestBase {

    KafkaMapBasedConfig conflictingConfig() {
        return kafkaConfig("mp.messaging.incoming.my-topic")
                // incoming my-topic
                .put("topic", "my-topic-1")
                .put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                // outgoing my-topic
                .withPrefix("mp.messaging.outgoing.my-topic")
                .put("topic", "my-topic-1")
                .put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    }

    @Test
    public void testWhenBothIncomingAndOutgoingUseTheSameName() {
        assertThatThrownBy(() -> runApplication(conflictingConfig(), Bean.class)).isInstanceOf(DeploymentException.class);
    }

    @ApplicationScoped
    public static class Bean {

        @Outgoing("my-topic")
        public Publisher<String> publisher() {
            return Multi.createFrom().item("0");
        }

        private final List<String> list = new CopyOnWriteArrayList<>();

        @Incoming("my-topic")
        @Merge
        public void consumer(String input) {
            list.add(input);
        }

        public List<String> getList() {
            return list;
        }
    }

}
