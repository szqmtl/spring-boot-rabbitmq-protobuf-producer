package rshu.springboot.mq.mqserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rshu.springboot.mq.proto.ExchangeData;

@RestController
public class Sender {

    @Autowired
    AMQPProducer producer;

    @PostMapping("/send")
    public void sendMessage(@RequestParam String msg) {
//        try {
            for (int i = 0; i < 100; i++) {
                var n = new Notification(String.format("type-%d", i), String.format("%s-%d", msg, i));
//                var mapper = new ObjectMapper();
                var elem = ExchangeData.ExchangeDataElement.newBuilder()
                        .setId(i).setMessage(n.getMsg()).setType(n.getNotificationType()).build();
                producer.sendMessage(elem);
            }
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
    }
}
