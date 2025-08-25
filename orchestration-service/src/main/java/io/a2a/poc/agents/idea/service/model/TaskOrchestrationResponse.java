package io.a2a.poc.agents.idea.service.model;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskOrchestrationResponse(
        String taskId,
        ExecutionMode executionMode,
        List<SelectedSkill> selectedSkills,
        String reason
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SelectedSkill(
            String stepId,
            String agentName,
            URI agentUrl,
            String skillId,
            Double confidence,
            Integer priority,
            Integer timeoutSec,
            Retries retries,
            Map<String, Object> input, 
            List<String> dependsOn
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Retries(
            Integer maxAttempts,
            Integer backoffSec
    ) {}

    public enum ExecutionMode {
        SEQUENTIAL, PARALLEL, MIXED;

        @JsonCreator
        public static ExecutionMode from(String value) {
            if (value == null) return null;
            return ExecutionMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }

        @JsonValue
        public String toValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
