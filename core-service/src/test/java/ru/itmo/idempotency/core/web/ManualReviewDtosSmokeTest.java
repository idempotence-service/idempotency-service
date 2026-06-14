package ru.itmo.idempotency.core.web;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

class ManualReviewDtosSmokeTest {

    @Test
    void shouldInstantiateAllRecords() {
        ManualReviewDtos.ErrorEventsQuery query = new ManualReviewDtos.ErrorEventsQuery(0, 20, "createDate,desc");
        ManualReviewDtos.RestartEventRequest restart = new ManualReviewDtos.RestartEventRequest("gk-1");
        ManualReviewDtos.ErrorEventItem item = new ManualReviewDtos.ErrorEventItem("ERROR", "gk-1", "svc", "int");
        ManualReviewDtos.EventDetails details = new ManualReviewDtos.EventDetails(
                "gk-1", "uid-1", "svc", "int", "ERROR", "desc",
                OffsetDateTime.now(), OffsetDateTime.now(),
                JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode()
        );
        ManualReviewDtos.DuplicateEventItem duplicate = new ManualReviewDtos.DuplicateEventItem(
                "gk-1", "svc", "int", "duplicate", OffsetDateTime.now()
        );

        Assertions.assertEquals("gk-1", restart.globalKey());
        Assertions.assertEquals("ERROR", item.status());
        Assertions.assertEquals("gk-1", details.globalKey());
        Assertions.assertEquals(0, query.page());
        Assertions.assertEquals("duplicate", duplicate.reason());
    }
}
