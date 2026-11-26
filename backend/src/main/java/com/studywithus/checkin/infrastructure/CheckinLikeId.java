package com.studywithus.checkin.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CheckinLikeId implements Serializable {

    @Column(name = "checkin_id", nullable = false)
    private UUID checkinId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
