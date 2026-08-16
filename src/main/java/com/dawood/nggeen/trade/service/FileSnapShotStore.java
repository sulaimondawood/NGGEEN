package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.OrderBookSnapshot;
import com.dawood.nggeen.trade.service.contracts.SnapshotStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileSnapShotStore implements SnapshotStore {

    private ObjectMapper objectMapper;

    @Value("${nggeen.snapshot.path}")
    private Path path;

    @Override
    public synchronized void save(OrderBookSnapshot snapshot) {
        try {
            Path dir = path.resolve(snapshot.getSymbol());
            Files.createDirectories(dir);

            String filename = "snapshot-%020d.json".formatted(snapshot.getSequenceNo());
            Path finalPath = dir.resolve(filename);
            Path tmp = dir.resolve(filename + ".tmp");

            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(snapshot);
            Files.write(tmp, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
                ch.force(true);
            }

            Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            cleanupOldFiles(2, dir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save snapshot for " + snapshot.getSymbol(), e);
        }

    }

    private void cleanupOldFiles(int keep, Path dir) {
        if (keep < 1) return;

        try (var pathStream = Files.list(dir)) {
            List<Path> snapshots = pathStream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("snapshot") && name.endsWith("json");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            int excess = snapshots.size() - keep;
            if (excess <= 0) {
                return;
            }
            for (int i = 0; i < excess; i++) {
                Path old = snapshots.get(i);
                try {
                    Files.deleteIfExists(old);
                } catch (Exception e) {
                    // don't fail snapshot save just because cleanup failed
                    log.warn("Failed to delete old snapshot {}", old, e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup old snapshots in {}", dir, e);
        }
    }

}
