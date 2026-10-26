package com.fooddelivery.catalog.restaurant.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRestaurantRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    private String address;

    @Size(max = 30)
    @Pattern(regexp = "^\\s*$|^\\s*\\+?[0-9][0-9 ()-]{6,28}\\s*$")
    private String phoneNumber;
}
