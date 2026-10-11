package com.tripplanner.service;

import com.tripplanner.entity.ItineraryItem;
import com.tripplanner.entity.Trip;
import com.tripplanner.repository.ItineraryItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ItinerarySuggestionService {
    private static final List<List<Suggestion>> KERALA_DAYS = List.of(
            List.of(
                    new Suggestion("Fort Kochi heritage walk", "Explore the waterfront, historic lanes, and Chinese fishing-net area before the afternoon heat.", "Fort Kochi, Kochi", "08:30", "11:00", "Check current access and weather; allow time for breakfast."),
                    new Suggestion("Mattancherry Palace and Jew Town", "Visit the palace and browse the heritage streets and spice shops nearby.", "Mattancherry, Kochi", "12:00", "15:00", "Verify opening hours and weekly closing day before setting out."),
                    new Suggestion("Kathakali performance and Kerala dinner", "Watch a traditional performance, then try a local Kerala meal.", "Kochi", "17:00", "20:00", "Check show times and book ahead where needed.")
            ),
            List.of(
                    new Suggestion("Scenic drive from Kochi to Munnar", "Travel inland through the Western Ghats, pausing for the waterfalls and viewpoints along the route.", "Kochi to Munnar", "07:30", "13:00", "Road times vary; arrange transport and confirm waterfall access."),
                    new Suggestion("Tea estate viewpoint and Munnar check-in", "Settle in, then take a relaxed walk at a nearby tea estate viewpoint.", "Munnar", "15:30", "18:00", "Use permitted paths and avoid entering working tea fields without permission.")
            ),
            List.of(
                    new Suggestion("Eravikulam National Park", "Spend the morning on the park's permitted trails and viewpoints.", "Eravikulam, Munnar", "08:00", "11:30", "Check current opening, ticket, and access conditions; availability can vary."),
                    new Suggestion("Tea museum visit", "Learn how Munnar tea is grown and processed, with time for a tasting.", "Munnar", "12:30", "14:00", "Confirm opening hours before visiting."),
                    new Suggestion("Mattupetty Dam and Echo Point", "Enjoy the lake and mountain scenery on an afternoon outing.", "Mattupetty, Munnar", "15:00", "17:30", "Allow for local traffic and weather.")
            ),
            List.of(
                    new Suggestion("Drive from Munnar to Alappuzha", "Travel down from the hills to the backwaters, with a lunch stop on the way.", "Munnar to Alappuzha", "07:30", "13:00", "This is a long transfer; confirm your accommodation and transport timing."),
                    new Suggestion("Alappuzha backwater cruise", "Take an afternoon cruise through the canals and village waterways.", "Alappuzha", "14:00", "17:30", "Compare licensed operators and confirm the route and return time.")
            ),
            List.of(
                    new Suggestion("Village canoe and backwater morning", "See the narrower canals at a slower pace with a local canoe operator.", "Alappuzha", "08:00", "11:00", "Agree the route and duration with the operator in advance."),
                    new Suggestion("Marari beach or departure buffer", "Relax by the coast or keep this time free for your onward journey.", "Marari / Alappuzha", "12:00", "15:00", "Choose based on your departure time; do not cut airport or station transfers close.")
            )
    );

    private final ItineraryItemRepository itineraryItemRepository;

    public ItinerarySuggestionService(ItineraryItemRepository itineraryItemRepository) {
        this.itineraryItemRepository = itineraryItemRepository;
    }

    public void createSuggestions(Trip trip) {
        if (trip.getStartDate() == null || trip.getEndDate() == null) {
            return;
        }

        long dayCount = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        List<List<Suggestion>> schedule = isKerala(trip.getDestination()) ? KERALA_DAYS : genericDays(trip.getDestination());
        List<ItineraryItem> items = new ArrayList<>();

        for (int day = 0; day < dayCount; day++) {
            List<Suggestion> suggestions = day < schedule.size()
                    ? schedule.get(day)
                    : extraDay(trip.getDestination(), day + 1);
            LocalDate activityDate = trip.getStartDate().plusDays(day);
            for (Suggestion suggestion : suggestions) {
                items.add(new ItineraryItem(
                        trip,
                        suggestion.title(),
                        suggestion.description(),
                        suggestion.location(),
                        activityDate,
                        LocalTime.parse(suggestion.startTime()),
                        LocalTime.parse(suggestion.endTime()),
                        BigDecimal.ZERO,
                        suggestion.notes()
                ));
            }
        }
        itineraryItemRepository.saveAll(items);
    }

    private boolean isKerala(String destination) {
        return destination.toLowerCase(Locale.ROOT).contains("kerala");
    }

    private List<List<Suggestion>> genericDays(String destination) {
        return List.of(
                List.of(
                        new Suggestion("Get oriented in " + destination, "Take an easy walk through the central or historic district and note local transport options.", destination, "09:00", "11:00", "Keep this flexible for arrival and check-in."),
                        new Suggestion("Explore a signature local landmark", "Choose a well-reviewed landmark or viewpoint that is open on your visit date.", destination, "14:00", "17:00", "Check current hours, ticket requirements, and travel time.")
                ),
                List.of(
                        new Suggestion("Visit a local museum or cultural site", "Spend the morning at a museum or heritage site that best represents " + destination + ".", destination, "09:00", "12:00", "Confirm opening hours and reserve timed entry if required."),
                        new Suggestion("Neighborhood food walk", "Explore a nearby neighborhood and try a few regional dishes.", destination, "16:00", "19:00", "Check dietary needs and use current local recommendations.")
                ),
                List.of(
                        new Suggestion("Park, gardens, or waterfront", "Take a relaxed outdoor break in a well-known public green space or waterfront area.", destination, "09:00", "12:00", "Check weather and seasonal access."),
                        new Suggestion("Market and independent shops", "Browse a local market or shopping street and leave time for a relaxed meal.", destination, "14:00", "17:00", "Check market days and opening times.")
                )
        );
    }

    private List<Suggestion> extraDay(String destination, int dayNumber) {
        return List.of(
                new Suggestion("Flexible day trip from " + destination, "Choose a nearby nature, heritage, or coastal excursion that matches your interests.", destination, "08:30", "13:00", "Check distance, transport, and opening conditions before booking."),
                new Suggestion("Slow afternoon and local dinner", "Keep the afternoon unhurried, then try a regional dinner near your accommodation.", destination, "15:00", "19:00", "Day " + dayNumber + " is intentionally flexible for rest or weather changes.")
        );
    }

    private record Suggestion(String title, String description, String location, String startTime, String endTime, String notes) {}
}
