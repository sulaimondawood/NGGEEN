package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBookSnapshot;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileSnapShotStoreTest {

    @TempDir
    private Path tempDir;

    private FileSnapShotStore snapShotStore;
    private final String SYMBOL = "BTCUSDT";
    private ObjectMapper objectMapper;

    @BeforeEach
    void init() {
        objectMapper = new ObjectMapper();
        snapShotStore = new FileSnapShotStore(objectMapper, tempDir);
    }

    private OrderBookSnapshot createSnapshot(List<Order> asks, List<Order> buys, long seq) {

        OrderBookSnapshot snapshot = new OrderBookSnapshot();
        snapshot.setSymbol(SYMBOL);
        snapshot.setSequenceNo(seq);
        snapshot.setBids(buys);
        snapshot.setAsks(asks);

        return snapshot;
    }

    @Nested
    class SaveTests {

        @Test
        void shouldSaveSnapshot_ToDisk() throws IOException {
            Order restingOrder = new Order();
            restingOrder.setSymbol(SYMBOL);
            restingOrder.setPrice(new BigDecimal("1000"));
            restingOrder.setOrderSide(OrderSide.BUY);

            OrderBookSnapshot snapshot = createSnapshot(List.of(), List.of(restingOrder), 1);

            assertDoesNotThrow(() -> snapShotStore.save(snapshot));

            Path symbolDir = tempDir.resolve(SYMBOL);
            Path snapshotFile = symbolDir.resolve("snapshot-00000000000000000001.json");
            Path tmpFile = symbolDir.resolve("snapshot-00000000000000000001.json.tmp");

            Assertions.assertThat(Files.exists(snapshotFile)).isTrue();
            Assertions.assertThat(Files.exists(snapshotFile)).isTrue();
            Assertions.assertThat(Files.size(snapshotFile)).isGreaterThan(0);
            assertFalse(Files.exists(tmpFile));
        }

        @Test
        void shouldCleanUpOldSnaps_WhenSnapshots_GrowsMoreThan3(){
            Order restingOrder = new Order();
            restingOrder.setSymbol(SYMBOL);
            restingOrder.setPrice(new BigDecimal("1000"));
            restingOrder.setOrderSide(OrderSide.BUY);

            OrderBookSnapshot snapshot = createSnapshot(List.of(), List.of(restingOrder), 1);
            OrderBookSnapshot snapshot2 = createSnapshot(List.of(), List.of(restingOrder), 2);
            OrderBookSnapshot snapshot3 = createSnapshot(List.of(), List.of(), 3);
            OrderBookSnapshot snapshot4 = createSnapshot(List.of(), List.of(), 4);
            OrderBookSnapshot snapshot5 = createSnapshot(List.of(), List.of(), 5);

            snapShotStore.save(snapshot);
            snapShotStore.save(snapshot2);
            snapShotStore.save(snapshot3);
            snapShotStore.save(snapshot4);
            snapShotStore.save(snapshot5);

            Path dir = tempDir.resolve(SYMBOL);

            Path snapFile1 = dir.resolve("snapshot-00000000000000000001.json");
            Path snapFile2 = dir.resolve("snapshot-00000000000000000002.json");
            assertFalse(Files.exists(snapFile1), "Snapshot 1 should have been purged");
            assertFalse(Files.exists(snapFile2), "Snapshot 2 should have been purged");


            Path snapFile3 = dir.resolve("snapshot-00000000000000000003.json");
            Path snapFile4 = dir.resolve("snapshot-00000000000000000004.json");
            Path snapFile5 = dir.resolve("snapshot-00000000000000000005.json");
            assertTrue(Files.exists(snapFile3), "Snapshot 3 should exist");
            assertTrue(Files.exists(snapFile4), "Snapshot 4 should exist");
            assertTrue(Files.exists(snapFile5), "Snapshot 5 should exist");

        }

    }

    @Nested
    class LoadTests {
        @Test
        void shouldSaveAndLoad_FromDisk() {
            Order restingOrder = new Order();
            restingOrder.setSymbol(SYMBOL);
            restingOrder.setPrice(new BigDecimal("1000"));
            restingOrder.setOrderSide(OrderSide.BUY);

            OrderBookSnapshot snapshot = createSnapshot(List.of(), List.of(restingOrder), 1);
            OrderBookSnapshot snapshot2 = createSnapshot(List.of(), List.of(restingOrder), 2);
            OrderBookSnapshot snapshot3 = createSnapshot(List.of(), List.of(), 3);

            snapShotStore.save(snapshot);
            snapShotStore.save(snapshot2);
            snapShotStore.save(snapshot3);

            Optional<OrderBookSnapshot> latestSnap = snapShotStore.loadLatest(SYMBOL);

            assertTrue(latestSnap.isPresent());
            assertEquals(snapshot3.getSequenceNo(), latestSnap.get().getSequenceNo());
        }
    }

}