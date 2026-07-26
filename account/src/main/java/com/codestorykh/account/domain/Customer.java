package com.codestorykh.account.domain;

import com.codestorykh.account.type.Gender;
import com.codestorykh.account.type.IdentityType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "tbl_customer",
        indexes = {
                // 1. CIF Lookup: Fast core banking identification
                @Index(name = "idx_customer_number", columnList = "customer_number"),

                // 2. KYC Lookup: Essential for deduplication and onboarding checks
                @Index(name = "idx_customer_identity", columnList = "identity_type, identity_number"),

                // 3. Email Search: Fast login and communication lookup
                @Index(name = "idx_customer_email", columnList = "email"),

                // 4. Customer Name Search: Enables efficient partial or full name queries
                @Index(name = "idx_customer_full_name", columnList = "full_name"),

                // 5. Geographic/Branch Filtering: Optimized for location-based reporting or regional filtering
                @Index(name = "idx_customer_location", columnList = "province, district")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", nullable = false, unique = true, length = 20)
    private String customerNumber;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false)
    private IdentityType identityType;

    @Column(name = "identity_number", nullable = false, unique = true, length = 50)
    private String identityNumber;

    @Column(nullable = false, length = 20)
    private String status;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "province", nullable = false)
    private String province;

    @Column(name = "district", nullable = false)
    private String district;

    @Column(name = "commune", nullable = false)
    private String commune;

    @Column(name = "village", nullable = false)
    private String village;

    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;
}