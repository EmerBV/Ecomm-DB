package com.emerbv.ecommdb.service.order;

import com.emerbv.ecommdb.dto.OrderDto;
import com.emerbv.ecommdb.dto.OrderItemDto;
import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.*;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.ProductRepository;
import com.emerbv.ecommdb.repository.VariantRepository;
import com.emerbv.ecommdb.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final CartService cartService;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public Order placeOrder(Long userId) {
        Cart cart   = cartService.getCartByUserId(userId);
        Order order = createOrder(cart);
        List<OrderItem> orderItemList = createOrderItems(order, cart);
        order.setOrderItems(new HashSet<>(orderItemList));
        order.setTotalAmount(calculateTotalAmount(orderItemList));
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(cart.getId());
        return savedOrder;
    }

    private Order createOrder(Cart cart) {
        Order order = new Order();
        // Set the user...
        order.setUser(cart.getUser());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDate.now());
        return  order;
    }

    /*
    private List<OrderItem> createOrderItems(Order order, Cart cart) {
        return  cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            product.setInventory(product.getInventory() - cartItem.getQuantity());
            productRepository.save(product);
            return  new OrderItem(
                    order,
                    product,
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice());
        }).toList();
    }
     */

    private List<OrderItem> createOrderItems(Order order, Cart cart) {
        return cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();

            // Actualizar inventario
            updateInventory(product, cartItem);

            // Actualizar número de ventas
            updateSalesCount(product, cartItem);

            // Crear OrderItem con los campos básicos
            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice()
            );

            // Si hay información de variante, establecerla
            if (cartItem.getVariantId() != null) {
                orderItem.setVariantId(cartItem.getVariantId());
                orderItem.setVariantName(cartItem.getVariantName());
            }

            return orderItem;
        }).toList();
    }

    private void updateInventory(Product product, CartItem cartItem) {
        // Si el item tiene variantId, actualizar el inventario de la variante
        if (cartItem.getVariantId() != null) {
            Optional<Variant> variantOptional = variantRepository.findById(cartItem.getVariantId());
            if (variantOptional.isPresent()) {
                Variant variant = variantOptional.get();
                variant.setInventory(variant.getInventory() - cartItem.getQuantity());
                // Actualizar también el inventario total del producto
                product.updateProductDetails();
                productRepository.save(product);
            }
        } else {
            // Si no tiene variante, actualizar directamente el inventario del producto
            product.setInventory(product.getInventory() - cartItem.getQuantity());
            productRepository.save(product);
        }
    }

    private void updateSalesCount(Product product, CartItem cartItem) {
        product.setSalesCount(product.getSalesCount() + cartItem.getQuantity());
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> orderItemList) {
        return  orderItemList
                .stream()
                .map(item -> item.getPrice()
                        .multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public OrderDto getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("No orders found"));
    }

    @Override
    public List<OrderDto> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return  orders.stream().map(this::convertToDto).toList();
    }

    /*
    @Override
    public OrderDto convertToDto(Order order) {
        return modelMapper.map(order, OrderDto.class);
    }
     */

    @Override
    public OrderDto convertToDto(Order order) {
        OrderDto orderDto = modelMapper.map(order, OrderDto.class);

        // Manejar manualmente la conversión de OrderItems para incluir la información de variantes
        if (order.getOrderItems() != null) {
            orderDto.setItems(order.getOrderItems().stream().map(item -> {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setProductId(item.getProduct().getId());
                itemDto.setProductName(item.getProduct().getName());
                itemDto.setProductBrand(item.getProduct().getBrand());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setPrice(item.getPrice());
                itemDto.setTotalPrice(item.getTotalPrice());

                // Agregar información de la variante si existe
                if (item.getVariantId() != null) {
                    itemDto.setVariantId(item.getVariantId());
                    itemDto.setVariantName(item.getVariantName());
                }

                return itemDto;
            }).toList());
        }

        return orderDto;
    }
}
