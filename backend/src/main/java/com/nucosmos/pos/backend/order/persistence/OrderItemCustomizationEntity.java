package com.nucosmos.pos.backend.order.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationGroupEntity;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationOptionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_customizations")
public class OrderItemCustomizationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItemEntity orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_customization_group_id")
    private ProductCustomizationGroupEntity productCustomizationGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_customization_option_id")
    private ProductCustomizationOptionEntity productCustomizationOption;

    @Column(name = "group_name", nullable = false, length = 80)
    private String groupName;

    @Column(name = "option_name", nullable = false, length = 80)
    private String optionName;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    protected OrderItemCustomizationEntity() {
    }

    public static OrderItemCustomizationEntity create(
            OrderItemEntity orderItem,
            ProductCustomizationGroupEntity productCustomizationGroup,
            ProductCustomizationOptionEntity productCustomizationOption,
            int lineNumber
    ) {
        OrderItemCustomizationEntity entity = new OrderItemCustomizationEntity();
        entity.orderItem = orderItem;
        entity.productCustomizationGroup = productCustomizationGroup;
        entity.productCustomizationOption = productCustomizationOption;
        entity.groupName = productCustomizationGroup.getName();
        entity.optionName = productCustomizationOption.getName();
        entity.priceDelta = productCustomizationOption.getPriceDelta();
        entity.lineNumber = lineNumber;
        return entity;
    }

    public OrderItemEntity getOrderItem() {
        return orderItem;
    }

    public ProductCustomizationGroupEntity getProductCustomizationGroup() {
        return productCustomizationGroup;
    }

    public ProductCustomizationOptionEntity getProductCustomizationOption() {
        return productCustomizationOption;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getOptionName() {
        return optionName;
    }

    public BigDecimal getPriceDelta() {
        return priceDelta;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
