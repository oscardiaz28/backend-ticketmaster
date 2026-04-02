package com.dev.tickets.service;

import com.dev.tickets.dto.category.CategoryWithEventsResponse;
import com.dev.tickets.dto.event.EventCardResponse;
import com.dev.tickets.dto.event.EventRequest;
import com.dev.tickets.dto.event.TicketAction;
import com.dev.tickets.dto.event.TicketTypeRequest;
import com.dev.tickets.exception.AppException;
import com.dev.tickets.exception.EventExpiredException;
import com.dev.tickets.libs.Utils;
import com.dev.tickets.mapper.EventMapper;
import com.dev.tickets.model.*;
import com.dev.tickets.repository.CategoryRepository;
import com.dev.tickets.repository.EventRepository;
import com.dev.tickets.repository.TicketTypeRepository;
import jdk.jfr.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public EventService(EventRepository eventRepository, UserService userService, CategoryRepository categoryRepository, TicketTypeRepository ticketTypeRepository) {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public EventEntity createEvent(EventRequest request){
        String slug = Utils.slugify(request.getName());
        UserEntity loggedUser = userService.getUserLogged();

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow( () -> new AppException("Category not found") );

        eventRepository.findBySlug(slug).ifPresent(e -> {
            throw new AppException("Se ha registrado un evento con el mismo nombre");
        });

        EventEntity event = EventMapper.toEntity(request, category);

        if( request.getTickets() != null && !request.getTickets().isEmpty() ){
            Map<Integer, TicketTypeEntity> existing =
                    event.getTicketTypes()
                            .stream()
                            .collect(Collectors.toMap( TicketTypeEntity::getId, t -> t));

            for(TicketTypeRequest req : request.getTickets() ){
                if( req.getAction() != null ){
                    switch( req.getAction() ){
                        case CREATE -> {
                            TicketTypeEntity ticket = new TicketTypeEntity();
                            ticket.setName(req.getName());
                            ticket.setPrice( req.getPrice() );
                            ticket.setTotalAvailable(req.getTotalAvailable());
                            ticket.setEvent(event);
                            event.getTicketTypes().add(ticket);
                        }
                        case UPDATE -> {
                            TicketTypeEntity ticket = existing.get(req.getId());
                            ticket.setName(req.getName());
                            ticket.setPrice( req.getPrice() );
                            ticket.setTotalAvailable(req.getTotalAvailable());
                            ticketTypeRepository.save(ticket);
                        }
                        case DELETE -> {
                            TicketTypeEntity ticket = existing.get(req.getId());
                            ticket.setDeleted(true);
                            ticketTypeRepository.save(ticket);
                        }
                        default -> throw new AppException("Unknown ticket action");
                    }
                }
            }
        }
        event.setOrganizer(loggedUser);
        return eventRepository.save(event);
    }

    public EventEntity updateEvent(Integer id, EventRequest request) {
        UserEntity loggedUser = userService.getUserLogged();

        EventEntity event = this.eventRepository.findById(id)
                .orElseThrow( () -> new AppException("Event not found") );

        if( !event.getOrganizer().getId().equals(loggedUser.getId()) ){
            throw new AppException("You dont have permission to this action");
        }

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow( () -> new AppException("Category not found") );

        if( !event.getName().equals(request.getName()) ){
            String newSlug = Utils.slugify(request.getName());
            eventRepository.findBySlug(newSlug)
                .filter(e -> !e.getId().equals(event.getId()))
                .ifPresent(e -> {
                throw new AppException("Ya existe un evento con ese nombre");
            });
        }
        EventMapper.updateEntity(event, request, category);

        if( request.getTickets() != null && !request.getTickets().isEmpty() ){
            Map<Integer, TicketTypeEntity> existing =
                    event.getTicketTypes()
                            .stream()
                            .collect(Collectors.toMap( TicketTypeEntity::getId, t -> t));

            for(TicketTypeRequest req : request.getTickets() ){
                if( req.getAction() != null ){
                    switch( req.getAction() ){
                        case CREATE -> {
                            TicketTypeEntity ticket = new TicketTypeEntity();
                            ticket.setName(req.getName());
                            ticket.setPrice(req.getPrice());
                            ticket.setTotalAvailable(req.getTotalAvailable());
                            ticket.setEvent(event);
                            event.getTicketTypes().add(ticket);
                        }
                        case UPDATE -> {
                            TicketTypeEntity ticket = existing.get(req.getId());
                            ticket.setName(req.getName());
                            ticket.setPrice(req.getPrice());
                            ticket.setTotalAvailable(req.getTotalAvailable());
                            ticketTypeRepository.save(ticket);
                        }
                        case DELETE -> {
                            TicketTypeEntity ticket = existing.get(req.getId());
                            ticket.setDeleted(true);
                            ticketTypeRepository.save(ticket);
                        }
                        default -> throw new AppException("Unknown ticket action");
                    }
                }
            }
        }

        return eventRepository.save(event);
    }

    public List<EventEntity> getEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventEntity> pagination = eventRepository.findAll(pageable);
        return pagination.getContent();
    }

    public EventEntity getEventBySlug(String slug){
        return eventRepository.findBySlug(slug)
                .orElseThrow( () -> new AppException("Evento no encontrado") );
    }

    public EventEntity getEventById(Integer id){
         UserEntity loggedUser = userService.getUserLogged();
        EventEntity event = eventRepository.findByIdWithActiveTickets(id)
                .orElseThrow( () -> new AppException("Evento no encontrado")) ;
        if( !event.getOrganizer().getId().equals(loggedUser.getId()) ){
            throw new AppException("No tienes permisos para realizar esta acción");
        }
        return event;
    }

    public List<EventEntity> listEventsForOrganizer() {
        UserEntity loggedUer = userService.getUserLogged();
        return eventRepository.findByOrganizer(loggedUer);
    }

    public List<EventEntity> listPublishedEvents(int page, int size, String category){
        return eventRepository.findByStatus(EventStatusEnum.PUBLISHED);
    }

    public CategoryWithEventsResponse eventsByCategory(int page, int size, String categoryName){
        CategoryEntity category = categoryRepository.findByName(categoryName)
                .orElseThrow( () -> new AppException("Category not found") );
        ZoneId zone = ZoneId.of("America/Lima");
        List<EventCardResponse> events = eventRepository.findByCategoryName(
                EventStatusEnum.PUBLISHED,
                categoryName,
                LocalDateTime.now(zone),
                Pageable.unpaged()
        );

        return CategoryWithEventsResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .image(category.getImage())
                .events(events)
                .build();
    }

    public List<EventCardResponse> searchPublishedEvents(String query){
        ZoneId zone = ZoneId.of("America/Lima");
        return eventRepository.searchPublishedEvents(EventStatusEnum.PUBLISHED, LocalDateTime.now(zone), query);
    }

    public EventEntity getPublishedEvent(String slug){
        EventEntity event = eventRepository.findByStatusAndSlug(EventStatusEnum.PUBLISHED, slug)
                .orElseThrow( () -> new AppException("EVENT_NOT_FOUND", "Event not found") );
        ZoneId zone = ZoneId.of("America/Lima");
        if( event.getStartDate().isBefore(LocalDateTime.now(zone))  ){
            throw new EventExpiredException();
        }
        return event;
    }

    @Scheduled(cron = "0 0 */12 * * *")
    public void expiredEvents(){
        ZoneId zone = ZoneId.of("America/Lima");
        LocalDateTime now = LocalDateTime.now(zone);
        List<EventEntity> events = eventRepository.findExpiredEventsAndActive(now, EventStatusEnum.EXPIRED);
        for( EventEntity e : events ){
            System.out.println("Evento expirado: " + e.getName());
            e.setStatus(EventStatusEnum.EXPIRED);
        }
        eventRepository.saveAll(events);
        System.out.println("Eventos actualizados: " + events.size());
    }

}
