package com.ash.auth_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        System.out.println("✓ Cloudinary initialized: " + cloudName);
    }

    
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "transformation", ObjectUtils.asMap(
                                    "quality", "auto",
                                    "fetch_format", "auto"
                            )
                    ));

            String imageUrl = (String) uploadResult.get("secure_url");
            System.out.println("✓ Image uploaded to Cloudinary: " + imageUrl);
            return imageUrl;

        } catch (IOException e) {
            System.err.println("✗ Failed to upload image to Cloudinary: " + e.getMessage());
            throw new IOException("Failed to upload image: " + e.getMessage());
        }
    }

    
    public String uploadImageFromUrl(String imageUrl, String folder) throws IOException {
        try {
            Map uploadResult = cloudinary.uploader().upload(imageUrl,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "transformation", ObjectUtils.asMap(
                                    "quality", "auto",
                                    "fetch_format", "auto",
                                    "width", 400,
                                    "height", 400,
                                    "crop", "fill"
                            )
                    ));

            String cloudinaryUrl = (String) uploadResult.get("secure_url");
            System.out.println("✓ Image uploaded from URL to Cloudinary: " + cloudinaryUrl);
            return cloudinaryUrl;

        } catch (IOException e) {
            System.err.println("✗ Failed to upload image from URL: " + e.getMessage());

            return imageUrl;
        }
    }

    
    public void deleteImage(String imageUrl) {
        try {

            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("✓ Image deleted from Cloudinary: " + publicId);
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to delete image from Cloudinary: " + e.getMessage());
        }
    }

    
    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }

        try {

            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String pathAfterUpload = parts[1];

                String withoutVersion = pathAfterUpload.replaceFirst("v\\d+/", "");

                return withoutVersion.substring(0, withoutVersion.lastIndexOf('.'));
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to extract public_id: " + e.getMessage());
        }

        return null;
    }
}
