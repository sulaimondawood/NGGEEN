package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.OrderBookSnapshot;
import com.dawood.nggeen.trade.service.contracts.SnapshotStore;
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
import java.util.Optional;

@Component
@Slf4j
public class FileSnapShotStore implements SnapshotStore {

    private final ObjectMapper objectMapper;

    private final Path path;

    public FileSnapShotStore(ObjectMapper objectMapper,
                             @Value("${nggeen.snapshot.path}") Path path) {
        this.objectMapper = objectMapper;
        this.path = path;
    }

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

            cleanupOldFiles(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save snapshot for " + snapshot.getSymbol(), e);
        }

    }

    @Override
    public synchronized Optional<OrderBookSnapshot> loadLatest(String symbol) {
        try {
            Path dir = path.resolve(symbol);
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return Optional.empty();
            }

            try (var stream = Files.list(dir)) {
                return stream
                        .filter(p -> p.getFileName().toString().startsWith("snapshot-")
                                && p.getFileName().toString().endsWith(".json"))
                        .max(Comparator.comparing(path -> path.getFileName().toString()))
                        .map(this::read);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load snapshot for " + symbol, e);
        }
    }

    private OrderBookSnapshot read(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), OrderBookSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed reading snapshot " + path, e);
        }
    }

    private void cleanupOldFiles(Path dir) {
        try (var pathStream = Files.list(dir)) {
            List<Path> snapshots = pathStream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("snapshot-") && name.endsWith(".json");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            int excess = snapshots.size() - 3;
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
