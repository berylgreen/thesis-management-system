package com.thesis.controller;

import com.thesis.entity.Review;
import com.thesis.service.ReviewService;
import com.thesis.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/create")
    public Result<Review> createReview(
            @RequestParam Long versionId,
            @RequestParam String comment,
            @RequestParam(required = false) BigDecimal score,
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            Authentication auth) {
        try {
            Long teacherId = (Long) auth.getPrincipal();
            Review review = reviewService.createReview(versionId, teacherId, comment, score, status);
            return Result.success("批改成功", review);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/version/{versionId}")
    public Result<List<Review>> getVersionReviews(@PathVariable Long versionId) {
        try {
            List<Review> reviews = reviewService.getVersionReviews(versionId);
            return Result.success(reviews);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<List<Review>> getMyReviews(Authentication auth) {
        try {
            Long teacherId = (Long) auth.getPrincipal();
            List<Review> reviews = reviewService.getTeacherReviews(teacherId);
            return Result.success(reviews);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
