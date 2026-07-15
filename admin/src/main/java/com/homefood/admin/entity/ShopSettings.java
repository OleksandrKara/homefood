package com.homefood.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Singleton row (id is always 1) - site-wide settings for the public shop, e.g. the one pickup
 * address every product shares (see V11 migration). */
@Entity
@Table(name = "shop_settings")
@Getter
@Setter
@NoArgsConstructor
public class ShopSettings {

    @Id
    private Long id = 1L;

    @Size(max = 500, message = "Слишком длинный адрес")
    @Column(name = "pickup_address")
    private String pickupAddress;
}
