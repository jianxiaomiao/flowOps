package com.example.demo.ticket;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.common.BusinessException;
import com.example.demo.entity.TicketNumberSequence;
import com.example.demo.mapper.TicketNumberSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** 用乐观锁取得租户每日递增编号，避免“先查后改”导致的重复工单号。 */
@Component
@RequiredArgsConstructor
public class TicketNumberGenerator {
    private final TicketNumberSequenceMapper sequenceMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextTicketNo(Long tenantId) {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        for (int attempt = 0; attempt < 3; attempt++) {
            TicketNumberSequence sequence = sequenceMapper.selectOne(Wrappers.lambdaQuery(TicketNumberSequence.class)
                    .eq(TicketNumberSequence::getTenantId, tenantId).eq(TicketNumberSequence::getSequenceDate, date));
            if (sequence == null) {
                TicketNumberSequence created = new TicketNumberSequence();
                created.setTenantId(tenantId); created.setSequenceDate(date); created.setCurrentValue(1);
                try {
                    sequenceMapper.insert(created);
                    return format(date, 1);
                } catch (DuplicateKeyException ignored) {
                    // 另一条并发请求已创建当天序列，重新读取即可。
                }
            } else {
                int next = sequence.getCurrentValue() + 1;
                sequence.setCurrentValue(next);
                if (sequenceMapper.updateById(sequence) == 1) return format(date, next);
            }
        }
        throw new BusinessException(30002, "工单编号生成发生并发冲突，请重试");
    }

    private String format(LocalDate date, int value) {
        return "OPS-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", value);
    }
}
