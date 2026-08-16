package com.dawood.nggeen.trade.infrastructure.journal.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class ChronicleQueueConfig {
    @Value("${nggeen.journal.path:./data/chronicle/journal}")
    private String journalPath;

    @Bean(destroyMethod = "close")
    public ChronicleQueue chronicleQueue() {
        try {
            Path baseDir = Path.of(journalPath);
            Files.createDirectories(baseDir);

            return SingleChronicleQueueBuilder
                    .single(baseDir.toFile())
                    .rollCycle(RollCycles.FAST_DAILY)
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Chronicle Queue directory at " + journalPath, e);
        }
    }

}
