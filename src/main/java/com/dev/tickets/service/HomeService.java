package com.dev.tickets.service;

import com.dev.tickets.dto.HomeResponse;
import com.dev.tickets.dto.category.CategoryResponse;
import com.dev.tickets.dto.event.EventCardResponse;
import com.dev.tickets.mapper.CategoryMapper;
import com.dev.tickets.model.CategoryEntity;
import com.dev.tickets.model.EventEntity;
import com.dev.tickets.model.EventStatusEnum;
import com.dev.tickets.repository.CategoryRepository;
import com.dev.tickets.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    public HomeResponse getHomeData(){
        ZoneId zone = ZoneId.of("America/Lima");
        PageRequest pageRequest = PageRequest.of(0, 6);
        List<CategoryEntity> categories = categoryRepository.findAll();
        List<EventCardResponse> events = eventRepository
                .findHeroEvents(EventStatusEnum.PUBLISHED, LocalDateTime.now(zone), pageRequest);

        Map<String, List<EventCardResponse>> eventsByCategory = new HashMap<>();

        categories
                .forEach( category -> {
                    List<EventCardResponse> eventsCategory = eventRepository.findByCategoryName(EventStatusEnum.PUBLISHED, category.getName(), LocalDateTime.now(zone), PageRequest.of(0, 4));
                    eventsByCategory.put(category.getName(),
                            eventsCategory);
                });

        return HomeResponse.builder()
                .heroEvents(events)
                .categories( categories.stream().map( CategoryMapper::toResponse ).toList() )
                .eventsByCategory(eventsByCategory)
                .build();
    }

}
