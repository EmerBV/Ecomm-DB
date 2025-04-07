package com.emerbv.ecommdb.service.order;

import com.emerbv.ecommdb.dto.OrderDto;
import com.emerbv.ecommdb.dto.OrderItemDto;
import com.emerbv.ecommdb.enums.OrderStatus;
import com.emerbv.ecommdb.exceptions.ResourceNotFoundException;
import com.emerbv.ecommdb.model.*;
import com.emerbv.ecommdb.repository.OrderRepository;
import com.emerbv.ecommdb.repository.ProductRepository;
import com.emerbv.ecommdb.repository.ShippingDetailsRepository;
import com.emerbv.ecommdb.repository.VariantRepository;
import com.emerbv.ecommdb.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final CartService cartService;
    private final ShippingDetailsRepository shippingDetailsRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public Order placeOrder(Long userId, Long shippingDetailsId) {
        // Obtener el carrito del usuario
        Cart cart = cartService.getCartByUserId(userId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("No se puede crear una orden con un carrito vacío");
        }

        // Obtener la dirección de envío
        ShippingDetails shippingDetails = shippingDetailsRepository.findById(shippingDetailsId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección de envío no encontrada con ID: " + shippingDetailsId));

        // Verificar que la dirección pertenece al usuario
        if (!shippingDetails.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("La dirección de envío no pertenece al usuario");
        }

        // Crear la orden
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setShippingDetails(shippingDetails);

        // Procesar los items del carrito
        List<OrderItem> orderItemList = createOrderItems(order, cart);
        orderItemList.forEach(order::addOrderItem);

        // Calcular el total
        order.setTotalAmount(order.calculateTotalAmount());

        // Guardar la orden
        Order savedOrder = orderRepository.save(order);

        // Limpiar el carrito
        cartService.clearCart(cart.getId());

        return savedOrder;
    }

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
        productRepository.save(product);
    }

    @Override
    public OrderDto getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
    }

    @Override
    public List<OrderDto> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron órdenes para el usuario con ID: " + userId);
        }
        return orders.stream().map(this::convertToDto).toList();
    }

    @Override
    public OrderDto convertToDto(Order order) {
        OrderDto orderDto = modelMapper.map(order, OrderDto.class);

        // Manejar manualmente la conversión de OrderItems para incluir la información de variantes
        if (order.getOrderItems() != null) {
            orderDto.setItems(order.getOrderItems().stream().map(item -> {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                itemDto.setProductName(item.getProduct() != null ? item.getProduct().getName() : "Producto no disponible");
                itemDto.setProductBrand(item.getProduct() != null ? item.getProduct().getBrand() : "");
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

        // Incluir información de la dirección de envío en el DTO
        if (order.getShippingDetails() != null) {
            orderDto.setShippingDetailsId(order.getShippingDetails().getId());
            orderDto.setShippingAddress(order.getShippingDetails().getAddress());
            orderDto.setShippingCity(order.getShippingDetails().getCity());
            orderDto.setShippingState(order.getShippingDetails().getState());
            orderDto.setShippingPostalCode(order.getShippingDetails().getPostalCode());
            orderDto.setShippingCountry(order.getShippingDetails().getCountry());
            orderDto.setShippingPhoneNumber(order.getShippingDetails().getPhoneNumber());
            orderDto.setShippingFullName(order.getShippingDetails().getFullName());
        }

        // Añadir información de pago al DTO
        orderDto.setPaymentMethod(order.getPaymentMethod());
        orderDto.setPaymentIntentId(order.getPaymentIntentId());

        return orderDto;
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        Order previousState = new Order();
        previousState.setOrderStatus(order.getOrderStatus());
        previousState.setPaymentMethod(order.getPaymentMethod());
        previousState.setPaymentIntentId(order.getPaymentIntentId());

        // Actualizar el estado
        order.setOrderStatus(status);

        // Garantizar que no perdemos la información de pago si ya estaba establecida
        if (order.getPaymentMethod() == null && previousState.getPaymentMethod() != null) {
            order.setPaymentMethod(previousState.getPaymentMethod());
        }

        if (order.getPaymentIntentId() == null && previousState.getPaymentIntentId() != null) {
            order.setPaymentIntentId(previousState.getPaymentIntentId());
        }

        logger.info("Updating order {} status from {} to {}, payment method: {}, payment intent: {}",
                orderId, previousState.getOrderStatus(), status, order.getPaymentMethod(), order.getPaymentIntentId());

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updatePaymentIntent(Long orderId, String paymentIntentId) {
        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalArgumentException("El ID de PaymentIntent no puede estar vacío");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        order.setPaymentIntentId(paymentIntentId);
        logger.info("Updated order {} with payment intent: {}", orderId, paymentIntentId);

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updatePaymentDetails(Long orderId, String paymentIntentId, String paymentMethodId) {
        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalArgumentException("El ID de PaymentIntent no puede estar vacío");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        order.setPaymentIntentId(paymentIntentId);

        if (StringUtils.hasText(paymentMethodId)) {
            order.setPaymentMethod(paymentMethodId);
        }

        // Si el pago ha sido procesado exitosamente, actualizar el estado
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.PAID);
        }

        logger.info("Updated order {} payment details - intent: {}, method: {}, status: {}",
                orderId, paymentIntentId, paymentMethodId, order.getOrderStatus());

        return orderRepository.save(order);
    }
}
