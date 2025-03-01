package inbank.task.dao;

import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;

import inbank.task.dto.CustomerProfile;

@Repository
public class CustomerProfileDao {
    private final Map<String, CustomerProfile> customerProfiles = Map.of(
        "12345678901", new CustomerProfile(true, -1), //ineligible
        "12345678912", new CustomerProfile(false, 50), //profile_1
        "12345678923", new CustomerProfile(false, 100), //profile_2
        "12345678934", new CustomerProfile(false, 500) //profile_3
    );

    public Optional<CustomerProfile> getCustomerProfile(String personalId) {
        return Optional.ofNullable(customerProfiles.get(personalId));
    }
} 