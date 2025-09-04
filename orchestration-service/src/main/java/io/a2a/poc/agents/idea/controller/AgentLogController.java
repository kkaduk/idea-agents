package io.a2a.poc.agents.idea.controller;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class AgentLogController {

    private static final String LOG_DIR = "logs";
    private static final Set<String> ALLOWED_AGENTS = Set.of(
            "human-agent",
            "idea-creator-agent",
            "idea-critic-agent",
            "idea-finalizer-agent",
            "risk-estimator-agent",
            "orchestration-service"
    );

    @GetMapping(value = "/{agent}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getAgentLog(
            @PathVariable("agent") String agent,
            @RequestParam(value = "lines", required = false, defaultValue = "100") int lines
    ) {
        if (!ALLOWED_AGENTS.contains(agent)) {
            return ResponseEntity.badRequest()
                    .body("Unknown agent: " + agent);
        }
        String currentDir = Paths.get("").toAbsolutePath().toString();
        File logFile = new File(currentDir + "/../" + LOG_DIR, agent + ".out");
        
        // System.out.println("KK path: " + currentDir);
        // System.out.println("KK logfile: " + logFile);
        if (!logFile.exists()) {
            return ResponseEntity.ok("Log file not found: " + logFile.getName());
        }

        List<String> collected = tail(logFile, lines);
        String joined = String.join("\n", collected);
        return ResponseEntity.ok(joined);
    }

    private List<String> tail(File file, int lines) {
        Deque<String> result = new ArrayDeque<>(lines);
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null && result.size() < lines) {
                if (line.contains("io.a2a")) {
                    result.addFirst(line);
                }
            }
        } catch (IOException e) {
            return List.of("Failed to read log: " + e.getMessage());
        }
        return new ArrayList<>(result);
    }

    // Helper: Read file in reverse order (tail)
    // Simple hand-rolled to avoid dependency on commons-io or external libs.
    private static class ReversedLinesFileReader implements Closeable {
        private final RandomAccessFile raf;
        private final long fileLength;
        private long pointer;

        ReversedLinesFileReader(File file, java.nio.charset.Charset charset) throws IOException {
            raf = new RandomAccessFile(file, "r");
            fileLength = raf.length();
            pointer = fileLength - 1;
        }

        public String readLine() throws IOException {
            if (pointer < 0) return null;
            StringBuilder sb = new StringBuilder();
            int c;
            boolean seenAny = false;
            while (pointer >= 0) {
                raf.seek(pointer--);
                c = raf.read();
                if (c == '\n' && seenAny) break;
                if (c != '\n' && c != '\r') {
                    sb.append((char) c);
                    seenAny = true;
                }
            }
            if (sb.length() == 0 && pointer < 0) return null;
            return sb.reverse().toString();
        }

        public void close() throws IOException {
            raf.close();
        }
    }
}
