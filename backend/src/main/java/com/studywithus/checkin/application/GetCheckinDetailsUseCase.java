package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCheckinDetailsUseCase implements UseCase<UUID, CheckinResult> {

    private final CheckinRepository checkinRepository;

    @Override
    @Transactional(readOnly = true)
    public CheckinResult execute(UUID checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Checkin not found"));

        return CheckinResult.fromDomain(checkin);
    }
}
