package com.homefood.admin.web;

import com.homefood.admin.entity.OrderStatus;
import com.homefood.admin.repository.ExpenseRepository;
import com.homefood.admin.repository.OrderItemRepository;
import com.homefood.admin.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Big-screen "wow effect" summary meant for a TV (or a phone mirrored to one) in kiosk mode, not
 * day-to-day admin work - see dashboard/index.html and the plain meta-refresh reload (kiosk
 * browsers are often too flaky for long-running JS timers to be worth the risk).
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    // Same business zone as OrderController - "today"/"this week" must follow the business's
    // clock, not the container's UTC one.
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Los_Angeles");
    private static final int TOP_PRODUCTS_LIMIT = 3;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ExpenseRepository expenseRepository;

    @GetMapping
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);

        // Headline duo: always-growing lifetime revenue next to the honest bottom line (profit -
        // colored green/red in the template depending on sign) so the two biggest numbers on
        // screen tell the whole financial story at a glance.
        BigDecimal revenueAllTime = orderRepository.sumTotalPriceByStatus(OrderStatus.DONE);
        BigDecimal expensesAllTime = expenseRepository.sumAmount();
        BigDecimal profitAllTime = revenueAllTime.subtract(expensesAllTime);
        model.addAttribute("revenueAllTime", revenueAllTime);
        model.addAttribute("expensesAllTime", expensesAllTime);
        model.addAttribute("profitAllTime", profitAllTime);

        // Momentum: is the business active right now.
        model.addAttribute("revenueToday", orderRepository.sumTotalPriceByStatusAndDeliveryDate(OrderStatus.DONE, today));
        model.addAttribute("revenueWeek", orderRepository.sumTotalPriceByStatusAndDeliveryDateBetween(OrderStatus.DONE, weekStart, today));
        model.addAttribute("revenueMonth", orderRepository.sumTotalPriceByStatusAndDeliveryDateBetween(OrderStatus.DONE, monthStart, today));

        // Scale/reach: the "look how far we've come" numbers.
        long paidOrders = orderRepository.countByStatus(OrderStatus.DONE);
        model.addAttribute("paidOrders", paidOrders);
        model.addAttribute("clientsServed", orderRepository.countDistinctClientsByStatus(OrderStatus.DONE));
        model.addAttribute("unitsSold", orderItemRepository.sumQuantityByOrderStatus(OrderStatus.DONE));
        model.addAttribute("tipsAllTime", orderRepository.sumTipAmountByStatus(OrderStatus.DONE));
        model.addAttribute("avgOrderValue", paidOrders == 0
                ? BigDecimal.ZERO
                : revenueAllTime.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP));

        model.addAttribute("topProducts",
                orderItemRepository.topProductsByRevenue(OrderStatus.DONE).stream().limit(TOP_PRODUCTS_LIMIT).toList());

        return "dashboard/index";
    }
}
