package com.tripplanner.service;

import com.tripplanner.dto.request.AddMemberRequest;
import com.tripplanner.dto.response.TripMemberDto;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripMember;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.TripMemberRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripMemberService {
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final AuthService authService;

    public TripMemberService(TripRepository tripRepository, UserRepository userRepository,
                            TripMemberRepository tripMemberRepository, AuthService authService) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<TripMemberDto> getMembers(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        authService.getCurrentUserEntity();
        Trip tripForAccess = trip;
        if (!trip.getCreatedBy().getId().equals(authService.getCurrentUserEntity().getId())
                && tripMemberRepository.findByTripAndUser(trip, authService.getCurrentUserEntity()).isEmpty()) {
            throw new BadRequestException("You are not authorized to access this trip");
        }
        return tripMemberRepository.findByTrip(trip).stream().map(this::toDto).toList();
    }

    public TripMemberDto addMember(Long tripId, AddMemberRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        User currentUser = authService.getCurrentUserEntity();
        if (!trip.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Only the trip owner can add members");
        }

        User memberUser = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User with this email not found"));

        if (tripMemberRepository.existsByTripAndUser(trip, memberUser)) {
            throw new BadRequestException("User is already a member of this trip");
        }

        TripMember member = new TripMember(trip, memberUser, "MEMBER");
        return toDto(tripMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(Long tripId, Long memberId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        User currentUser = authService.getCurrentUserEntity();
        if (!trip.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Only the trip owner can remove members");
        }

        TripMember member = tripMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip member not found"));
        if (!member.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Member does not belong to this trip");
        }
        if (member.getUser().getId().equals(trip.getCreatedBy().getId())) {
            throw new BadRequestException("Owner cannot be removed from trip");
        }

        tripMemberRepository.delete(member);
    }

    private TripMemberDto toDto(TripMember member) {
        TripMemberDto dto = new TripMemberDto();
        dto.setId(member.getId());
        dto.setUserId(member.getUser().getId());
        dto.setUserName(member.getUser().getName());
        dto.setEmail(member.getUser().getEmail());
        dto.setRole(member.getRole());
        dto.setJoinedAt(member.getJoinedAt());
        return dto;
    }
}
