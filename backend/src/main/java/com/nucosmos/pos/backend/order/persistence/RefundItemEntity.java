package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "refund_items")
public class RefundItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false)
    private RefundEntity refund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItemEntity orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 20)
    private String inventoryDisposition;

    protected RefundItemEntity() {
    }

    public RefundItemEntity(
            RefundEntity refund,
            OrderItemEntity orderItem,
            ProductEntity product,
            int quantity,
            String inventoryDisposition
    ) {
        this.refund = refund;
        this.orderItem = orderItem;
        this.product = product;
        this.quantity = quantity;
        this.inventoryDisposition = inventoryDisposition;
    }

    public OrderItemEntity getOrderItem() {
        return orderItem;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getInventoryDisposition() {
        return inventoryDisposition;
    }
}
