package com.dev.tickets.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "purchases")
public class PurchaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private BigDecimal total;
    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private UserEntity buyer;
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL)
    private List<TicketEntity> tickets = new ArrayList<>();

    @PrePersist
    void prePersist(){
        this.createdAt = LocalDateTime.now(ZoneId.of("America/Lima"));
    }

    public void addTickets(TicketTypeEntity type, int quantity ){
        type.reserve(quantity);
        for( int i = 0; i < quantity; i++ ){
            tickets.add( TicketEntity.create(type, this) );
        }
        recalculateTotal();
    }
    private void recalculateTotal(){
        this.total = tickets.stream()
                .map(t -> t.getTicketType().getPrice())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }
}
