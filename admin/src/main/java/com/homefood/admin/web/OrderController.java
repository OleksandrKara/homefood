package com.homefood.admin.web;

import com.homefood.admin.entity.Client;
import com.homefood.admin.entity.DeliveryType;
import com.homefood.admin.entity.Order;
import com.homefood.admin.entity.OrderItem;
import com.homefood.admin.entity.OrderPayment;
import com.homefood.admin.entity.OrderStatus;
import com.homefood.admin.entity.Product;
import com.homefood.admin.pricing.OrderPricing;
import com.homefood.admin.repository.ClientRepository;
import com.homefood.admin.repository.DistrictRepository;
import com.homefood.admin.repository.OrderItemRepository;
import com.homefood.admin.repository.OrderPaymentRepository;
import com.homefood.admin.repository.OrderRepository;
import com.homefood.admin.repository.PaymentMethodRepository;
import com.homefood.admin.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    // The business operates in PST/PDT; the container runs in UTC, so "today"/"tomorrow"
    // must be computed in the business's zone, not the server's default zone.
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Los_Angeles");

    private static final List<OrderStatus> PROCESSED_STATUSES = List.of(OrderStatus.DONE, OrderStatus.CANCELLED);

    private static final String[] MONTHS_RU = {
            "", "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
    };
    private static final String[] DOW_RU = {
            "", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"
    };

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final DistrictRepository districtRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @GetMapping
    public String list(Model model) {
        List<Order> all = orderRepository.findAllByOrderByDeliveryDateAscDeliveryTimeAsc();

        Map<String, Map<String, List<Order>>> grouped = new LinkedHashMap<>();
        Map<String, Integer> itemCountByDate = new LinkedHashMap<>();
        List<Order> requested = new ArrayList<>();

        for (Order o : all) {
            if (o.isArchived()) {
                continue;
            }
            if (o.getStatus() == OrderStatus.REQUESTED) {
                requested.add(o);
                continue;
            }
            if (PROCESSED_STATUSES.contains(o.getStatus())) {
                continue; // already sold/cancelled - excluded from the "still owed" count below too
            }
            String dateLabel = formatDateLabel(o.getDeliveryDate());
            String subLabel = o.getDeliveryType() == DeliveryType.PICKUP
                    ? "🏠 Самовывоз"
                    : (o.getDistrict() != null && !o.getDistrict().isBlank() ? o.getDistrict() : "Без района");
            grouped.computeIfAbsent(dateLabel, k -> new LinkedHashMap<>())
                    .computeIfAbsent(subLabel, k -> new ArrayList<>())
                    .add(o);
            itemCountByDate.merge(dateLabel, totalQuantity(o), Integer::sum);
        }

        model.addAttribute("requestedOrders", requested);
        model.addAttribute("groupedOrders", grouped);
        model.addAttribute("itemCountByDate", itemCountByDate);
        model.addAttribute("soldTotal", orderRepository.sumTotalPriceByStatus(OrderStatus.DONE));
        model.addAttribute("expectedTotal", orderRepository.sumTotalPriceByStatus(OrderStatus.NEW));
        model.addAttribute("tipsTotal", orderRepository.sumTipAmountByStatus(OrderStatus.DONE));
        model.addAttribute("processedCount", orderRepository.countByArchivedFalseAndStatusIn(PROCESSED_STATUSES));
        model.addAttribute("archivedCount", orderRepository.countByArchivedTrue());
        return "orders/list";
    }

    @GetMapping("/processed")
    public String processed(Model model) {
        model.addAttribute("orders",
                orderRepository.findByArchivedFalseAndStatusInOrderByDeliveryDateDescCreatedAtDesc(PROCESSED_STATUSES));
        return "orders/processed";
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        model.addAttribute("orders", orderRepository.findByArchivedTrueOrderByDeliveryDateDescCreatedAtDesc());
        return "orders/archive";
    }

    @PostMapping("/{id}/archive")
    public String archiveOrder(@PathVariable Long id, @RequestParam(defaultValue = "/orders") String returnTo) {
        Order order = orderRef(id);
        order.setArchived(true);
        orderRepository.save(order);
        return "redirect:" + sanitizeReturnTo(returnTo);
    }

    @PostMapping("/{id}/unarchive")
    public String unarchiveOrder(@PathVariable Long id, @RequestParam(defaultValue = "/orders/archive") String returnTo) {
        Order order = orderRef(id);
        order.setArchived(false);
        orderRepository.save(order);
        return "redirect:" + sanitizeReturnTo(returnTo);
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Order order = new Order();
        addFormAttributes(model, null);
        model.addAttribute("order", order);
        return "orders/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("order") Order order, BindingResult result,
                          @RequestParam Long clientId, HttpServletRequest request, Model model) {
        List<OrderItem> items = buildItems(order, request);
        if (items.isEmpty()) {
            result.reject("items", "Добавьте хотя бы один товар");
        }
        if (result.hasErrors()) {
            addFormAttributes(model, null);
            return "orders/form";
        }
        order.setClient(clientRef(clientId));
        order.setItems(items);
        order.setTotalPrice(sumItems(items));
        order.setPayments(buildPayments(order, request));
        orderRepository.save(order);
        return "redirect:/orders";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Order order = orderRef(id);
        addFormAttributes(model, order);
        model.addAttribute("order", order);
        return "orders/form";
    }

    @Transactional
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("order") Order order, BindingResult result,
                          @RequestParam Long clientId, HttpServletRequest request, Model model) {
        Order existing = orderRef(id);
        List<OrderItem> items = buildItems(order, request);
        if (items.isEmpty()) {
            result.reject("items", "Добавьте хотя бы один товар");
        }
        if (result.hasErrors()) {
            addFormAttributes(model, existing);
            return "orders/form";
        }
        order.setId(id);
        order.setCreatedAt(existing.getCreatedAt());
        order.setArchived(existing.isArchived());
        order.setClient(clientRef(clientId));
        order.setItems(items);
        order.setTotalPrice(sumItems(items));
        order.setPayments(buildPayments(order, request));
        orderItemRepository.deleteByOrderId(id);
        orderPaymentRepository.deleteByOrderId(id);
        orderRepository.save(order);
        return "redirect:/orders";
    }

    private Order orderRef(Long id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private Client clientRef(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
    }

    private Product productRef(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    /**
     * Reads parallel itemProductId[]/itemQuantity[] arrays as raw servlet parameters rather than
     * @RequestParam List<String> - Spring's collection conversion silently comma-splits a
     * single-element list, corrupting a lone numeric value with no error shown (same reasoning as
     * PublicShopController.reserve and RecipeController). Rows with a blank product/quantity or a
     * quantity below 1 are skipped rather than rejected outright, so a stray empty row left in the
     * form doesn't block saving the rest.
     */
    private List<OrderItem> buildItems(Order order, HttpServletRequest request) {
        String[] productIds = request.getParameterValues("itemProductId");
        String[] quantities = request.getParameterValues("itemQuantity");
        int rowCount = productIds == null ? 0 : productIds.length;

        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            String productIdStr = productIds[i];
            String qtyStr = (quantities != null && i < quantities.length) ? quantities[i] : null;
            if (productIdStr == null || productIdStr.isBlank() || qtyStr == null || qtyStr.isBlank()) {
                continue;
            }
            Long productId;
            int quantity;
            try {
                productId = Long.valueOf(productIdStr.trim());
                quantity = Integer.parseInt(qtyStr.trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (quantity < 1) {
                continue;
            }
            Product product = productRef(productId);
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setLineTotal(OrderPricing.calculateTotal(product.getBasePrice(), quantity, product.isTieredDiscountEnabled()));
            items.add(item);
        }
        return items;
    }

    private BigDecimal sumItems(List<OrderItem> items) {
        return items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Reads parallel paymentMethodRow[]/paymentAmountRow[] arrays the same way buildItems() reads
     * the product rows - see that method's javadoc for why raw servlet params instead of
     * @RequestParam List<String>. Unlike items, payments are optional (an order isn't necessarily
     * paid yet), so an empty result is valid and rows with a blank method/amount or a
     * zero-or-negative amount are just skipped rather than rejected.
     */
    private Set<OrderPayment> buildPayments(Order order, HttpServletRequest request) {
        String[] methods = request.getParameterValues("paymentMethodRow");
        String[] amounts = request.getParameterValues("paymentAmountRow");
        int rowCount = methods == null ? 0 : methods.length;

        Set<OrderPayment> payments = new LinkedHashSet<>();
        for (int i = 0; i < rowCount; i++) {
            String method = methods[i];
            String amountStr = (amounts != null && i < amounts.length) ? amounts[i] : null;
            if (method == null || method.isBlank() || amountStr == null || amountStr.isBlank()) {
                continue;
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr.trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (amount.signum() <= 0) {
                continue;
            }
            OrderPayment payment = new OrderPayment();
            payment.setOrder(order);
            payment.setPaymentMethod(method.trim());
            payment.setAmount(amount);
            payments.add(payment);
        }
        return payments;
    }

    private int totalQuantity(Order order) {
        return order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
    }

    /** Active products, plus any product still assigned to the order's current line items even if
     * it's been deactivated since. */
    private List<Product> selectableProducts(Order currentOrder) {
        List<Product> products = new ArrayList<>(productRepository.findAllByActiveTrueOrderByNameAsc());
        if (currentOrder == null) {
            return products;
        }
        for (OrderItem item : currentOrder.getItems()) {
            Product p = item.getProduct();
            if (!p.isActive() && products.stream().noneMatch(existing -> existing.getId().equals(p.getId()))) {
                products.add(p);
            }
        }
        return products;
    }

    /**
     * @param currentOrder the order being edited, if any - its currently-assigned products are
     *                      included even when inactive so their dropdown rows still show/keep them
     *                      selected.
     */
    private void addFormAttributes(Model model, Order currentOrder) {
        model.addAttribute("clients", clientRepository.findAllByOrderByNameAsc());
        model.addAttribute("products", selectableProducts(currentOrder));
        model.addAttribute("deliveryTypes", DeliveryType.values());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("districts", districtRepository.findAllByOrderByNameAsc());
        model.addAttribute("paymentMethods", paymentMethodRepository.findAllByOrderByNameAsc());

        Set<String> addresses = new LinkedHashSet<>(clientRepository.findDistinctAddresses());
        addresses.addAll(orderRepository.findDistinctDeliveryAddresses());
        model.addAttribute("addressSuggestions", addresses);
    }

    private String formatDateLabel(LocalDate date) {
        if (date == null) {
            return "Без даты";
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (date.equals(today)) {
            return "Сегодня";
        }
        if (date.equals(today.plusDays(1))) {
            return "Завтра";
        }
        return date.getDayOfMonth() + " " + MONTHS_RU[date.getMonthValue()] + ", " + DOW_RU[date.getDayOfWeek().getValue()];
    }

    /** Whitelist redirect target so a crafted form field can't send an admin off-app. */
    private String sanitizeReturnTo(String returnTo) {
        if ("/orders/processed".equals(returnTo) || "/orders/archive".equals(returnTo)) {
            return returnTo;
        }
        return "/orders";
    }
}
