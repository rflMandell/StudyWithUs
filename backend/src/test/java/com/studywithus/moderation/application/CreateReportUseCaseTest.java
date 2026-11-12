package com.studywithus.moderation.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportCreatedEvent;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
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
class CreateReportUseCaseTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateReportUseCase useCase;

    private UUID reporterId;
    private User reporter;

    @BeforeEach
    void setUp() {
        reporterId = UUID.randomUUID();
        reporter = mock(User.class);
    }

    @Test
    @DisplayName("Should create report successfully for comment")
    void shouldCreateReportForComment() {
        UUID commentId = UUID.randomUUID();
        Comment comment = mock(Comment.class);
        when(comment.isActive()).thenReturn(true);
        when(reporter.isActive()).thenReturn(true);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReportCommand command = new CreateReportCommand(
                reporterId,
                ReportTargetType.COMMENT,
                commentId,
                "Spam or harassment",
                "User keeps spamming links"
        );

        ReportResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.reporterId()).isEqualTo(reporterId);
        assertThat(result.targetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(result.targetId()).isEqualTo(commentId);
        assertThat(result.reason()).isEqualTo("Spam or harassment");
        assertThat(result.status()).isEqualTo(ReportStatus.PENDING);

        verify(reportRepository).save(any(Report.class));
        verify(eventPublisher).publishEvent(any(ReportCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw when reporter is not found")
    void shouldThrowWhenReporterNotFound() {
        when(userRepository.findById(reporterId)).thenReturn(Optional.empty());

        CreateReportCommand command = new CreateReportCommand(
                reporterId,
                ReportTargetType.COMMENT,
                UUID.randomUUID(),
                "Spam",
                "details"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Should throw when reason is blank")
    void shouldThrowWhenReasonIsBlank() {
        when(reporter.isActive()).thenReturn(true);
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));

        CreateReportCommand command = new CreateReportCommand(
                reporterId,
                ReportTargetType.COMMENT,
                UUID.randomUUID(),
                "   ",
                "details"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Should throw when target comment does not exist")
    void shouldThrowWhenTargetCommentNotFound() {
        UUID commentId = UUID.randomUUID();
        when(reporter.isActive()).thenReturn(true);
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(null));

        CreateReportCommand command = new CreateReportCommand(
                reporterId,
                ReportTargetType.COMMENT,
                commentId,
                "Inappropriate language",
                null
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException dex = (DomainException) ex;
                    assertThat(dex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(dex.getCode()).isEqualTo("COMMENT_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("Should create report successfully for checkin")
    void shouldCreateReportForCheckin() {
        UUID checkinId = UUID.randomUUID();
        Checkin checkin = mock(Checkin.class);
        when(checkin.isActive()).thenReturn(true);
        when(reporter.isActive()).thenReturn(true);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReportCommand command = new CreateReportCommand(
                reporterId,
                ReportTargetType.CHECKIN,
                checkinId,
                "Fake checkin",
                "Spamming empty checkins"
        );

        ReportResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.targetType()).isEqualTo(ReportTargetType.CHECKIN);
        verify(eventPublisher).publishEvent(any(ReportCreatedEvent.class));
    }
}
