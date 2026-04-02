package com.dev.tickets.service;

import com.dev.tickets.dto.validation.ValidationRequest;
import com.dev.tickets.dto.validation.ValidationResponse;
import com.dev.tickets.exception.AppException;
import com.dev.tickets.mapper.ValidationMapper;
import com.dev.tickets.model.*;
import com.dev.tickets.repository.TicketRepository;
import com.dev.tickets.repository.TicketValidationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
public class ValidationService {

    private final TicketValidationRepository ticketValidationRepository;
    private final TicketRepository ticketRepository;

    public ValidationService(TicketValidationRepository ticketValidationRepository, TicketRepository ticketRepository) {
        this.ticketValidationRepository = ticketValidationRepository;
        this.ticketRepository = ticketRepository;
    }

    public TicketValidationEntity validateTicketByQrCode(ValidationRequest request){

        TicketEntity ticket = ticketRepository.findByQrValue(request.code())
                .orElseThrow( () -> new AppException("Ticket not found") ) ;

        TicketValidationEntity validation = getTicketValidationEntity(ticket, request.method());

        return ticketValidationRepository.save(validation);
    }

    public TicketValidationEntity validateTicketManually( ValidationRequest request ){
        TicketEntity ticket = ticketRepository.findByTicketNumber(request.code())
                .orElseThrow( () -> new AppException("Ticket not found") ) ;
        TicketValidationEntity validation = getTicketValidationEntity(ticket, request.method());

        return ticketValidationRepository.save(validation);
    }

    private TicketValidationEntity getTicketValidationEntity(TicketEntity ticket, TicketValidationMethodEnum method ) {
        ZoneId zone = ZoneId.of("America/Lima");
        if( ticket.getStatus() == TicketStatusEnum.USED ){
            saveValidation(ticket, TicketValidationStatusEnum.ALREADY_USED);
            throw new AppException("Ticket ya fue usado");
        }

        if( ticket.getStatus() == TicketStatusEnum.CANCELLED ){
            saveValidation(ticket, TicketValidationStatusEnum.INVALID);
            throw new AppException("Ticket cancelado");
        }

        if( ticket.getTicketType().getEvent().getStartDate().isBefore(LocalDateTime.now(zone)) ){
            saveValidation(ticket, TicketValidationStatusEnum.EXPIRED);
            throw new AppException("El evento ha expirado");
        }

        ticket.setStatus(TicketStatusEnum.USED);
        TicketEntity saved = ticketRepository.save(ticket);

        TicketValidationEntity validation = new TicketValidationEntity();
        validation.setTicket(saved);
        validation.setValidationMethod( method );
        validation.setStatus(TicketValidationStatusEnum.SUCCESS);
        return validation;
    }

    private void saveValidation(TicketEntity ticket, TicketValidationStatusEnum status) {
        TicketValidationEntity validation = new TicketValidationEntity();
        validation.setTicket(ticket);
        validation.setStatus(status);
        validation.setValidationMethod(TicketValidationMethodEnum.QR_CODE);
        ticketValidationRepository.save(validation);
    }

}
