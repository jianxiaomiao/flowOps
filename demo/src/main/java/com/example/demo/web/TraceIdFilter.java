package com.example.demo.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
/**
 * 为每个 HTTP 请求提供可跨日志检索的 traceId。
 *
 * <p>若客户端提供合法 {@code X-Trace-Id} 则沿用；否则创建新值，并写回响应头。</p>
 */
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    /**
     * 在请求进入应用前写入 MDC，并在请求结束后清理，避免线程复用时串号。
     *
     * @param request 当前 HTTP 请求。
     * @param response 当前 HTTP 响应，用于写回 traceId。
     * @param filterChain 后续过滤器和 Controller 组成的执行链。
     * @throws ServletException 下游 Servlet 处理失败时可能抛出。
     * @throws IOException 下游读取或写入 HTTP 内容失败时可能抛出。
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //读取客户端传来的链路 ID
        //HEADER_NAME 是 X-Trace-Id。例如客户端请求：
        //GET /ticket/1001
        //X-Trace-Id: web-20260719-abc
        //Authorization: Bearer ...
        //这里的 incoming 就是 web-20260719-abc。
        //这样做的价值是：如果请求经过网关、前端、多个系统，大家可沿用同一个 ID，排查日志时可以串起整条链路。
        String incoming = request.getHeader(HEADER_NAME);
        //判断条件有两个：
        //请求头存在；
        //值符合正则 [A-Za-z0-9-]{1,64}，即仅允许字母、数字、连字符，长度 1～64。
        //客户端传入非法值时，例如带换行符、超长字符串，系统不会原样写入日志，而是生成 UUID。这能限制日志污染和日志注入风险。
        String traceId = incoming != null && SAFE_TRACE_ID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString();
        //MDC 可以理解为“当前执行线程专用的日志附加信息盒子”。
        //假设日志格式配置中包含 %X{traceId}，之后同一请求里的业务日志：
        //log.info("创建工单成功");
        //就会自动变为类似：
        //2026-07-19 10:30:00 INFO [traceId=web-20260719-abc] 创建工单成功
        //因此 Controller、Service、Mapper 不需要层层传递 traceId 参数。
        MDC.put(MDC_KEY, traceId);
        //写回响应头
        response.setHeader(HEADER_NAME, traceId);
        try {
            //放行后续请求链
            //这句是 Filter 最核心的“继续往下走”。
            //后续会继续经过其他 Filter，例如 Spring Security，然后到 Controller、Service、数据库访问，最后返回响应。
            //如果不调用这句，请求就会停在 TraceIdFilter，Controller 永远不会执行。
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
