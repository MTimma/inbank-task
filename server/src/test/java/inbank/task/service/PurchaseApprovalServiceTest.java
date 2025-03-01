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
    void testEvaluatePurchase_ifRequestAmountLessThan200_ShouldDenyRequest() {
        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(100, 12));

        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertFalse(response.approved());
        assertNull(response.details());
        assertTrue(response.message().contains("Amount must be between"));
    }

    @Test
    void testEvaluatePurchase_ifRequestAmountMoreThan5000_ShouldDenyRequest() {
        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(6000, 12));

        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertFalse(response.approved());
        assertNull(response.details());
        assertTrue(response.message().contains("Amount must be between"));
    }

    @Test
    void testEvaluatePurchase_ifRequestPeriodLessThan6_ShouldDenyRequest() {
        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(1000, 3));

        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertFalse(response.approved());
        assertNull(response.details());
        assertTrue(response.message().contains("Period must be between"));
    }

    @Test
    void testEvaluatePurchase_ifRequestPeriodMoreThan24_ShouldDenyRequest() {
        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(1000, 36));

        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertFalse(response.approved());
        assertNull(response.details());
        assertTrue(response.message().contains("Period must be between"));
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
    void testAmountHigherThanMaximum_AdjustPeriod() {
        assertTrue(false);//TODO fix
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 50)));

        // For financial factor 50 and 12 months, max amount would be 600
        // Requesting 1000 should result in a smaller valid amount
        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(100, 6));
        PurchaseResponse response = service.evaluatePurchase(request);

        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(600, response.details().amount());
        assertEquals(12, response.details().period());
        assertTrue(response.message().contains("The maximum available offer is"));
    }

    @Test
    void testNoValidOfferFound() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 5))); // Very low financial factor

        PurchaseRequest request = new PurchaseRequest("12345678912", new PurchaseDetails(1000, 12));
        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertFalse(response.approved());
        assertNull(response.details());
        assertEquals("No valid offer found.", response.message());
    }

    @Test
    void testMaximumPossibleAmount() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 500)));

        // With financial factor 500 and 24 months, raw amount would be 12000
        // But should be capped at 5000 (MAX_AMOUNT)
        PurchaseRequest request = new PurchaseRequest("12345678934", new PurchaseDetails(4000, 24));
        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(5000, response.details().amount());
        assertEquals(24, response.details().period());
    }

    @Test
    void testUnknownCustomer() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.empty());

        PurchaseRequest request = new PurchaseRequest("99999999999", new PurchaseDetails(1000, 12));
        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertFalse(response.approved());
        assertNull(response.details());
        assertEquals("Customer is not found.", response.message());
    }

    @Test
    void testEdgeCaseMinimumValidRequest() {
        when(customerProfileDao.getCustomerProfile(anyString()))
            .thenReturn(Optional.of(new CustomerProfile(false, 100)));

        PurchaseRequest request = new PurchaseRequest("12345678923", new PurchaseDetails(200, 6));
        PurchaseResponse response = service.evaluatePurchase(request);
        
        assertTrue(response.approved());
        assertNotNull(response.details());
        assertEquals(600, response.details().amount());
        assertEquals(6, response.details().period());
    }
} 