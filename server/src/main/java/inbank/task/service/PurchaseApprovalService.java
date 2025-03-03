package inbank.task.service;

import inbank.task.dao.CustomerProfileDao;
import inbank.task.dto.PurchaseDetails;
import inbank.task.dto.PurchaseRequest;
import inbank.task.dto.PurchaseResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PurchaseApprovalService {
    private static final double MIN_AMOUNT = 200;
    private static final double MAX_AMOUNT = 5000;
    private static final int MIN_PERIOD = 6;
    private static final int MAX_PERIOD = 24;

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
        var customerProfileOptional = customerProfileDao.getCustomerProfile(request.personalId());
        if (customerProfileOptional.isEmpty()) {
            return new PurchaseResponse(false, null, "Customer is not found.");
        }
        if (customerProfileOptional.get().flagged()) {
            return new PurchaseResponse(false, null, "Customer is flagged.");
        }

        double amount = sanitizeRequestAmount(request);
        int period = sanitizeRequestPeriod(request);

        int financialFactor = customerProfileOptional.get().financialFactor();
        double approvalScore = calculateApprovalScore(financialFactor, amount, period);

        if (approvalScore == 1) {//maximum amount for period is requested
            return new PurchaseResponse(true, request.details(), 
                "The maximum available offer is the same as the requested amount €" + amount);
        } 
        
        double maxAmount = calculateMaxAmountForPeriod(financialFactor, period);

        if (maxAmount >= MIN_AMOUNT) {
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
     *
     * These constraints should apply only to the response values
     * Otherwise {@link PurchaseApprovalService#findNearestValidPurchaseDetails} cannot be tested
     *
     * @param request
     * @return amount that is between {@link PurchaseApprovalService.MIN_AMOUNT} and {@link PurchaseApprovalService.MAX_AMOUNT}
     */
    private double sanitizeRequestAmount(PurchaseRequest request) {
        return Math.max(Math.min(request.details().amount(), MAX_AMOUNT), MIN_AMOUNT);
    }

    /**
     * Constraints:
     * ● Minimum and maximum payment periods: 6 months - 24 months
     *
     * These constraints should apply only to the response values
     * Otherwise {@link PurchaseApprovalService#findNearestValidPurchaseDetails} cannot be tested
     *
     * @param request
     * @return period that is between {@link PurchaseApprovalService.MIN_MONTHS} and {@link PurchaseApprovalService.MAX_MONTHS}
     */
    private int sanitizeRequestPeriod(PurchaseRequest request) {
        return Math.max(Math.min(request.details().period(), MAX_PERIOD), MIN_PERIOD);
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
        return Math.min(maxAmount, MAX_AMOUNT);
    }

    /**
     * If a suitable purchase amount is not found for the requested payment period,
     * the system should attempt to adjust the payment period to find a suitable match
     *
     * @param financialFactor
     * @param period
     * @return PurchaseDetails
     * PurchaseDetails.period closest value with valid amount after period value
     * PurchaseDetails.amount calculated highest valid amount per closest valid period
     * Optional.empty if such period could not be found
     */
    private Optional<PurchaseDetails> findNearestValidPurchaseDetails(int financialFactor, int period) {
        for (int months = period+1; months <= MAX_PERIOD; months++) {
            double amount = calculateMaxAmountForPeriod(financialFactor, months);
            if (isValidAmount(amount)) {
                return Optional.of(new PurchaseDetails(amount, months));
            }
        }
        return Optional.empty();
    }

    private boolean isValidAmount(double amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;
    }
}