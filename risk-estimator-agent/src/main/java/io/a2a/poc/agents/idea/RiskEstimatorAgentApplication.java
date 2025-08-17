package io.a2a.poc.agents.idea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"io.a2a.poc.agents.idea", "io.a2a.receptionist"})
public class RiskEstimatorAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskEstimatorAgentApplication.class, args);
    }
}