package com.provectus.kafka.ui.cluster.deserialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DynamicMessage;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class ProtobufFileRecordDeserializer implements RecordDeserializer {
    private final ProtobufSchema protobufSchema;
    private final ObjectMapper objectMapper;

    public ProtobufFileRecordDeserializer(Path protobufSchemaPath, String messageName, ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        final String schemaString = Files.lines(protobufSchemaPath).collect(Collectors.joining());
        this.protobufSchema = new ProtobufSchema(schemaString).copy(messageName);
    }

    @Override
    public Object deserialize(ConsumerRecord<Bytes, Bytes> record) {
        try {
            final DynamicMessage message = DynamicMessage.parseFrom(
                    protobufSchema.toDescriptor(),
                    new ByteArrayInputStream(record.value().get())
            );
            byte[] bytes = ProtobufSchemaUtils.toJson(message);
            return parseJson(bytes);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to parse record from topic " + record.topic(), e);
        }
    }

    private Object parseJson(byte[] bytes) throws IOException {
        return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
    }
}
