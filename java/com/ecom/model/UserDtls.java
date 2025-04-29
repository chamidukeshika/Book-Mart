package com.ecom.model;

import java.util.Date;

import jakarta.persistence.Entity; // Specifies that the class is an entity and is mapped to a database table.
import jakarta.persistence.GeneratedValue; // Specifies the generation strategy for primary keys.
import jakarta.persistence.GenerationType; // Enumerates the generation strategies.
import jakarta.persistence.Id; // Marks a field as the primary key.
import lombok.AllArgsConstructor; // Generates a constructor with all fields.
import lombok.Getter; // Generates getter methods for all fields.
import lombok.NoArgsConstructor; // Generates a no-argument constructor.
import lombok.Setter; // Generates setter methods for all fields.

/**
 * This class represents the User Details entity.
 * It includes fields for user information, account status, and security-related details.
 * The annotations used here ensure the entity is properly configured for use with JPA.
 */
@AllArgsConstructor // Lombok annotation to generate a constructor with all fields.
@NoArgsConstructor // Lombok annotation to generate a no-arguments constructor.
@Getter // Lombok annotation to generate getter methods for all fields.
@Setter // Lombok annotation to generate setter methods for all fields.
@Entity // Marks this class as a JPA entity mapped to a database table.
public class UserDtls {

    @Id // Marks the 'id' field as the primary key.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Specifies that the primary key is auto-generated.
    private Integer id;

    // Field to store the user's name.
    private String name;

    // Field to store the user's mobile number.
    private String mobileNumber;

    // Field to store the user's email address.
    private String email;

    // Field to store the user's residential address.
    private String address;

    // Field to store the user's city of residence.
    private String city;

    // Field to store the user's state of residence.
    private String state;

    // Field to store the user's postal code.
    private Integer pincode;

    // Field to store the user's account password.
    private String password;

    // Field to store the file path or URL of the user's profile image.
    private String profileImage;

    // Field to store the user's role (e.g., admin, customer).
    private String role;

    // Field to indicate if the user's account is enabled.
    private Boolean isEnable;

    // Field to indicate if the user's account is locked or not.
    private Boolean accountNonLocked;

    // Field to store the number of failed login attempts by the user.
    private Integer failedAttempt;

    // Field to store the timestamp of when the account was locked.
    private Date lockTime;

    // Field to store the reset token for password recovery.
    private String resetToken;

}
