package com.example.demo.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditOperation {
    String module();
    String operation();
    String resourceType();
}
