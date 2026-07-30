package com.financeflow.entity;
import com.financeflow.enums.CategoryType;
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
@Table(name="categories",uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_category_user_name",
                columnNames = {"user_id", "name"}
        )
})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id ;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;
    @OneToMany(mappedBy = "category")
    private List<Transaction> transactions;
    @OneToMany(mappedBy = "category")
    private List<Rule>rules;
    @Column(name="name",nullable = false,length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name="type",nullable = false,length = 20)
    private CategoryType type;
    @Column(name="icon",length = 100)
    private String icon;
    @Column(name="color",length = 20)
    private String color;
    @Column(name="is_default",nullable = false)
    private Boolean isDefault;
    @Column(name="created_at",nullable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;
}
