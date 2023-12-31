package com.java.order.service;

import com.java.order.dto.InventoryResponse;
import com.java.order.dto.OrderLineItemsDto;
import com.java.order.dto.OrderRequest;
import com.java.order.event.OrderPlacedEvent;
import com.java.order.model.Order;
import com.java.order.model.OrderLineItems;
import com.java.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

       List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDto).toList();
       order.setOrderLineItems(orderLineItems);
       orderLineItems.forEach(item -> item.setOrder(order));
       List<String> skuCodes = order.getOrderLineItems().stream().map(OrderLineItems::getSkuCode).toList();

       Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");
       try (Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
           // Call the inventory service to check the availability of stock
           InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                   .uri("http://inventory-service/api/inventory",
                           uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                   .retrieve()
                   .bodyToMono(InventoryResponse[].class).block();  // block() is used to make synchronous call (default is async)

           boolean allProductsInStock = Arrays.stream(inventoryResponses)
                   .allMatch(InventoryResponse::isInStock);

           if(allProductsInStock){
               orderRepository.save(order);
               kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
               return "Order placed successfully";
           }
           else throw new IllegalArgumentException("Product is not in stock. Please try again.");

       }finally {
           inventoryServiceLookup.end();
       }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = OrderLineItems.builder()
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .skuCode(orderLineItemsDto.getSkuCode())
                .build();

        return  orderLineItems;
    }
}
