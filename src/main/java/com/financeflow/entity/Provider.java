package com.financeflow.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="providers")
    public class Provider {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private  Long id;
        @Column(name="name",nullable = false,length = 100)
        private String name;
        @Column(name="logo_url",length = 255)
        private String logoUrl;
        @Column(name="created_at",nullable = false)
        private LocalDateTime createdAt;
        @Column(name="updated_at",nullable = false)
        private LocalDateTime updatedAt;
        @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY)
        private List<Account> accounts;

    }
