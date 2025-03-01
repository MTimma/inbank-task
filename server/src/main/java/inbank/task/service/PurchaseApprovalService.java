package inbank.task.service;

import inbank.task.dao.CustomerProfileDao;
import inbank.task.dto.PurchaseDetails;
import inbank.task.dto.PurchaseRequest;
import inbank.task.dto.PurchaseResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PurchaseApprovalService {
    private static final int MIN_AMOUNT = 200;
    private static final int MAX_AMOUNT = 5000;
    private static final int MIN_MONTHS = 6;
    private static final int MAX_MONTHS = 24;

    private final CustomerProfileDao customerProfileDao;

    public PurchaseApprovalService(CustomerProfileDao customerProfileDao) {
        this.customerProfileDao = customerProfileDao;
    }

    /**
     * Will approve a purchase request and determine the maximum purchase amount
     * 
     * @param request
     * @return 
     * PurchaseResponse.approved - false if user is flagged or it is not possible to calculate a valid offer, otherwise true
     * PurchaseResponse.details.amount - maximum amount that we are willing to approve
     * PurchaseResponse.details.period - requested period or nearest period we are willing to approve
     * or PurchaseResponse.details - null if PurchaseResponse.approved = false
     */
    public PurchaseResponse evaluatePurchase(PurchaseRequest request) {
        Optional<PurchaseResponse> invalidRequestResponse = validateRequest(request);
        if(invalidRequestResponse.isPresent()) {
            return invalidRequestResponse.get();
        }

        var customerProfileOptional = customerProfileDao.getCustomerProfile(request.personalId());
        if (customerProfileOptional.isEmpty()) {
            return new PurchaseResponse(false, null, "Customer is not found.");
        }
        if (customerProfileOptional.get().flagged()) {
            return new PurchaseResponse(false, null, "Customer is flagged.");
        }

        double amount = request.details().amount();
        int period = request.details().period();
        int financialFactor = customerProfileOptional.get().financialFactor();
        double approvalScore = calculateApprovalScore(financialFactor, amount, period);

        if (approvalScore == 1) {//maximum amount for period is requested
            return new PurchaseResponse(true, request.details(), 
                "The maximum available offer is the same as the requested amount €" + amount);
        } 
        
        double maxAmount = calculateMaxAmountForPeriod(financialFactor, period);

        if (maxAmount > MIN_AMOUNT) {
            return new PurchaseResponse(true, new PurchaseDetails(maxAmount, period),
                "The maximum available offer is €" + maxAmount);
        }

        var validPurchaseDetailsOptional = findNearestValidPurchaseDetails(financialFactor, period);
        if (validPurchaseDetailsOptional.isPresent()) {
            return new PurchaseResponse(true, validPurchaseDetailsOptional.get(),
                "Nearest offer - €" + validPurchaseDetailsOptional.get().amount() + " in " + validPurchaseDetailsOptional.get().period() + " months");
        }

        return new PurchaseResponse(false, null, "No valid offer found.");
    }

    /**
     * Constraints:
     * ● Minimum and maximum purchase amounts: €200 – €5000
     * ● Minimum and maximum payment periods: 6 months - 24 months
     *
     * A possible approach would be to calculate a valid offer also if the API request is outside the constraints
     * But I do not think implementing this corner case will show anything positive
     * (for example, if it improved UX, I would consider it)
     * so I am keeping it simple and denying anything outside the constraints
     *
     * @param request
     * @return PurchaseResponse if request is not valid, otherwise Optional.empty
     */
    private Optional<PurchaseResponse> validateRequest(PurchaseRequest request) {
        if (!isValidAmount(request.details().amount())) {
            return Optional.of(new PurchaseResponse(false, null,
                "Amount must be between €" + MIN_AMOUNT + " and €" + MAX_AMOUNT));
        }

        if (!isValidPeriod(request.details().period())) {
            return Optional.of(new PurchaseResponse(false, null,
                "Period must be between " + MIN_MONTHS + " and " + MAX_MONTHS + " months"));
        }
        return Optional.empty();
    }

    /**
     * Approval Score = (financial capacity factor / requested purchase amount) * payment period 
     * If approval score = 1, the highest possible amount is requested
     * If approval score > 1, amount is less than the maximum purchase amount for user
     * If approval score < 1, amount is higher than the maximum purchase amount for user
     */
    private double calculateApprovalScore(int financialFactor, double amount, int months) {
        return (financialFactor / amount) * months;
    }

    private double calculateMaxAmountForPeriod(int financialFactor, int months) {
        double maxAmount = financialFactor * months;
        return maxAmount > MAX_AMOUNT ? MAX_AMOUNT : maxAmount;
    }

    private boolean isValidAmount(double amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;
    }

    private boolean isValidPeriod(int months) {
        return months >= MIN_MONTHS && months <= MAX_MONTHS;
    }

    private Optional<PurchaseDetails> findNearestValidPurchaseDetails(int financialFactor, int period) {
        for (int months = period+1; months <= MAX_MONTHS; months++) {
            double amount = calculateMaxAmountForPeriod(financialFactor, months);
            if (isValidAmount(amount)) {
                return Optional.of(new PurchaseDetails(amount, months));
            }
        }
        return Optional.empty();
    }
} 