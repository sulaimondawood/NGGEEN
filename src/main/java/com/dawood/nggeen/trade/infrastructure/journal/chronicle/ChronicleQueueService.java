package com.dawood.nggeen.trade.infrastructure.journal.chronicle;

import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.model.enums.EventType;
import lombok.RequiredArgsConstructor;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.Marshallable;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ChronicleQueueService {
    private final ChronicleQueueConfig chronicleQueueConfig;
    private final ThreadLocal<ExcerptAppender> threadAppender = ThreadLocal.withInitial(()->excerptAppender());

    public void appendEvent(EventType eventType, DomainEvent event){
        Objects.requireNonNull(event, "DomainEvent must not be null");
        writeEvent(eventType.name(),event);
    }

    public ExcerptTailer createTailer(){
        return chronicleQueueConfig.chronicleQueue().createTailer();
    }

    private void writeEvent(String eventType, Marshallable event){
        ExcerptAppender appender = threadAppender.get();
       appender.writeDocument(w->w.write(eventType)
               .typedMarshallable(event));
    }

    private ExcerptAppender excerptAppender(){
        return chronicleQueueConfig.chronicleQueue().createAppender();
    }

}
