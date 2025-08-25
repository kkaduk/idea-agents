package io.a2a.poc.agents.idea.service.mapper;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.a2a.poc.agents.idea.service.model.SkillsSearch;
import reactor.core.publisher.Mono;

public final class SkillsSearchMapper {

    private SkillsSearchMapper() {}

    /** Convert Mono<Map<String,Object>> -> Mono<SkillsSearch> */
    public static Mono<SkillsSearch> toSkillsSearch(Mono<Map<String, Object>> source) {
        return source
            .defaultIfEmpty(Collections.emptyMap())
            .map(SkillsSearchMapper::mapToSkillsSearch);
    }

    /** Convert Map -> SkillsSearch (tolerant to various input shapes) */
    @SuppressWarnings("unchecked")
    private static SkillsSearch mapToSkillsSearch(Map<String, Object> map) {
        if (map == null) map = Collections.emptyMap();

        // allow case-insensitive keys just in case
        Map<String, Object> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ci.putAll(map);

        List<String> keywords     = toStringList(ci.get("keywords"));
        List<String> requiredTags = toStringList(ci.get("requiredTags"));

        // normalize: trim, drop empties, dedupe while preserving order
        keywords     = normalize(keywords);
        requiredTags = normalize(requiredTags);

        return new SkillsSearch(keywords, requiredTags);
    }

    /** Best-effort coercion to List<String> from common shapes */
    public static List<String> toStringList(Object value) {
        if (value == null) return Collections.emptyList();

        if (value instanceof List<?> list) {
            return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        }

        // Handle single string: either one token or comma/semicolon-separated
        if (value instanceof String s) {
            if (s.isBlank()) return Collections.emptyList();
            // split on comma or semicolon if present
            String[] parts = s.contains(",") || s.contains(";")
                ? s.split("[,;]")
                : new String[] { s };
            return Arrays.stream(parts).map(String::trim).toList();
        }

        // Handle arrays
        if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            List<String> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                Object elem = java.lang.reflect.Array.get(value, i);
                if (elem != null) out.add(elem.toString());
            }
            return out;
        }

        // Fallback: single value -> singleton list
        return List.of(value.toString());
    }

    private static List<String> normalize(List<String> in) {
        if (in == null || in.isEmpty()) return Collections.emptyList();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : in) {
            String t = s == null ? "" : s.trim();
            if (!t.isEmpty()) set.add(t);
        }
        return new ArrayList<>(set);
    }
}
