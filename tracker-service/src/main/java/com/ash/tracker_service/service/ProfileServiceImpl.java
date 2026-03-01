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
    private final CloudinaryService cloudinaryService;

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

    public void syncGoogleProfile(String userId, String email, String name, String googlePictureUrl) {
        System.out.println("========================================");
        System.out.println("✓ ProfileServiceImpl.syncGoogleProfile called");
        System.out.println("  User ID: " + userId);
        System.out.println("  Email: " + email);
        System.out.println("  Name: " + name);
        System.out.println("  Google Picture URL: " + googlePictureUrl);
        System.out.println("========================================");
        
        Optional<TrackerUser> optional = trackerUserRepository.findByUserId(userId);
        
        TrackerUser user;
        if (optional.isEmpty()) {
            System.out.println("✓ Creating new TrackerUser for: " + userId);
            user = TrackerUser.builder()
                    .userId(userId)
                    .email(email)
                    .name(name)
                    .createdAt(Instant.now())
                    .build();
            
            UserPortfolio portfolio = UserPortfolio.builder()
                    .userId(userId)
                    .totalInvestment(0)
                    .totalCurrentValue(0)
                    .updatedAt(Instant.now())
                    .build();
            userPortfolioRepository.save(portfolio);
            System.out.println("✓ Created UserPortfolio for: " + userId);
        } else {
            System.out.println("✓ Updating existing TrackerUser for: " + userId);
            user = optional.get();
            if (name != null && !name.isEmpty()) {
                user.setName(name);
                System.out.println("✓ Updated name to: " + name);
            }
        }


        if (googlePictureUrl != null && !googlePictureUrl.isEmpty()) {

            if (googlePictureUrl.contains("cloudinary.com")) {
                System.out.println("✓ Using Cloudinary URL from auth-service: " + googlePictureUrl);
                user.setProfilePicture(googlePictureUrl);
            } else {

                try {
                    System.out.println("✓ Uploading Google profile picture to Cloudinary...");
                    System.out.println("  Source URL: " + googlePictureUrl);
                    
                    String cloudinaryUrl = cloudinaryService.uploadImageFromUrl(
                        googlePictureUrl, 
                        "profile_images"
                    );
                    
                    user.setProfilePicture(cloudinaryUrl);
                    System.out.println("✅ Profile picture uploaded to Cloudinary: " + cloudinaryUrl);
                } catch (Exception e) {
                    System.err.println("✗ Failed to upload profile picture to Cloudinary");
                    System.err.println("✗ Error: " + e.getMessage());
                    e.printStackTrace();

                    System.out.println("⚠️  Keeping existing profile picture URL");
                }
            }
        } else {
            System.out.println("⚠️  No profile picture URL provided");
        }

        user.setUpdatedAt(Instant.now());
        trackerUserRepository.save(user);
        System.out.println("✅ TrackerUser saved successfully");
        System.out.println("  Final profile picture: " + user.getProfilePicture());
        System.out.println("  Final name: " + user.getName());
        System.out.println("========================================");
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
    
    @Override
    public String uploadProfileImage(String userId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        System.out.println("========================================");
        System.out.println("✓ Uploading profile image for user: " + userId);
        
        TrackerUser user = trackerUserRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidRequestException("User not found"));
        

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            try {
                System.out.println("✓ Deleting old profile picture: " + user.getProfilePicture());
                cloudinaryService.deleteImage(user.getProfilePicture());
                System.out.println("✅ Old profile picture deleted");
            } catch (Exception e) {
                System.err.println("⚠️  Failed to delete old profile picture: " + e.getMessage());

            }
        }
        

        System.out.println("✓ Uploading new profile picture to Cloudinary...");
        String imageUrl = cloudinaryService.uploadImage(file, "profile_images");
        System.out.println("✅ New profile picture uploaded: " + imageUrl);
        

        user.setProfilePicture(imageUrl);
        user.setUpdatedAt(Instant.now());
        trackerUserRepository.save(user);
        
        System.out.println("✅ Profile updated with new image URL");
        System.out.println("========================================");
        
        return imageUrl;
    }
}
