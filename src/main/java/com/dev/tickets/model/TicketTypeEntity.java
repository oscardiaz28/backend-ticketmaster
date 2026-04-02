package com.dev.tickets.model;

import com.dev.tickets.exception.AppException;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "ticket_types")
public class TicketTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private BigDecimal price;
    private Integer totalAvailable;
    private Integer reserved;
    private Integer sold;
    @JsonIgnore
    @OneToMany(mappedBy = "ticketType", cascade = CascadeType.ALL)
    private List<TicketEntity> tickets = new ArrayList<>();
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "event_id")
    private EventEntity event;
    private boolean isDeleted;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist(){
        ZoneId zone = ZoneId.of("America/Lima");
        this.createdAt = LocalDateTime.now(zone);
        this.reserved = 0;
        this.sold = 0;
        this.isDeleted = false;
    }

    @Transient
    public Integer getAvailable(){
        return this.getTotalAvailable() - this.getReserved() - this.getSold();
    }

    public void reserve(int quantity){
        if(quantity < 0){
            throw new AppException("Cantidad inválida");
        }
        if( quantity > totalAvailable ){
            throw new AppException("No hay stock suficiente");
        }
        this.totalAvailable -= quantity;
    }

}
