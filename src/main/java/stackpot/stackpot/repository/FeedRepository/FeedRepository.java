package stackpot.stackpot.repository.FeedRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stackpot.stackpot.domain.Feed;
import stackpot.stackpot.domain.enums.Category;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

    @Query("SELECT f, " +
            "       COALESCE(FL.likeCount, 0) AS likeCount " +
            "FROM Feed f " +
            "LEFT JOIN (SELECT fl.feed.id AS feedId, COUNT(fl) AS likeCount FROM FeedLike fl GROUP BY fl.feed.id) FL " +
            "ON f.id = FL.feedId " +
            "WHERE (:category IS NULL OR f.category = :category) " + // category 필터 수정
            "AND ( " +
            "     (:sort = 'new' AND f.createdAt < :lastCreatedAt) OR " +
            "     (:sort = 'old' AND f.createdAt <= :lastCreatedAt) OR " +
            "     (:sort = 'popular') " +
            ") " +
            "ORDER BY " +
            "     CASE WHEN :sort = 'popular' THEN likeCount END DESC, " +
            "     CASE WHEN :sort = 'new' THEN f.createdAt END DESC, " +
            "     CASE WHEN :sort = 'old' THEN f.createdAt END ASC, " +
            "     f.id ASC")
    List<Object[]> findFeeds(
            @Param("category") Category category,  // ✅ String → Enum 타입 유지
            @Param("sort") String sort,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            Pageable pageable);

    List<Feed> findByUser_Id(Long userId);
    Page<Feed> findByTitleContainingOrContentContainingOrderByCreatedAtDesc(String titleKeyword, String contentKeyword, Pageable pageable);

    // 기본 페이징 조회
    List<Feed> findByUser_Id(Long userId, Pageable pageable);

    // 커서 기반 페이징 조회
    List<Feed> findByUserIdAndCreatedAtBefore(Long userId, LocalDateTime createdAt, Pageable pageable);

}