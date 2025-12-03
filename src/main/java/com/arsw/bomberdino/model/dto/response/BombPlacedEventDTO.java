package com.arsw.bomberdino.model.dto.response;

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

public class BombPlacedEventDTO {

    @NotBlank(message = "Bomb ID cannot be blank")
    private String bombId;

    @NotBlank(message = "Player ID cannot be blank")
    private String playerId;

    @NotNull(message = "X position cannot be null")
    private Integer x;

    @NotNull(message = "Y position cannot be null")
    private Integer y;

    @NotNull(message = "Bomb range cannot be null")
    private Integer range;

    @NotNull(message = "Time to explode cannot be null")
    private Long timeToExplode;

    @NotNull(message = "Sequence number cannot be null")
    private Long sequenceNumber;

    @NotNull(message = "Timestamp cannot be null")
    private Long timestamp;

}
