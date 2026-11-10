package com.studywithus.social.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "O comentário não pode ser vazio")
        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres")
        String content
) {
}
