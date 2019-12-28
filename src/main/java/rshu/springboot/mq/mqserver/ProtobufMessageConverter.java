package rshu.springboot.mq.mqserver;

import com.google.common.base.Preconditions;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MessageLite;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import rshu.springboot.mq.proto.ExchangeData;

import java.util.HashMap;
import java.util.Map;

public final class ProtobufMessageConverter extends AbstractMessageConverter {

    private final static String MESSAGE_TYPE_NAME = "_msg_type_name_";
    private final static String CONTENT_TYPE_PROTOBUF = "application/x-backend-command";

    private Descriptors.FileDescriptor fileDescriptor;

    public ProtobufMessageConverter(Descriptors.FileDescriptor fileDescriptor){
        this.fileDescriptor = fileDescriptor;
    }

    @Override
    protected Message createMessage(Object o, MessageProperties messageProperties) {
        Preconditions.checkNotNull(o, "Object to send is null!");

        if(!com.google.protobuf.Message.class.isAssignableFrom(o.getClass())){
            throw new MessageConversionException("Message wasn't a protobuf");
        }else{
            com.google.protobuf.Message protobuf = (com.google.protobuf.Message) o;
            byte[] byteArray = protobuf.toByteArray();

            messageProperties.setContentLength(byteArray.length);
            messageProperties.setContentType(ProtobufMessageConverter.CONTENT_TYPE_PROTOBUF);
            messageProperties.setHeader(ProtobufMessageConverter.MESSAGE_TYPE_NAME,
                    protobuf.getDescriptorForType().getName());

            return new Message(byteArray, messageProperties);
        }
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        com.google.protobuf.Message parseMessage = null;

        try {
            if(ProtobufMessageConverter.CONTENT_TYPE_PROTOBUF.equals(message.getMessageProperties().getContentType())) {
                String typeName = getMessageTypeName(message);
                var messageType = fileDescriptor.findMessageTypeByName(typeName);
                parseMessage = DynamicMessage.parseFrom(messageType, message.getBody());
            }
        }catch (Exception e){
            throw new AmqpRejectAndDontRequeueException(
                    String.format("Cannot convert, unknown message type %s", getMessageTypeName(message)));
        }
        return parseMessage;
    }

    private String getMessageTypeName(Message msg) {
        Map<String, Object> headers = msg.getMessageProperties().getHeaders();
        return Preconditions.checkNotNull(headers.get(ProtobufMessageConverter.MESSAGE_TYPE_NAME)).toString();
    }
}
