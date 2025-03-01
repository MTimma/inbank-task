package inbank.task.dto;

public record PurchaseResponse(
    boolean approved,
    PurchaseDetails details,
    String message
) {} 