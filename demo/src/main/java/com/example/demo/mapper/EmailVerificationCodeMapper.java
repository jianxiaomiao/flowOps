package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.EmailVerificationCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCode> {

    @Update("""
            UPDATE email_verification_code
            SET status = 'VERIFIED', verified_at = #{verifiedAt}, updated_at = #{verifiedAt}
            WHERE id = #{id} AND status = 'PENDING' AND expires_at > #{verifiedAt}
            """)
    int markVerifiedIfPending(@Param("id") Long id, @Param("verifiedAt") LocalDateTime verifiedAt);

    @Update("""
            UPDATE email_verification_code
            SET status = 'REPLACED', updated_at = #{updatedAt}
            WHERE email = #{email} AND purpose = #{purpose} AND status = 'PENDING'
            """)
    int replacePendingByEmailAndPurpose(@Param("email") String email,
                                        @Param("purpose") String purpose,
                                        @Param("updatedAt") LocalDateTime updatedAt);
}
