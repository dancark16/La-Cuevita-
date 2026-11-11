package com.tienda.customers;

import com.tienda.customers.dto.AddressRequest;
import com.tienda.customers.dto.CustomerProfileRequest;
import com.tienda.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public Customer getOrCreateForUser(User user, CustomerProfileRequest request) {
        return customerRepository.findByUserId(user.getId())
                .map(customer -> updateIfProvided(customer, request))
                .orElseGet(() -> createProfile(user, request));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findCustomerIdByUserId(UUID userId) {
        return customerRepository.findByUserId(userId).map(Customer::getId);
    }

    @Transactional
    public Address addAddress(Customer customer, AddressRequest request) {
        Address address = Address.builder()
                .customer(customer)
                .line1(request.line1())
                .line2(request.line2())
                .city(request.city())
                .province(request.province())
                .reference(request.reference())
                .defaultAddress(false)
                .build();

        return addressRepository.save(address);
    }

    private Customer createProfile(User user, CustomerProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Necesitamos tus datos de facturación para tu primer pedido");
        }

        Customer customer = Customer.builder()
                .user(user)
                .idType(request.idType())
                .idNumber(request.idNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .businessName(request.businessName())
                .email(user.getEmail())
                .phone(request.phone())
                .build();

        return customerRepository.save(customer);
    }

    private Customer updateIfProvided(Customer customer, CustomerProfileRequest request) {
        if (request == null) {
            return customer;
        }
        customer.setIdType(request.idType());
        customer.setIdNumber(request.idNumber());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setBusinessName(request.businessName());
        customer.setPhone(request.phone());
        return customerRepository.save(customer);
    }
}
