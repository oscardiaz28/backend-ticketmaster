package com.dev.tickets.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "events")
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    @Column(unique = true)
    private String slug;
    private String image;
    private String location;
    private LocalDateTime startDate;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String detalle;
    @Enumerated(EnumType.STRING)
    private EventStatusEnum status;
    @ManyToOne
    @JoinColumn(name = "category_id")
    private CategoryEntity category;
    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private UserEntity organizer;
    @ManyToOne
    @JoinColumn(name = "venue_id")
    private VenueEntity venue;
    @JsonIgnore
    @ManyToMany(mappedBy = "staffingEvents")
    private List<UserEntity> staff = new ArrayList<>();
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<TicketTypeEntity> ticketTypes = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist(){
        ZoneId zone = ZoneId.of("America/Lima");
        this.createdAt = LocalDateTime.now(zone);
    }

    @PreUpdate
    void preUpdate(){
        ZoneId zone = ZoneId.of("America/Lima");
        this.updatedAt = LocalDateTime.now(zone);
    }

}
