package com.nucosmos.pos.backend.store.persistence;

import com.nucosmos.pos.backend.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class StoreEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(nullable = false, length = 10)
    private String currencyCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "receipt_footer_text", length = 1000)
    private String receiptFooterText;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public String getReceiptFooterText() {
        return receiptFooterText;
    }

    public void setReceiptFooterText(String receiptFooterText) {
        this.receiptFooterText = receiptFooterText;
    }
}
