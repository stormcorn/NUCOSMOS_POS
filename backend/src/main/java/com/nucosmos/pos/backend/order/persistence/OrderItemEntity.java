package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int lineNumber;

    @Column(nullable = false, length = 50)
    private String productSku;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCostAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lineCostAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedCostAmount;

    @Column(length = 255)
    private String note;

    protected OrderItemEntity() {
    }

    public OrderItemEntity(
            OrderEntity order,
            ProductEntity product,
            int lineNumber,
            String productSku,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotalAmount,
            String note
    ) {
        this.order = order;
        this.product = product;
        this.lineNumber = lineNumber;
        this.productSku = productSku;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotalAmount = lineTotalAmount;
        this.unitCostAmount = BigDecimal.ZERO.setScale(2);
        this.lineCostAmount = BigDecimal.ZERO.setScale(2);
        this.refundedCostAmount = BigDecimal.ZERO.setScale(2);
        this.note = note;
    }

    public void applyCost(BigDecimal unitCostAmount, BigDecimal lineCostAmount) {
        this.unitCostAmount = unitCostAmount;
        this.lineCostAmount = lineCostAmount;
    }

    public void applyRefundedCost(BigDecimal refundedCostAmount) {
        this.refundedCostAmount = refundedCostAmount;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotalAmount() {
        return lineTotalAmount;
    }

    public BigDecimal getUnitCostAmount() {
        return unitCostAmount;
    }

    public BigDecimal getLineCostAmount() {
        return lineCostAmount;
    }

    public BigDecimal getRefundedCostAmount() {
        return refundedCostAmount;
    }

    public String getNote() {
        return note;
    }
}
