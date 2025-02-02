package stackpot.stackpot.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stackpot.stackpot.domain.Feed;
import stackpot.stackpot.domain.Pot;
import stackpot.stackpot.domain.User;
import stackpot.stackpot.repository.FeedLikeRepository;
import stackpot.stackpot.repository.PotMemberRepository;
import stackpot.stackpot.web.dto.PotRecruitmentResponseDto;
import stackpot.stackpot.web.dto.UserMypageResponseDto;
import stackpot.stackpot.domain.enums.Role;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mysql.cj.util.TimeUtil.DATE_FORMATTER;

@Component
@RequiredArgsConstructor
public class UserMypageConverter {
    private final PotMemberRepository potMemberRepository; // 추가

    public UserMypageResponseDto toDto(User user, List<Pot> completedPots, List<Feed> feeds) {
        return UserMypageResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname() + getVegetableNameByRole(user.getRole().name()))
                .role(user.getRole())
                .interest(user.getInterest())
                .userTemperature(user.getUserTemperature())
                .kakaoId(user.getKakaoId())
                .userIntroduction(user.getUserIntroduction())
                .completedPots(completedPots.stream().map(this::convertToCompletedPotDto).collect(Collectors.toList()))
                .feeds(feeds.stream().map(this::convertToFeedDto).collect(Collectors.toList()))
                .build();
    }


    private UserMypageResponseDto.CompletedPotDto convertToCompletedPotDto(Pot pot) {
        // 역할별 참여자 수 계산
        List<Object[]> roleCounts = potMemberRepository.findRoleCountsByPotId(pot.getPotId());
        Map<String, Integer> roleCountsMap = roleCounts.stream()
                .collect(Collectors.toMap(
                        roleCount -> ((Role) roleCount[0]).name(),
                        roleCount -> ((Long) roleCount[1]).intValue()
                ));

        return UserMypageResponseDto.CompletedPotDto.builder()
                .potId(pot.getPotId())
                .potName(pot.getPotName())
                .potStartDate(formatDate(pot.getPotStartDate()))
                .potEndDate(formatDate(pot.getPotEndDate()))
                .potSummary(pot.getPotSummary())
                .recruitmentDetails(pot.getRecruitmentDetails().stream()
                        .map(detail -> PotRecruitmentResponseDto.builder()
                                .recruitmentRole(detail.getRecruitmentRole().name()) // Enum → String 변환
                                .recruitmentCount(detail.getRecruitmentCount())
                                .build())
                        .collect(Collectors.toList()))
                .roleCounts(roleCountsMap)
                .build();
    }

    private final FeedLikeRepository feedLikeRepository;

    private UserMypageResponseDto.FeedDto convertToFeedDto(Feed feed) {
        return UserMypageResponseDto.FeedDto.builder()
                .feedId(feed.getFeedId())
                .title(feed.getTitle())
                .content(feed.getContent())
                .category(feed.getCategory())
                .likeCount(feedLikeRepository.countByFeed(feed))
                .createdAt(formatLocalDateTime(feed.getCreatedAt()))
                .build();
    }

    private String getVegetableNameByRole(String role) {
        Map<String, String> roleToVegetableMap = Map.of(
                "BACKEND", " 양파",
                "FRONTEND", " 버섯",
                "DESIGN", " 브로콜리",
                "PLANNING", " 당근"
        );
        return roleToVegetableMap.getOrDefault(role, "알 수 없음");
    }

    private String formatDate(java.time.LocalDate date) {
        return (date != null) ? date.format(DATE_FORMATTER) : "N/A";
    }

    // 날짜 포맷 적용 메서드
    private String formatLocalDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 H:mm");
        return (dateTime != null) ? dateTime.format(formatter) : "날짜 없음";
    }
}
