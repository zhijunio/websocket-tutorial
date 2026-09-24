package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PresenceControllerTests {
    @Test
    void presenceQueryCannotSelectAnotherTenant() {
        AppProperties properties = new AppProperties();
        properties.setTenantId("tenant-a");
        PresenceController controller = new PresenceController(mock(PresenceService.class), properties);

        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> controller.users("tenant-b", null, null, null, 0, 100));
    }

    @Test
    void absentTenantQueryUsesAuthenticatedApplicationTenant() {
        AppProperties properties = new AppProperties();
        properties.setTenantId("tenant-a");
        PresenceService presence = mock(PresenceService.class);
        org.mockito.Mockito.when(presence.find("tenant-a", null, null, null, 0, 100))
                .thenReturn(java.util.List.of());
        PresenceController controller = new PresenceController(presence, properties);

        Map<String, Object> response = controller.users(null, null, null, null, 0, 100);

        assertEquals(0L, response.get("count"));
        org.mockito.Mockito.verify(presence).find("tenant-a", null, null, null, 0, 100);
    }
}
