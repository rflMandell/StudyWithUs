package com.studywithus.moderation.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.moderation.domain.ReportResolvedEvent;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.notification.application.CreateInAppNotificationUseCase;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveReportUseCaseTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private CreateInAppNotificationUseCase createInAppNotificationUseCase;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ResolveReportUseCase useCase;

    private UUID reviewerId;
    private UUID reportId;
    private UUID commentId;
    private UUID checkinId;
    private UUID communityId;
    private UUID authorId;
    private User reviewer;

    @BeforeEach
    void setUp() {
        reviewerId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        checkinId = UUID.randomUUID();
        communityId = UUID.randomUUID();
        authorId = UUID.randomUUID();

        reviewer = mock(User.class);
        when(reviewer.isActive()).thenReturn(true);
    }

    @Test
    @DisplayName("Should resolve report and remove offensive comment, recording audit log")
    void shouldResolveReportAndRemoveComment() {
        Report report = Report.create(
                UUID.randomUUID(),
                ReportTargetType.COMMENT,
                commentId,
                "Offensive language",
                "Swearing in comments"
        );

        Comment comment = Comment.create(checkinId, authorId, "Spam text here");
        Checkin checkin = mock(Checkin.class);
        when(checkin.getCommunityId()).thenReturn(communityId);

        CommunityMember moderatorMember = CommunityMember.builder()
                .communityId(communityId)
                .userId(reviewerId)
                .role(CommunityRole.MODERATOR)
                .status(MemberStatus.ACTIVE)
                .build();

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, reviewerId))
                .thenReturn(Optional.of(moderatorMember));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveReportCommand command = new ResolveReportCommand(
                reportId,
                reviewerId,
                ModerationAction.REMOVE_CONTENT,
                "Violates code of conduct"
        );

        ReportResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(result.reviewedBy()).isEqualTo(reviewerId);
        assertThat(result.reviewedAt()).isNotNull();

        assertThat(comment.isActive()).isFalse();
        assertThat(comment.getDeletedAt()).isNotNull();
        verify(commentRepository).save(comment);
        verify(auditLogRepository).save(any(AuditLog.class));
        verify(eventPublisher).publishEvent(any(ReportResolvedEvent.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when user is not a community moderator")
    void shouldThrowForbiddenWhenNotModerator() {
        Report report = Report.create(
                UUID.randomUUID(),
                ReportTargetType.COMMENT,
                commentId,
                "Offensive",
                null
        );

        Comment comment = Comment.create(checkinId, authorId, "Some text");
        Checkin checkin = mock(Checkin.class);
        when(checkin.getCommunityId()).thenReturn(communityId);

        CommunityMember member = CommunityMember.builder()
                .communityId(communityId)
                .userId(reviewerId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, reviewerId))
                .thenReturn(Optional.of(member));

        ResolveReportCommand command = new ResolveReportCommand(
                reportId,
                reviewerId,
                ModerationAction.REMOVE_CONTENT,
                null
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when report is already reviewed")
    void shouldThrowWhenAlreadyReviewed() {
        Report report = Report.create(
                UUID.randomUUID(),
                ReportTargetType.COMMENT,
                commentId,
                "Spam",
                null
        );
        report.resolve(reviewerId);

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

        ResolveReportCommand command = new ResolveReportCommand(
                reportId,
                reviewerId,
                ModerationAction.DISMISS,
                null
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
