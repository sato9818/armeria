/*
 * Copyright 2024 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server.grpc;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.curioswitch.common.protobuf.json.MessageMarshaller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;

import com.linecorp.armeria.common.annotation.Nullable;

/**
 * Constructs a {@link UnframedGrpcErrorHandler} to handle unframed gRPC errors.
 */
public final class UnframedGrpcErrorHandlerBuilder {
    private UnframedGrpcStatusMappingFunction statusMappingFunction = UnframedGrpcStatusMappingFunction.of();

    @Nullable
    private MessageMarshaller jsonMarshaller;

    private final List<Message> messages = new ArrayList<>();

    private final List<Class<? extends Message>> messageTypes = new ArrayList<>();

    @Nullable
    private Set<UnframedGrpcErrorResponseType> responseTypes;

    UnframedGrpcErrorHandlerBuilder() {}

    /**
     * Sets a custom JSON marshaller to be used by the error handler.
     *
     * <p>This method allows the caller to specify a custom JSON marshaller
     * for encoding the error responses. If messages or message types have
     * already been registered, calling this method will result in an
     * {@link IllegalArgumentException}. If nothing is specified,
     * {@link UnframedGrpcErrorHandlers#ERROR_DETAILS_MARSHALLER} is used as
     * default json marshaller.
     *
     * @param jsonMarshaller The custom JSON marshaller to use
     */
    public UnframedGrpcErrorHandlerBuilder jsonMarshaller(MessageMarshaller jsonMarshaller) {
        requireNonNull(jsonMarshaller, "jsonMarshaller");
        if (!messages.isEmpty() || !messageTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot set a custom JSON marshaller because one or more Message or MessageType instances" +
                    " have already been registered. To set a custom marshaller, ensure that no Message or" +
                    " MessageType registrations have been made before calling this method.");
        }
        this.jsonMarshaller = requireNonNull(jsonMarshaller, "jsonMarshaller");
        return this;
    }

    /**
     * Specifies the status mapping function to be used by the error handler.
     *
     * <p>This function determines how gRPC statuses are mapped to HTTP statuses
     * in the error response.
     *
     * @param statusMappingFunction The status mapping function
     */
    public UnframedGrpcErrorHandlerBuilder statusMappingFunction(
            UnframedGrpcStatusMappingFunction statusMappingFunction) {
        this.statusMappingFunction = requireNonNull(statusMappingFunction, "statusMappingFunction");
        return this;
    }

    /**
     * Specifies the response types that the error handler will support.
     *
     * <p>This method allows specifying one or more response types (e.g., JSON, PLAINTEXT)
     * that the error handler can produce. If nothing is specified, the error handler will
     * support JSON and PLAINTEXT.
     *
     * @param responseTypes The response types to support
     */
    public UnframedGrpcErrorHandlerBuilder responseTypes(UnframedGrpcErrorResponseType... responseTypes) {
        requireNonNull(responseTypes, "responseTypes");
        if (this.responseTypes == null) {
            this.responseTypes = EnumSet.noneOf(UnframedGrpcErrorResponseType.class);
        }
        this.responseTypes.addAll(ImmutableSet.copyOf(responseTypes));
        return this;
    }

    /**
     * Registers custom messages to be marshalled by the error handler.
     *
     * <p>This method registers specific message instances for custom error responses.
     * If a custom JSON marshaller has already been set, calling this method will
     * result in an {@link IllegalArgumentException}.
     *
     * @param messages The message instances to register
     */
    public UnframedGrpcErrorHandlerBuilder registerMarshalledMessages(Message... messages) {
        requireNonNull(messages, "messages");
        if (jsonMarshaller != null) {
            throw new IllegalArgumentException(
                    "Cannot register custom messages because a custom JSON marshaller has already been set. " +
                    "Use the custom marshaller to register custom messages.");
        }
        this.messages.addAll(ImmutableList.copyOf(messages));
        return this;
    }

    /**
     * Registers custom message types to be marshalled by the error handler.
     *
     * <p>This method registers specific message types for custom error responses.
     * If a custom JSON marshaller has already been set, calling this method will
     * result in an {@link IllegalArgumentException}.
     *
     * @param messageTypes The message types to register
     */
    @SafeVarargs
    public final UnframedGrpcErrorHandlerBuilder registerMarshalledMessages(
            Class<? extends Message>... messageTypes) {
        requireNonNull(messageTypes, "messageTypes");
        if (jsonMarshaller != null) {
            throw new IllegalArgumentException(
                    "Cannot register custom messageTypes because a custom JSON marshaller has already been " +
                    "set. Use the custom marshaller to register custom message types.");
        }
        this.messageTypes.addAll(ImmutableList.copyOf(messageTypes));
        return this;
    }

    /**
     * Registers custom messages or message types to be marshalled by the error handler.
     *
     * <p>This method allows registering either message instances or message types for
     * custom error responses. If a custom JSON marshaller has already been set,
     * calling this method will result in an {@link IllegalArgumentException}.
     *
     * @param messagesOrMessageTypes The messages or message types to register
     */
    public UnframedGrpcErrorHandlerBuilder registerMarshalledMessages(Iterable<?> messagesOrMessageTypes) {
        requireNonNull(messagesOrMessageTypes, "messagesOrMessageTypes");
        if (jsonMarshaller != null) {
            throw new IllegalArgumentException(
                    "Cannot register custom messagesOrMessageTypes because a custom JSON marshaller has " +
                    "already been set. Use the custom marshaller to register custom messages or message types."
            );
        }

        for (final Object messageOrMessageType : messagesOrMessageTypes) {
            requireNonNull(messageOrMessageType, "messagesOrMessageTypes contains null.");
            if (messageOrMessageType instanceof Message) {
                messages.add((Message) messageOrMessageType);
            } else if (messageOrMessageType instanceof Class &&
                       Message.class.isAssignableFrom((Class<?>) messageOrMessageType)) {
                messageTypes.add((Class<? extends Message>) messageOrMessageType);
            } else {
                final String className =
                        messageOrMessageType instanceof Class ? ((Class<?>) messageOrMessageType).getName()
                                                              : messageOrMessageType.getClass().getName();
                throw new IllegalArgumentException(
                        className + " is neither Message class nor Message subclass.");
            }
        }

        return this;
    }

    /**
     * Builds and returns an instance of {@link UnframedGrpcErrorHandler}.
     *
     * <p>This method constructs a new {@code UnframedGrpcErrorHandler} with the
     * current configuration of this builder.
     */
    public UnframedGrpcErrorHandler build() {
        if (jsonMarshaller == null) {
            jsonMarshaller = UnframedGrpcErrorHandlers.ERROR_DETAILS_MARSHALLER;
            MessageMarshaller.Builder builder = jsonMarshaller.toBuilder();

            for (final Message message : messages) {
                builder = builder.register(message);
            }

            for (final Class<? extends Message> messageType : messageTypes) {
                builder = builder.register(messageType);
            }

            jsonMarshaller = builder.build();
        }
        if (responseTypes == null) {
            return UnframedGrpcErrorHandlers.of(statusMappingFunction, jsonMarshaller);
        }
        if (responseTypes.equals(ImmutableSet.of(UnframedGrpcErrorResponseType.JSON))) {
            return UnframedGrpcErrorHandlers.ofJson(statusMappingFunction, jsonMarshaller);
        }
        if (responseTypes.equals(ImmutableSet.of(UnframedGrpcErrorResponseType.PLAINTEXT))) {
            return UnframedGrpcErrorHandlers.ofPlaintext(statusMappingFunction);
        }
        return UnframedGrpcErrorHandlers.of(statusMappingFunction, jsonMarshaller);
    }
}
