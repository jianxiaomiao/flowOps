package com.example.demo.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
/**
 * MyBatis-Plus 自动填充处理器，为标记了 FieldFill 的时间和版本字段赋值。
 */
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    /** @param metaObject MyBatis-Plus 正在插入的实体元对象。 */
    public void insertFill(MetaObject metaObject) {
        log.info("开始插入填充...");
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    /** @param metaObject MyBatis-Plus 正在更新的实体元对象。 */
    public void updateFill(MetaObject metaObject) {
        log.info("开始更新填充...");
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
