package com.example.demo.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class TicketCommentResponse {
    private Long id; private Long authorId; private String content; private String visibility;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createdAt;
}
