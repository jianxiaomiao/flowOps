package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.ProcessedEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessedEventMapper extends BaseMapper<ProcessedEvent> {
    @Insert("""
            INSERT IGNORE INTO processed_event(tenant_id, event_id, consumer_name, processed_at)
            VALUES(#{tenantId}, #{eventId}, #{consumerName}, #{processedAt})
            """)
    int insertIfAbsent(ProcessedEvent event);
}
