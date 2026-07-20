package com.example.demo.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TicketCommentCreateRequest {
    @NotBlank private String content;
    @NotBlank private String visibility;
}
