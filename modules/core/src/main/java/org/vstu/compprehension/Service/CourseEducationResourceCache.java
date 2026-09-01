package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.repository.CourseRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серсис закэшированных курсов с educational resources.
 */
@Service
@RequiredArgsConstructor
public class CourseEducationResourceCache {

    private static final Duration TTL = Duration.ofMinutes(10);

    private static final Long ABSENT = Long.valueOf(Long.MIN_VALUE);

    private record CachedValue(Long educationResourceId, long expiresAtNanos) {
    }

    private final CourseRepository courseRepository;
    private final Map<Long, CachedValue> cache = new ConcurrentHashMap<>();

    public List<Long> educationResourceIdsOf(Collection<Long> courseIds) {
        long now = System.nanoTime();
        var resolved = new LinkedHashSet<Long>();
        var missing = new ArrayList<Long>();

        for (Long courseId : courseIds) {
            CachedValue cached = cache.get(courseId);
            if (cached == null || cached.expiresAtNanos() - now <= 0) {
                missing.add(courseId);
            } else if (!ABSENT.equals(cached.educationResourceId())) {
                resolved.add(cached.educationResourceId());
            }
        }

        if (!missing.isEmpty()) {
            long expiresAt = now + TTL.toNanos();
            for (Long courseId : missing) {
                cache.put(courseId, new CachedValue(ABSENT, expiresAt));
            }
            for (var ref : courseRepository.findEducationResourceRefsByCourseIdIn(missing)) {
                cache.put(ref.courseId(), new CachedValue(ref.educationResourceId(), expiresAt));
                resolved.add(ref.educationResourceId());
            }
        }

        return List.copyOf(resolved);
    }
}
