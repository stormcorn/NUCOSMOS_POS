package com.nucosmos.pos.backend.product;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping
    public ApiResponse<List<ProductSummaryResponse>> listProducts() {
        return ApiResponse.ok(productQueryService.listAvailableProducts());
    }
}
