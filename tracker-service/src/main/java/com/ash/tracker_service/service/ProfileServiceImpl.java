package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.ProfileResponseDTO;
import com.ash.tracker_service.dto.ProfileUpdateRequestDTO;
import com.ash.tracker_service.entity.TrackerUser;
import com.ash.tracker_service.entity.UserPortfolio;
import com.ash.tracker_service.exception.InvalidRequestException;
import com.ash.tracker_service.mapper.ProfileMapper;
import com.ash.tracker_service.repository.TrackerUserRepository;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final TrackerUserRepository trackerUserRepository;
    private final UserPortfolioRepository userPortfolioRepository;

    @Override
    public ProfileResponseDTO getProfile(String userId,String email) {
        Optional<TrackerUser>optional=trackerUserRepository.findByUserId(userId);

        if(optional.isEmpty()){
            initProfile(userId, email);
            optional = trackerUserRepository.findByUserId(userId);
        }

        return ProfileMapper.toProfileResponse(optional.get());
    }

    @Override
    public void initProfile(String userId, String email) {

        if (trackerUserRepository.existsByUserId(userId)) {
            return;
        }

        TrackerUser user = TrackerUser.builder()
                .userId(userId)
                .email(email)
                .createdAt(Instant.now())
                .build();

        trackerUserRepository.save(user);

        UserPortfolio portfolio = UserPortfolio.builder()
                .userId(userId)
                .totalInvestment(0)
                .totalCurrentValue(0)
                .updatedAt(Instant.now())
                .build();

        userPortfolioRepository.save(portfolio);
    }

    @Override
    public ProfileResponseDTO updateProfile(String userId,ProfileUpdateRequestDTO profileUpdateRequestDTO) {

        TrackerUser user=trackerUserRepository.findByUserId(userId)
                .orElseThrow(()->new InvalidRequestException("Profile NOt found"));

        if (profileUpdateRequestDTO.getName() != null) user.setName(profileUpdateRequestDTO.getName());
        if (profileUpdateRequestDTO.getPhoneNumber() != null) user.setPhoneNumber(profileUpdateRequestDTO.getPhoneNumber());
        if (profileUpdateRequestDTO.getBio() != null) user.setBio(profileUpdateRequestDTO.getBio());
        if (profileUpdateRequestDTO.getProfilePicture() != null) user.setProfilePicture(profileUpdateRequestDTO.getProfilePicture());
        if (profileUpdateRequestDTO.getDateOfBirth() != null) user.setDateOfBirth(profileUpdateRequestDTO.getDateOfBirth());
        if (profileUpdateRequestDTO.getPanMasked() != null) user.setPanMasked(profileUpdateRequestDTO.getPanMasked());

        user.setUpdatedAt(Instant.now());

        trackerUserRepository.save(user);

        return ProfileMapper.toProfileResponse(user);
    }
}
