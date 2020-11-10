/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.samples.spring;

import com.datastax.driver.core.utils.UUIDs;

import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequestMapping(path = "/orders")
public class OrderController {

    public OrderController() throws Exception {
    }

    // create spring data template with session
    CassandraOperations template = new CassandraTemplate(Util.getSession());

    @PostMapping
    public @ResponseBody String createOrder(@RequestBody Order order) {
        order.setId(UUIDs.timeBased());
        order.setTime(Instant.now().toString());
        template.insert(order);
        System.out.println("Added order ID " + order.getId().toString());
        return String.format("Added order ID %s.", order.getId().toString());
    }

    @GetMapping("/{id}")
    public @ResponseBody Order getOrder(@PathVariable UUID id) {
        return template.selectOneById(id, Order.class);
    }
}
