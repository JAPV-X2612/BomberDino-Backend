package com.arsw.bomberdino.model.dto.response;

import com.arsw.bomberdino.model.enums.GameStatus;
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
public class HeartbeatEventDTO {

    @NotBlank(message = "Session ID cannot be blank")
    private String sessionId;

    @NotNull(message = "Game status cannot be null")
    private GameStatus status;

    @NotNull(message = "Sequence number cannot be null")
    private Long sequenceNumber;

    @NotNull(message = "Timestamp cannot be null")
    private Long timestamp;

    @NotNull(message = "Alive players count cannot be null")
    private Integer alivePlayersCount;

}
