package com.example.demo.config;

import com.example.demo.messaging.RabbitTopology;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "flowops.messaging", name = "enabled", havingValue = "true")
/**
 * 声明 FlowOps 的 RabbitMQ 交换机、通知队列和死信队列。
 *
 * <p>仅当 {@code flowops.messaging.enabled=true} 时生效，便于本地未安装 RabbitMQ 时继续开发核心业务。</p>
 */
public class RabbitMqConfig {
    /**
     * @return 接收领域事件的持久化 Topic 交换机。
     */
    //Topic Exchange 可以根据“路由键”灵活分发不同事件。
    //durable(true) 表示持久化：RabbitMQ 重启后，这个交换机仍然存在。
    @Bean
    TopicExchange flowOpsExchange() {
        return ExchangeBuilder.topicExchange(RabbitTopology.EXCHANGE).durable(true).build();
    }

    /**
     * @return 接收消费失败消息的持久化死信交换机。
     */
    //当一条消息被消费者多次处理失败，并按配置拒绝或无法继续重试时，它不应该悄悄消失，而应该进入死信队列，等待人工排查或后续补偿。
    //这里使用 DirectExchange，因为死信只需要按一个固定路由键送去固定的异常队列，不需要 Topic 的复杂匹配能力。
    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(RabbitTopology.DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    /**
     * @return 用于消费工单通知事件、并配置死信去向的持久化队列。
     */
    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(RabbitTopology.NOTIFICATION_QUEUE)
                .deadLetterExchange(RabbitTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /**
     * @return 保存多次消费失败消息的持久化死信队列。
     */
    @Bean
    Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.DEAD_LETTER_QUEUE).build();
    }

    /**
     * 将 {@code ticket.#} 路由键的工单事件绑定到通知队列。
     *
     * @param notificationQueue 通知消费者监听的队列。
     * @param flowOpsExchange 发布工单领域事件的交换机。
     * @return 创建好的队列绑定关系。
     */
    @Bean
    Binding notificationBinding(Queue notificationQueue, TopicExchange flowOpsExchange) {
        return BindingBuilder.bind(notificationQueue).to(flowOpsExchange).with("ticket.#");
    }

    /**
     * 将死信交换机中的失败消息绑定到死信队列。
     *
     * @param notificationDeadLetterQueue 保存失败消息的队列。
     * @param deadLetterExchange 发布死信消息的交换机。
     * @return 创建好的死信队列绑定关系。
     */
    @Bean
    Binding deadLetterBinding(Queue notificationDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue).to(deadLetterExchange)
                .with(RabbitTopology.DEAD_LETTER_ROUTING_KEY);
    }
}
