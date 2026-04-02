package com.dev.tickets.service;

import com.dev.tickets.dto.purchase.CreateReservationRequest;
import com.dev.tickets.dto.purchase.ReservationTicketRequest;
import com.dev.tickets.exception.AppException;
import com.dev.tickets.model.*;
import com.dev.tickets.repository.EventRepository;
import com.dev.tickets.repository.ReservationItemRepository;
import com.dev.tickets.repository.ReservationRepository;
import com.dev.tickets.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final UserService userService;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;

    @Transactional
    public ReservationEntity createReservation(CreateReservationRequest request){

        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow( () -> new AppException("Event not found") );

        UserEntity loggedUser = userService.getUserLogged();
        ReservationEntity reservation = new ReservationEntity();
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setUser(loggedUser);
        reservation.setEvent(event);
        ZoneId zone = ZoneId.of("America/Lima");
        reservation.setExpiresAt(LocalDateTime.now(zone).plusMinutes(15));

        List<ReservationItem> items = new ArrayList<>();
        for(ReservationTicketRequest ticket : request.getTickets()){
            int updated = ticketTypeRepository
                    .reserveIfAvailable(ticket.getTicketTypeId(), ticket.getQuantity());
            if (updated == 0) {
                throw new AppException("No hay suficientes tickets disponibles");
            }
            TicketTypeEntity ticketType = ticketTypeRepository
                    .findById(ticket.getTicketTypeId())
                    .orElseThrow( () -> new AppException("Ticket Type not found"));
            //ticketTypeRepository.save(ticketType);
            ReservationItem item = new ReservationItem();
            item.setReservation(reservation);
            item.setTicketType(ticketType);
            item.setQuantity(ticket.getQuantity());
            items.add(item);
        }
        reservation.setItems(items);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public ReservationEntity getReservation(String id){
        return reservationRepository.findById(id)
                .orElseThrow( () -> new AppException("Reservation not found") );
    }

    // TODO : cron job to delete reserves

    @Transactional
    public void deleteReservation(String id){
        ReservationEntity reservation = reservationRepository.findById(id)
                .orElseThrow( () -> new AppException("Resrvation not found") );
        if( reservation.getStatus() != ReservationStatus.ACTIVE ){
            throw new AppException("Reservation is not active");
        }
        for( ReservationItem item : reservation.getItems() ){
            // Lock
            int updated = ticketTypeRepository.releaseReserved(
                    item.getTicketType().getId(),
                    item.getQuantity()
            );
            if (updated == 0) {
                throw new AppException("Inconsistencia en reserved");
            }
        }
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);
    }

}
