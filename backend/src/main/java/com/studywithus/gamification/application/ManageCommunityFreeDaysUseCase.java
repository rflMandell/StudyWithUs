package com.studywithus.gamification.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.gamification.domain.CommunityFreeDay;
import com.studywithus.gamification.domain.CommunityFreeDayRepository;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageCommunityFreeDaysUseCase {

    private final CommunityFreeDayRepository freeDayRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<Integer> getFreeDays(UUID communityId) {
        return freeDayRepository.findDaysOfWeekByCommunityId(communityId);
    }

    @Transactional
    public void addFreeDay(UUID communityId, int dayOfWeek, UUID requesterId) {
        validateAdminPermission(communityId, requesterId);
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_DAY_OF_WEEK", "Day of week must be between 1 (Monday) and 7 (Sunday)");
        }
        if (!freeDayRepository.existsByCommunityIdAndDayOfWeek(communityId, dayOfWeek)) {
            freeDayRepository.save(CommunityFreeDay.builder()
                    .communityId(communityId)
                    .dayOfWeek(dayOfWeek)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    @Transactional
    public void removeFreeDay(UUID communityId, int dayOfWeek, UUID requesterId) {
        validateAdminPermission(communityId, requesterId);
        freeDayRepository.deleteByCommunityIdAndDayOfWeek(communityId, dayOfWeek);
    }

    private void validateAdminPermission(UUID communityId, UUID requesterId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        if (!community.getOwnerId().equals(requesterId)) {
            CommunityMember member = memberRepository.findByCommunityIdAndUserId(communityId, requesterId)
                    .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Requester is not a member of this community"));

            if (member.getRole() != CommunityRole.OWNER && member.getRole() != CommunityRole.ADMIN) {
                throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only community owner or admin can configure free days");
            }
        }
    }
}
