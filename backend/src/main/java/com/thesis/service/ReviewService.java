package com.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thesis.entity.Review;
import com.thesis.entity.Thesis;
import com.thesis.mapper.ReviewMapper;
import com.thesis.mapper.ThesisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ThesisMapper thesisMapper;

    @Transactional
    public Review createReview(Long versionId, Long teacherId, String comment, BigDecimal score, String status) {
        Review review = new Review();
        review.setVersionId(versionId);
        review.setTeacherId(teacherId);
        review.setComment(comment);
        review.setScore(score);
        review.setStatus(status);
        reviewMapper.insert(review);
        return review;
    }

    public List<Review> getVersionReviews(Long versionId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getVersionId, versionId);
        return reviewMapper.selectList(wrapper);
    }

    public List<Review> getTeacherReviews(Long teacherId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTeacherId, teacherId).orderByDesc(Review::getCreatedAt);
        return reviewMapper.selectList(wrapper);
    }
}
