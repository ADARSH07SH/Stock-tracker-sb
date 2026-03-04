package com.ash.tracker_service.service;

import com.ash.tracker_service.dto.ProfileResponseDTO;
import com.ash.tracker_service.dto.ProfileUpdateRequestDTO;
import com.ash.tracker_service.entity.Account;
import com.ash.tracker_service.entity.StockHolding;
import com.ash.tracker_service.entity.TrackerUser;
import com.ash.tracker_service.entity.UserPortfolio;
import com.ash.tracker_service.exception.InvalidRequestException;
import com.ash.tracker_service.mapper.ProfileMapper;
import com.ash.tracker_service.repository.AccountRepository;
import com.ash.tracker_service.repository.TrackerUserRepository;
import com.ash.tracker_service.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final TrackerUserRepository trackerUserRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final AccountRepository accountRepository;
    private final CloudinaryService cloudinaryService;
    private final MongoTemplate mongoTemplate;

    private static final String DEMO_USER_ID = "demo26320056F31I";
    private static final String DEMO_ACCOUNT_ID = "abcdefabcdefabcdefabcdea";

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

        seedDemoPortfolio(userId);
    }

    public void syncGoogleProfile(String userId, String email, String name, String googlePictureUrl) {
        Optional<TrackerUser> optional = trackerUserRepository.findByUserId(userId);
        
        TrackerUser user;
        if (optional.isEmpty()) {
            user = TrackerUser.builder()
                    .userId(userId)
                    .email(email)
                    .name(name)
                    .createdAt(Instant.now())
                    .build();
            
            seedDemoPortfolio(userId);
        } else {
            user = optional.get();
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }
        }

        if (googlePictureUrl != null && !googlePictureUrl.isEmpty()) {
            if (googlePictureUrl.contains("cloudinary.com")) {
                user.setProfilePicture(googlePictureUrl);
            } else {
                try {
                    String cloudinaryUrl = cloudinaryService.uploadImageFromUrl(
                        googlePictureUrl, 
                        "profile_images/" + userId,
                        "profile"
                    );
                    user.setProfilePicture(cloudinaryUrl);
                } catch (Exception e) {
                    // Silently fail, keep existing profile picture
                }
            }
        }

        user.setUpdatedAt(Instant.now());
        trackerUserRepository.save(user);
    }

    private void seedDemoPortfolio(String userId) {
        try {
            Account account = Account.builder()
                    .userId(userId)
                    .accountName("My Portfolio")
                    .createdAt(Instant.now())
                    .build();
            Account savedAccount = accountRepository.save(account);

            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(DEMO_USER_ID)
                    .and("accountId").is(DEMO_ACCOUNT_ID));
            
            UserPortfolio demo = mongoTemplate.findOne(query, UserPortfolio.class, "demo-stock-collection");

            List<StockHolding> demoStocks = new ArrayList<>();
            double demoInvestment = 0;

            if (demo != null) {
                if (demo.getStocks() != null) {
                    for (StockHolding stock : demo.getStocks()) {
                        demoStocks.add(StockHolding.builder()
                                .stockName(stock.getStockName())
                                .isin(stock.getIsin())
                                .quantity(stock.getQuantity())
                                .averageBuyPrice(stock.getAverageBuyPrice())
                                .buyValue(stock.getBuyValue())
                                .lastUpdated(Instant.now())
                                .build());
                    }
                }
                demoInvestment = demo.getTotalInvestment();
            }

            UserPortfolio portfolio = UserPortfolio.builder()
                    .userId(userId)
                    .accountId(savedAccount.getId())
                    .accountName("My Portfolio")
                    .stocks(demoStocks)
                    .totalInvestment(demoInvestment)
                    .totalCurrentValue(0)
                    .isDemoData(!demoStocks.isEmpty())
                    .updatedAt(Instant.now())
                    .build();

            userPortfolioRepository.save(portfolio);
            log.info("Seeded demo portfolio for new user: {} with {} stocks", userId, demoStocks.size());
        } catch (Exception e) {
            log.error("Failed to seed demo portfolio for user: {}. Creating empty portfolio.", userId, e);
            UserPortfolio portfolio = UserPortfolio.builder()
                    .userId(userId)
                    .totalInvestment(0)
                    .totalCurrentValue(0)
                    .updatedAt(Instant.now())
                    .build();
            userPortfolioRepository.save(portfolio);
        }
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
        TrackerUser user = trackerUserRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidRequestException("User not found"));
        
        String imageUrl = cloudinaryService.uploadImage(file, "profile_images/" + userId, "profile");
        
        user.setProfilePicture(imageUrl);
        user.setUpdatedAt(Instant.now());
        trackerUserRepository.save(user);
        
        return imageUrl;
    }

    @Override
    public void savePushToken(String userId, String token) {
        TrackerUser user = trackerUserRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidRequestException("User not found"));
        
        user.setExpoPushToken(token);
        user.setUpdatedAt(Instant.now());
        trackerUserRepository.save(user);
    }
}

