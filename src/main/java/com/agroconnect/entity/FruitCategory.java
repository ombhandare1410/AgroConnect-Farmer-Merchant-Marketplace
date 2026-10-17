package com.agroconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fruit_categories")
@Getter @Setter
public class FruitCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80) private String name;      // English
    @Column(name = "name_mr", length = 80) private String nameMr;                    // Marathi
    @Column(name = "name_hi", length = 80) private String nameHi;                    // Hindi
    @Column(name = "image_url") private String imageUrl;
    @Column(nullable = false) private boolean active = true;
}
