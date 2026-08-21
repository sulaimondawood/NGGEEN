package com.dawood.nggeen.trade.infrastructure.journal.chronicle;

import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.model.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.ValueIn;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChronicleQueueService {
    private final ChronicleQueue chronicleQueue;
    private final ThreadLocal<ExcerptAppender> threadAppender = ThreadLocal.withInitial(() -> excerptAppender());

    public long appendEvent(EventType eventType, DomainEvent event) {
        Objects.requireNonNull(event, "DomainEvent must not be null");
        return writeEvent(eventType.name(), event);
    }

    public void replayFrom(long startIndex, BiConsumer<String, DomainEvent> consumer) {
        ExcerptTailer tailer = createTailer();
        if (startIndex > 0) {
            boolean moved = tailer.moveToIndex(startIndex);
            if(!moved){
                log.warn("Could not move to index {}, replaying from start", startIndex);
                tailer.toStart();
            }
        }else {
            tailer.toStart();
        }

        while (true){
            try( DocumentContext dc = tailer.readingDocument()) {
                if(!dc.isPresent()){
                    break;
                }
                String eventType = dc.wire().read("eventType").text();
                DomainEvent event = (DomainEvent) dc.wire().read("event").typedMarshallable();

                if(event != null){
                    consumer.accept(eventType, event);
                }
            }catch (Exception e){
                log.error("Error reading event from Chronicle Queue at index: {}", tailer.index(), e);
                break;
            }
        }
    }

    public ExcerptTailer createNamedTailer(String id) {
        return chronicleQueue.createTailer(id);
    }

    private ExcerptTailer createTailer() {
        return chronicleQueue.createTailer();
    }

    private long writeEvent(String eventType, Marshallable event) {
        ExcerptAppender appender = threadAppender.get();
        appender.writeDocument(w -> {
            w.write("eventType")
                    .text(eventType);
            w.write("event")
                    .typedMarshallable(event);
        });

        return appender.lastIndexAppended();
    }

    private ExcerptAppender excerptAppender() {
        return chronicleQueue.createAppender();
    }

    public void replay(BiConsumer<String, DomainEvent> eventConsumer) {
        ExcerptTailer tailer = createTailer().toStart();
        while (true) {
            try (DocumentContext dc = tailer.readingDocument()) {
                if (!dc.isPresent()) {
                    break;
                }

                String eventType = dc.wire().read("eventType").text();
                DomainEvent event = (DomainEvent) dc.wire().read("event").typedMarshallable();

                if (event != null) {
                    eventConsumer.accept(eventType, event);
                }
            } catch (Exception e) {
                log.error("Error reading event from Chronicle Queue at index: {}", tailer.index(), e);
                break;
            }
        }
    }

}
