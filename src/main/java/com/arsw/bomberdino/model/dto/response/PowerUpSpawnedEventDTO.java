package com.arsw.bomberdino.model.dto.response;

import com.arsw.bomberdino.model.enums.PowerUpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PowerUpSpawnedEventDTO {

    @NotBlank(message = "Power-up ID cannot be blank")
    private String powerUpId;

    @NotNull(message = "Power-up type cannot be null")
    private PowerUpType type;

    @NotNull(message = "X position cannot be null")
    private Integer x;

    @NotNull(message = "Y position cannot be null")
    private Integer y;

    @NotNull(message = "Sequence number cannot be null")
    private Long sequenceNumber;

    @NotNull(message = "Timestamp cannot be null")
    private Long timestamp;
}
