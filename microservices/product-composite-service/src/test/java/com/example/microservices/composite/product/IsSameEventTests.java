package com.example.microservices.composite.product;

import com.example.api.core.product.Product;
import com.example.api.event.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class IsSameEventTests {
    ObjectMapper mapper = new ObjectMapper();

    @Test
    void testEventObjectCompare() throws JsonProcessingException {
        // Event #1 and #2 are the same event, but occurs at different times
        // Event #3 and #4 are different events
        Event<Integer, Product> event1 = new Event<>(Event.Type.CREATE, 1,
                new Product(1, "name", 1, null));
        Event<Integer, Product> event2 = new Event<>(Event.Type.CREATE, 1,
                new Product(1, "name", 1, null));
        Event<Integer, Product> event3 = new Event<>(Event.Type.DELETE, 1,
                null);
        Event<Integer, Product> event4 = new Event<>(Event.Type.CREATE, 2,
                new Product(2, "name", 2, null));
        String event1Json = mapper.writeValueAsString(event1);

        MatcherAssert.assertThat(event1Json, Matchers.is(IsSameEvent.sameEventExceptCreatedAt(event2)));
        MatcherAssert.assertThat(event1Json, Matchers.not(IsSameEvent.sameEventExceptCreatedAt(event3)));
        MatcherAssert.assertThat(event1Json, Matchers.not(IsSameEvent.sameEventExceptCreatedAt(event4)));
    }
}
