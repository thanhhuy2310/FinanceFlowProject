package com.financeflow.entity;
import com.financeflow.enums.ImportBatchStatus;
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
@Table(name="import_batches")
public class ImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private  User user;
    @Column(name="file_name",nullable = false,length = 255)
    private String fileName;
    @Column(name="imported_at",nullable = false)
    private LocalDateTime importedAt;
    @Column(name="total_rows",nullable = false)
    private Integer totalRows;
    @Column(name="success_rows",nullable = false)
    private Integer successRows;
    @Column(name="failed_rows",nullable = false)
    private Integer failedRows;
    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false,length = 100)
    private ImportBatchStatus status;
    @Column(name="error_message")
    private String errorMessage;
    @OneToMany(mappedBy = "importBatch")
    private List<Transaction> transactions;

}
