package com.arsw.bomberdino.model.dto.response;

import com.arsw.bomberdino.model.enums.Direction;
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
public class PlayerMovedEventDTO {

    @NotBlank(message = "Player ID cannot be blank")
    private String playerId;

    @NotNull(message = "New X position cannot be null")
    private Integer newX;

    @NotNull(message = "New Y position cannot be null")
    private Integer newY;

    @NotNull(message = "Direction cannot be null")
    private Direction direction;

    @NotNull(message = "Sequence number cannot be null")
    private Long sequenceNumber;

    @NotNull(message = "Timestamp cannot be null")
    private Long timestamp;

}
