package com.dev.tickets.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ticket_validations")
public class TicketValidationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Enumerated(EnumType.STRING)
    private TicketValidationStatusEnum status;
    @Enumerated(EnumType.STRING)
    private TicketValidationMethodEnum validationMethod;
    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private TicketEntity ticket;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        ZoneId zone = ZoneId.of("America/Lima");
        this.createdAt = LocalDateTime.now(zone);
    }

}
