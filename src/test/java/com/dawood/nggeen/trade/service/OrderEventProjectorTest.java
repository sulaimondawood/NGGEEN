package com.dawood.nggeen.trade.service;

import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.infrastructure.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProjectorTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ChronicleQueueService queueService;

    @InjectMocks
    private OrderEventProjector projector;

    @Test
    void shouldProjectAcceptedAndTradedEvents_InSameBatch(){



    }

}