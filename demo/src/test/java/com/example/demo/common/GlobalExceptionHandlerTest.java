package com.example.demo.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    @Test
    void shouldMapMethodAuthorizationDeniedToForbiddenBusinessResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleAccessDenied(
                new AuthorizationDeniedException("Access Denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10003, response.getBody().getCode());
        assertEquals("没有执行该操作的权限", response.getBody().getMsg());
        assertNull(response.getBody().getData());
    }
}
