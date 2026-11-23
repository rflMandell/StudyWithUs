package com.studywithus.moderation.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationAuthorizationService {

    private final CheckinRepository checkinRepository;
    private final CommentRepository commentRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public boolean isPlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public void assertCanModerateTarget(ReportTargetType targetType, UUID targetId, UUID reviewerId) {
        switch (targetType) {
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comentário não encontrado"));
                Checkin checkin = checkinRepository.findById(comment.getCheckinId())
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));
                assertCommunityModerator(checkin.getCommunityId(), reviewerId);
            }
            case CHECKIN -> {
                Checkin checkin = checkinRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));
                assertCommunityModerator(checkin.getCommunityId(), reviewerId);
            }
            case COMMUNITY -> assertCommunityModerator(targetId, reviewerId);
            case USER -> {
                if (!isPlatformAdmin()) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Apenas administradores da plataforma podem moderar usuários");
                }
            }
            case MEDIA -> {
                MediaAsset asset = mediaAssetRepository.findById(targetId)
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Mídia não encontrada"));
                if (asset.getPurpose() == MediaPurpose.COMMUNITY_COVER && asset.getTargetId() != null) {
                    assertCommunityModerator(asset.getTargetId(), reviewerId);
                } else if (!isPlatformAdmin()) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Apenas administradores da plataforma podem moderar esta mídia");
                }
            }
        }
    }

    public UUID resolveCommunityId(ReportTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case COMMENT -> commentRepository.findById(targetId)
                    .flatMap(comment -> checkinRepository.findById(comment.getCheckinId()))
                    .map(Checkin::getCommunityId)
                    .orElse(null);
            case CHECKIN -> checkinRepository.findById(targetId)
                    .map(Checkin::getCommunityId)
                    .orElse(null);
            case COMMUNITY -> targetId;
            case MEDIA -> mediaAssetRepository.findById(targetId)
                    .filter(asset -> asset.getPurpose() == MediaPurpose.COMMUNITY_COVER)
                    .map(MediaAsset::getTargetId)
                    .orElse(null);
            case USER -> null;
        };
    }

    public void assertCommunityModerator(UUID communityId, UUID reviewerId) {
        CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(communityId, reviewerId)
                .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Você não é membro desta comunidade"));

        if (!member.isModeratorOrHigher()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Apenas moderadores e administradores da comunidade podem avaliar denúncias");
        }
    }

    public boolean isModeratorOfAnyCommunity(UUID userId) {
        List<UUID> communityIds = communityMemberRepository.findActiveCommunityIdsByUserId(userId);
        for (UUID communityId : communityIds) {
            if (communityMemberRepository.findByCommunityIdAndUserId(communityId, userId)
                    .map(CommunityMember::isModeratorOrHigher)
                    .orElse(false)) {
                return true;
            }
        }
        return false;
    }

    public boolean canViewReport(ReportResult report, UUID userId) {
        if (isPlatformAdmin() || report.reporterId().equals(userId)) {
            return true;
        }
        UUID communityId = resolveCommunityId(report.targetType(), report.targetId());
        if (communityId == null) {
            return false;
        }
        return communityMemberRepository.findByCommunityIdAndUserId(communityId, userId)
                .map(CommunityMember::isModeratorOrHigher)
                .orElse(false);
    }
}
