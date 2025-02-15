package stackpot.stackpot.service.PotApplicationService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stackpot.stackpot.apiPayload.code.status.ErrorStatus;
import stackpot.stackpot.apiPayload.exception.handler.MemberHandler;
import stackpot.stackpot.apiPayload.exception.handler.PotHandler;
import stackpot.stackpot.config.security.JwtTokenProvider;
import stackpot.stackpot.converter.PotApplicationConverter.PotApplicationConverter;
import stackpot.stackpot.converter.PotDetailConverter;
import stackpot.stackpot.domain.Pot;
import stackpot.stackpot.domain.User;
import stackpot.stackpot.domain.enums.ApplicationStatus;
import stackpot.stackpot.domain.mapping.PotApplication;
import stackpot.stackpot.repository.PotApplicationRepository.PotApplicationRepository;
import stackpot.stackpot.repository.PotRepository.PotRepository;
import stackpot.stackpot.repository.UserRepository.UserRepository;
import stackpot.stackpot.service.EmailService.EmailService;
import stackpot.stackpot.web.dto.PotApplicationRequestDto;
import stackpot.stackpot.web.dto.PotApplicationResponseDto;
import stackpot.stackpot.web.dto.PotDetailResponseDto;
import stackpot.stackpot.web.dto.PotDetailWithApplicantsResponseDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PotApplicationServiceImpl implements PotApplicationService {

    private final PotApplicationRepository potApplicationRepository;
    private final PotRepository potRepository;
    private final UserRepository userRepository;
    private final PotApplicationConverter potApplicationConverter;
    private final PotDetailConverter potDetailConverter;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;


    @Transactional
    public PotApplicationResponseDto applyToPot(PotApplicationRequestDto dto, Long potId) {
        // 인증된 사용자 이메일 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new MemberHandler(ErrorStatus.AUTHENTICATION_FAILED);
        }
        String email = authentication.getName();

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 팟 조회
        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new PotHandler(ErrorStatus.POT_NOT_FOUND));

        // 중복 지원 방지
        if (potApplicationRepository.existsByUserIdAndPot_PotId(user.getId(), potId)) {
            throw new PotHandler(ErrorStatus.DUPLICATE_APPLICATION);
        }

        // 지원 엔티티 생성 및 저장
        PotApplication potApplication = potApplicationConverter.toEntity(dto, pot, user);
        potApplication.setApplicationStatus(ApplicationStatus.PENDING);
        potApplication.setAppliedAt(LocalDateTime.now());

        PotApplication savedApplication = potApplicationRepository.save(potApplication);

        // 지원자의 역할 조회 및 한글 변환
        String applicantRole = user.getRole() != null ? getVegetableNameByRole(user.getRole().name()) : "멤버";

        // 이메일 전송
        emailService.sendSupportNotification(
                pot.getUser().getEmail(),
                pot.getPotName(),
                String.format("%s %s", user.getNickname(), applicantRole), // 닉네임 + 역할
                user.getUserIntroduction() != null ? user.getUserIntroduction() : "없음"
        );

        // 저장된 지원 정보를 응답 DTO로 변환
        return potApplicationConverter.toDto(savedApplication);
    }
//
//    @Transactional
//    @Override
//    public void cancelApplication(Long potId) {
//        // 인증된 사용자 이메일 가져오기
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//
//        // 사용자 조회
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//
//        // 팟 조회
//        Pot pot = potRepository.findById(potId)
//                .orElseThrow(() -> new IllegalArgumentException("해당 팟을 찾을 수 없습니다."));
//
//        // 지원 내역 조회
//        PotApplication application = potApplicationRepository.findByUserIdAndPot_PotId(user.getId(), potId)
//                .orElseThrow(() -> new IllegalArgumentException("해당 팟에 지원한 이력이 없습니다."));
//
////        // 이미 취소된 지원인지 확인
////        if (application.getApplicationStatus() == ApplicationStatus.CANCELED) {
////            throw new IllegalStateException("이미 취소된 지원입니다.");
////        }
//
//        // 상태 변경
//        application.setApplicationStatus(ApplicationStatus.CANCELED);
//        application.setAppliedAt(LocalDateTime.now()); // 취소 시간 업데이트
//        potApplicationRepository.save(application);
//
//
//    }

    @Transactional
    public void cancelApplication(Long potId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new MemberHandler(ErrorStatus.AUTHENTICATION_FAILED);
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        PotApplication application = potApplicationRepository.findByUserIdAndPot_PotId(user.getId(), potId)
                .orElseThrow(() -> new PotHandler(ErrorStatus.APPLICATION_NOT_FOUND));

        potApplicationRepository.delete(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PotApplicationResponseDto> getApplicantsByPotId(Long potId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new MemberHandler(ErrorStatus.AUTHENTICATION_FAILED);
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Pot pot = potRepository.findById(potId)
                .orElseThrow(() -> new PotHandler(ErrorStatus.POT_NOT_FOUND));

        if (!pot.getUser().equals(user)) {
            throw new PotHandler(ErrorStatus.UNAUTHORIZED_ACCESS);
        }

        List<PotApplication> applications = potApplicationRepository.findByPot_PotId(potId);

        return applications.stream()
                .map(potApplicationConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PotDetailWithApplicantsResponseDto getPotDetailsAndApplicants(Long potId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new MemberHandler(ErrorStatus.AUTHENTICATION_FAILED);
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        Pot pot = potRepository.findPotWithRecruitmentDetailsByPotId(potId)
                .orElseThrow(() -> new PotHandler(ErrorStatus.POT_NOT_FOUND));

        boolean isOwner = user.getId().equals(pot.getUser().getId());
        boolean isApplied = pot.getPotApplication().stream()
                .anyMatch(application -> application.getUser().getId().equals(user.getId()));

        String recruitmentDetails = pot.getRecruitmentDetails().stream()
                .map(recruitmentDetail -> getKoreanRoleName(recruitmentDetail.getRecruitmentRole().name()) + "(" + recruitmentDetail.getRecruitmentCount() + ")")
                .collect(Collectors.joining(", "));

        PotDetailResponseDto potDetailDto = potDetailConverter.toPotDetailResponseDto(pot.getUser(), pot, recruitmentDetails, isOwner, isApplied);

        List<PotApplicationResponseDto> applicants = Collections.emptyList();
        if (isOwner && "RECRUITING".equals(pot.getPotStatus())) {
            List<PotApplication> applications = potApplicationRepository.findByPot_PotId(potId);
            applicants = applications.stream()
                    .map(potApplicationConverter::toDto)
                    .collect(Collectors.toList());
        }

        return PotDetailWithApplicantsResponseDto.builder()
                .potDetail(potDetailDto)
                .applicants(applicants)
                .build();
    }

    private String getKoreanRoleName(String role) {
        Map<String, String> roleToKoreaneMap = Map.of(
                "BACKEND", " 백엔드",
                "FRONTEND", " 프론트엔드",
                "DESIGN", " 디자인",
                "PLANNING", " 기획"
        );
        return roleToKoreaneMap.getOrDefault(role, "알 수 없음");
    }
    private String getVegetableNameByRole(String role) {
        Map<String, String> roleToVegetableMap = Map.of(
                "DESIGN", " 브로콜리",
                "PLANNING", " 당근",
                "BACKEND", " 양파",
                "FRONTEND", " 버섯",
                "UNKNOWN",""
        );

        return roleToVegetableMap.getOrDefault(role, "알 수 없음");
    }

}
