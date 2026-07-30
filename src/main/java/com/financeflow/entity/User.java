
package com.financeflow.entity;
import com.financeflow.enums.UserRole;
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
@Table(name="users")
public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @Column(name = "full_name", nullable = false, length = 100)
        private String fullName;
        @Column(name = "email", nullable = false, length = 255, unique = true)
        private String email;
        @Column(name = "password", nullable = false, length = 255)
        private String password;
        @Enumerated(EnumType.STRING)
        @Column(name = "role", nullable = false, length = 20)
        private UserRole role;
        @Column(name = "status", nullable = false)
        private Boolean status;
        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
        @OneToMany(mappedBy = "user")
        private List<Account> accounts;
        @OneToMany(mappedBy = "user")
        private List<Category> categories;
        @OneToMany(mappedBy = "user")
        private List<ImportBatch> importBatches;
        @OneToMany(mappedBy = "user")
        private List<Rule>rules;
}
