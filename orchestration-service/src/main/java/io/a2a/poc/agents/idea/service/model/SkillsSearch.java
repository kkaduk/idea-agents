package io.a2a.poc.agents.idea.service.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkillsSearch {

    private List<String> keywords;
    private List<String> requiredTags;

    // toString (optional)
    @Override
    public String toString() {
        return "SearchCriteria{" +
                "keywords=" + keywords +
                ", requiredTags=" + requiredTags +
                '}';
    }
}
