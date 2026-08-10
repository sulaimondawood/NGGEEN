package com.dawood.nggeen.trade.infrastructure.journal.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ChronicleQueueConfig {
    @Value("${app.journal.path:./data/chronicle/journal}")
    private String journalPath;

    @Bean(destroyMethod = "close")
    public ChronicleQueue chronicleQueue() {
        File baseDir = new File(journalPath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        return SingleChronicleQueueBuilder
                .single(baseDir)
                .rollCycle(RollCycles.FAST_DAILY)
                .build();
    }

}
