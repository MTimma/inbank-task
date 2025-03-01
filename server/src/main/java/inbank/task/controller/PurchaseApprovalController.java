package inbank.task.controller;

import inbank.task.dto.PurchaseRequest;
import inbank.task.dto.PurchaseResponse;
import inbank.task.service.PurchaseApprovalService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") // For Vite default port
public class PurchaseApprovalController {
    private final PurchaseApprovalService purchaseApprovalService;

    public PurchaseApprovalController(PurchaseApprovalService purchaseApprovalService) {
        this.purchaseApprovalService = purchaseApprovalService;
    }

    @PostMapping("/evaluate-purchase")
    public PurchaseResponse evaluatePurchase(@RequestBody PurchaseRequest request) {
        return purchaseApprovalService.evaluatePurchase(request);
    }
} 