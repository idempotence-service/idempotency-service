package ru.itmo.idempotency.sender.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimulationConfigDtosSmokeTest {

    @Test
    void shouldInstantiateRecords() {
        SimulationConfigDtos.SimulationConfigResponse response = new SimulationConfigDtos.SimulationConfigResponse(
                true, "system1-to-system2", 2L, 2, 5, 5L
        );
        SimulationConfigDtos.SimulationConfigRequest request = new SimulationConfigDtos.SimulationConfigRequest(
                true, "system1-to-system2", 2L, 2, 5, 0L
        );

        Assertions.assertTrue(response.enabled());
        Assertions.assertEquals("system1-to-system2", request.integration());
    }
}
