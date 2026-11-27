package com.example.demo.service.impl;

import com.example.demo.entity.EmailVerificationCode;
import com.example.demo.mapper.EmailVerificationCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 在独立事务中持久化验证码输错次数与过期/锁定状态，避免外层业务异常回滚导致防爆破失效。 */
@Component
@RequiredArgsConstructor
public class EmailVerificationAttemptRecorder {
    private final EmailVerificationCodeMapper codeMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStatus(EmailVerificationCode record, String status, LocalDateTime now) {
        record.setStatus(status);
        record.setUpdatedAt(now);
        codeMapper.updateById(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(EmailVerificationCode record, int maxAttempts, LocalDateTime now) {
        int current = record.getFailedAttempts() == null ? 0 : record.getFailedAttempts();
        int updated = current + 1;
        record.setFailedAttempts(updated);
        if (updated >= maxAttempts) {
            record.setStatus(EmailVerificationServiceImpl.STATUS_LOCKED);
        }
        record.setUpdatedAt(now);
        codeMapper.updateById(record);
        return updated;
    }
}
