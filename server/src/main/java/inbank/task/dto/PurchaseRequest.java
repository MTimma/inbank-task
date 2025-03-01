package inbank.task.dto;

public record PurchaseRequest(
    String personalId,
    PurchaseDetails details
) {} 