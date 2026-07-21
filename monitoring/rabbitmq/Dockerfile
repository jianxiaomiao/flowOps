FROM rabbitmq:4.3.2-management-alpine

# RabbitMQ 4.x 自带 Prometheus 插件；离线启用避免容器启动时再下载插件。
RUN rabbitmq-plugins enable --offline rabbitmq_prometheus
