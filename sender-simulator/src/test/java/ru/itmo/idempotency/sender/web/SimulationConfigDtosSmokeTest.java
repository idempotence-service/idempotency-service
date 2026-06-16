package ru.itmo.idempotency.sender.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimulationConfigDtosSmokeTest {

    @Test
    void shouldInstantiateRecords() {
        SimulationConfigDtos.SimulationConfigResponse response = new SimulationConfigDtos.SimulationConfigResponse(
                true, "system1-to-system2", 0.25, 2, 5, 0.5
        );
        SimulationConfigDtos.SimulationConfigRequest request = new SimulationConfigDtos.SimulationConfigRequest(
                true, "system1-to-system2", 0.25, 2, 5, 0.0
        );

        Assertions.assertTrue(response.enabled());
        Assertions.assertEquals("system1-to-system2", request.integration());
    }
}
