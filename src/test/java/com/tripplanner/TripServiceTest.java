package com.tripplanner;

import com.tripplanner.dto.request.TripRequest;
import com.tripplanner.entity.ItineraryItem;
import com.tripplanner.entity.User;
import com.tripplanner.repository.ItineraryItemRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.service.TripService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TripServiceTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ItineraryItemRepository itineraryItemRepository;
    @Autowired
    private TripService tripService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("Trip Owner", "trip-owner@example.com", "password-hash"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getEmail(), "password")
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTrip_generatesDateBasedKeralaItineraryAndCanLoadTrip() {
        LocalDate startDate = LocalDate.of(2026, 10, 16);
        TripRequest request = new TripRequest();
        request.setName("Kerala trip");
        request.setDestination("Kerala");
        request.setStartDate(startDate);
        request.setEndDate(startDate.plusDays(4));

        var createdTrip = tripService.createTrip(request);
        var loadedTrip = tripService.getTripById(createdTrip.getId());
        List<ItineraryItem> items = itineraryItemRepository.findByTripOrderByActivityDateAscStartTimeAsc(
                tripRepository.findById(createdTrip.getId()).orElseThrow()
        );

        assertEquals(createdTrip.getName(), loadedTrip.getName());
        assertEquals(5, items.stream().map(ItineraryItem::getActivityDate).distinct().count());
        assertEquals(startDate, items.get(0).getActivityDate());
        assertTrue(items.stream().anyMatch(item -> item.getTitle().equals("Fort Kochi heritage walk")));
        assertTrue(items.stream().anyMatch(item -> item.getTitle().equals("Village canoe and backwater morning")));
    }
}
