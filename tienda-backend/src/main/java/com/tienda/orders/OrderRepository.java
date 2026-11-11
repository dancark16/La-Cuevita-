package com.tienda.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    long countByCouponId(UUID couponId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    @org.springframework.data.jpa.repository.Query("""
            SELECT o FROM Order o JOIN o.customer c
            WHERE (:status IS NULL OR CAST(o.status AS string) = CAST(:status AS string))
            AND (:q IS NULL
                 OR CAST(o.orderNumber AS string) LIKE CONCAT('%', CAST(:q AS string), '%')
                 OR LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                 OR LOWER(CONCAT(COALESCE(c.firstName, ''), ' ', COALESCE(c.lastName, ''), ' ', COALESCE(c.businessName, '')))
                    LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Order> searchAdmin(@org.springframework.data.repository.query.Param("status") String status,
                            @org.springframework.data.repository.query.Param("q") String q,
                            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    java.util.List<Object[]> countByStatus();
}
