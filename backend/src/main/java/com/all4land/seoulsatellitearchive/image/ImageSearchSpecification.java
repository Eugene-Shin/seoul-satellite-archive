package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.config.ClockConfig;
import com.all4land.seoulsatellitearchive.image.dto.ImageSearchRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.spatial.criteria.JTSSpatialCriteriaBuilder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImageSearchSpecification {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    public static Specification<SatelliteImage> of(ImageSearchRequest request) {
        return Specification.allOf(
                titleContains(request.getKeyword()),
                acquiredFrom(request.getAcquiredFrom()),
                acquiredTo(request.getAcquiredTo()),
                in("satellite", request.getSatellites()),
                instrumentsOverlap(request.getInstruments()),
                in("sensorType", request.getSensorTypes()),
                orbitDirectionEquals(request.getOrbitDirection()),
                providerIn(request.getProviders()),
                providerIsPaid(request.getIsPaid()),
                gsdAtLeast(request.getGsdMin()),
                gsdAtMost(request.getGsdMax()),
                cloudAtMost(request.getCloudMax()),
                footprintIntersects(request.getBbox()));
    }

    private static Specification<SatelliteImage> titleContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLike(keyword.strip()) + "%";
        return (root, query, cb) -> ((HibernateCriteriaBuilder) cb).ilike(root.get("title"), pattern, '\\');
    }

    private static Specification<SatelliteImage> acquiredFrom(LocalDate from) {
        if (from == null) {
            return Specification.unrestricted();
        }
        Instant start = from.atStartOfDay(ClockConfig.KST).toInstant();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("acquiredAt"), start);
    }

    // 소수점 초가 있는 23:59:59.xxx까지 포함하도록 다음 날 0시 미만으로 비교한다
    private static Specification<SatelliteImage> acquiredTo(LocalDate to) {
        if (to == null) {
            return Specification.unrestricted();
        }
        Instant nextDayStart = to.plusDays(1).atStartOfDay(ClockConfig.KST).toInstant();
        return (root, query, cb) -> cb.lessThan(root.<Instant>get("acquiredAt"), nextDayStart);
    }

    private static Specification<SatelliteImage> in(String attribute, List<?> values) {
        return CollectionUtils.isEmpty(values)
                ? Specification.unrestricted()
                : (root, query, cb) -> root.get(attribute).in(values);
    }

    private static Specification<SatelliteImage> instrumentsOverlap(List<String> instruments) {
        return CollectionUtils.isEmpty(instruments)
                ? Specification.unrestricted()
                : (root, query, cb) -> ((HibernateCriteriaBuilder) cb)
                        .arrayIntersects(root.<String[]>get("instruments"), instruments.toArray(String[]::new));
    }

    private static Specification<SatelliteImage> orbitDirectionEquals(OrbitDirection orbitDirection) {
        return orbitDirection == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("orbitDirection"), orbitDirection);
    }

    private static Specification<SatelliteImage> providerIn(List<String> providers) {
        return CollectionUtils.isEmpty(providers)
                ? Specification.unrestricted()
                : (root, query, cb) -> root.get("provider").get("code").in(providers);
    }

    private static Specification<SatelliteImage> providerIsPaid(Boolean isPaid) {
        return isPaid == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("provider").get("isPaid"), isPaid);
    }

    private static Specification<SatelliteImage> gsdAtLeast(BigDecimal min) {
        return min == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("gsdMeters"), min);
    }

    private static Specification<SatelliteImage> gsdAtMost(BigDecimal max) {
        return max == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("gsdMeters"), max);
    }

    private static Specification<SatelliteImage> cloudAtMost(BigDecimal max) {
        return max == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("cloudCover"), max);
    }

    private static Specification<SatelliteImage> footprintIntersects(List<Double> bbox) {
        if (bbox == null) {
            return Specification.unrestricted();
        }
        Geometry envelope = WGS84.toGeometry(new Envelope(bbox.get(0), bbox.get(2), bbox.get(1), bbox.get(3)));
        return (root, query, cb) -> ((HibernateCriteriaBuilder) cb)
                .unwrap(JTSSpatialCriteriaBuilder.class)
                .intersects(root.<Geometry>get("footprint"), envelope);
    }

    // 정렬 순서에서 커서 행보다 뒤인 행. 앞 기준들이 같고 이번 기준에서 처음 뒤인 경우를 기준마다 OR로 묶는다
    public static Specification<SatelliteImage> after(ImageCursor cursor) {
        if (cursor == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            List<Predicate> branches = new ArrayList<>();
            List<Predicate> same = new ArrayList<>();
            for (ImageSort sort : cursor.sorts()) {
                boolean ascending = sort.getDirection().isAscending();
                switch (sort) {
                    case ACQUIRED_AT_ASC, ACQUIRED_AT_DESC ->
                        appendKey(cb, root.get("acquiredAt"), cursor.acquiredAt(), ascending, branches, same);
                    case CLOUD_COVER_ASC, CLOUD_COVER_DESC ->
                        appendKey(cb, root.get("cloudCover"), cursor.cloudCover(), ascending, branches, same);
                }
            }
            branches.add(and(cb, same, cb.lessThan(root.get("id"), cursor.id())));
            return cb.or(branches.toArray(Predicate[]::new));
        };
    }

    // 정렬은 nullsLast: 값이 있으면 NULL도 뒤, 커서 값이 NULL이면 이 기준으로 뒤인 행은 없다
    private static <T extends Comparable<? super T>> void appendKey(
            CriteriaBuilder cb,
            Path<T> path,
            T value,
            boolean ascending,
            List<Predicate> branches,
            List<Predicate> same) {
        if (value == null) {
            same.add(cb.isNull(path));
            return;
        }
        Predicate beyond = ascending ? cb.greaterThan(path, value) : cb.lessThan(path, value);
        branches.add(and(cb, same, cb.or(beyond, cb.isNull(path))));
        same.add(cb.equal(path, value));
    }

    private static Predicate and(CriteriaBuilder cb, List<Predicate> same, Predicate last) {
        List<Predicate> all = new ArrayList<>(same);
        all.add(last);
        return cb.and(all.toArray(Predicate[]::new));
    }

    private static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
