package inbank.task.service;

import inbank.task.dao.CustomerProfileDao;
import inbank.task.dto.CustomerProfile;
import inbank.task.dto.PurchaseDetails;
import inbank.task.dto.PurchaseRequest;
import inbank.task.dto.PurchaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PurchaseApprovalServiceTest {

    @Mock
    private CustomerProfileDao customerProfileDao;

    private PurchaseApprovalService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PurchaseApprovalService(customerProfileDao);
    }

    @Test
    void testEvaluatePurchase_ifCustomerFlagged_ShouldDenyRequest() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(true, -1)));
        PurchaseRequest request = new PurchaseRequest("12345678901", new PurchaseDetails(1000, 12));

        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertFalse(response.approved());
        assertNull(response.details());
        assertEquals("Customer is flagged.", response.message());
    }

    @Test
    void testEvaluatePurchase_ifPurchaseRequestValidHighest_ShouldReturnSame() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 100)));
        // For financial factor 100 and 24 months, max amount would be 2400
        PurchaseRequest request = new PurchaseRequest("12345678923", new PurchaseDetails(2400, 24));

        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(2400, response.details().amount());
        assertEquals(24, response.details().period());
        assertTrue(response.message().contains("same as the requested amount"));
    }

    @Test
    void testEvaluatePurchase_ifPurchaseRequestValidNotHighest_ShouldCalculateHighest() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 100)));
        // For financial factor 100 and 12 months, max amount would be 1200
        PurchaseRequest request = new PurchaseRequest("12345678923", new PurchaseDetails(800, 12));

        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(1200, response.details().amount());
        assertEquals(12, response.details().period());
        assertTrue(response.message().contains("maximum available offer"));
    }

    @Test
    void testEvaluatePurchase_AmountLessThanMinimum_AdjustPeriod() {
        // financial factor of 15 should result in less than acceptable minimum for  6 months
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 15)));
        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(50, 6));

        PurchaseResponse response = service.evaluatePurchase(request);

        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(210, response.details().amount());
        assertEquals(14, response.details().period());
        assertTrue(response.message().contains("Nearest offer"));
    }

    @Test
    void testEvaluatePurchase_NoValidOffer_ShouldDenyRequest() {
        // financial factor of 5 should not allow to find a valid purchase amount
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 5)));
        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(50, 1));

        PurchaseResponse response = service.evaluatePurchase(request);

        assertFalse(response.approved());
        assertNull(response.details());
        assertTrue(response.message().contains("No valid offer found"));
    }


    @Test
    void testEvaluatePurchase_MaxPossibleRequestWith100FinancialFactor_ShouldApprove() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 500)));

        // With financial factor 500 and 24 months, raw amount would be 12000
        // But should be capped at 5000 (MAX_AMOUNT)
        PurchaseRequest request = new PurchaseRequest("12345678934", new PurchaseDetails(Integer.MAX_VALUE, Integer.MAX_VALUE));
        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(5000, response.details().amount());
        assertEquals(24, response.details().period());
    }

    @Test
    void testEvaluatePurchase_MinPossibleRequestWith100FinancialFactor_ShouldApprove() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 100)));

        PurchaseRequest request = new PurchaseRequest("12345678923", new PurchaseDetails(-1*Integer.MAX_VALUE, -1*Integer.MAX_VALUE));
        PurchaseResponse response = service.evaluatePurchase(request);

        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(600, response.details().amount());
        assertEquals(6, response.details().period());
    }

    @Test
    void testEvaluatePurchase_UnknownCustomer_ShouldDenyRequest()
    {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.empty());

        PurchaseRequest request = new PurchaseRequest("99999999999", new PurchaseDetails(1000, 12));
        PurchaseResponse response = service.evaluatePurchase(request);

        assertFalse(response.approved());
        assertNull(response.details());
        assertEquals("Customer is not found.", response.message());
    }
} 