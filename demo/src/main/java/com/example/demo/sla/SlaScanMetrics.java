package com.example.demo.sla;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SLA 定时扫描的业务指标。
 *
 * <p>HTTP 指标只能说明接口是否可用；这里补充“扫描是否最近成功完成”和“扫描/单工单是否失败”，
 * 让告警能覆盖没有直接 HTTP 请求的后台任务。</p>
 */
@Component
public class SlaScanMetrics {
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();
    private final Counter scanFailures;
    private final Counter ticketFailures;
    private final Timer scanDuration;
    private final MeterRegistry meterRegistry;

    public SlaScanMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.scanFailures = Counter.builder("flowops.sla.scan.failures")
                .description("SLA scan failures before all due tickets can be processed")
                .tag("stage", "scan")
                .register(meterRegistry);
        this.ticketFailures = Counter.builder("flowops.sla.scan.failures")
                .description("SLA scan failures before all due tickets can be processed")
                .tag("stage", "ticket")
                .register(meterRegistry);
        this.scanDuration = Timer.builder("flowops.sla.scan.duration")
                .description("Duration of one SLA scan")
                .register(meterRegistry);
        Gauge.builder("flowops.sla.scan.last.success.unixtime", lastSuccessEpochSeconds, AtomicLong::get)
                .description("Unix timestamp of the latest successful SLA scan")
                .strongReference(true)
                .register(meterRegistry);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(scanDuration);
    }

    public void recordSuccess() {
        lastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
    }

    public void recordScanFailure() {
        scanFailures.increment();
    }

    public void recordTicketFailure() {
        ticketFailures.increment();
    }
}
