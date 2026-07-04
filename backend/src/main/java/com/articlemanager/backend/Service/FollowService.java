package com.articlemanager.backend.Service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.articlemanager.backend.DTOs.Response.UserFollowerResponseDTO;
import com.articlemanager.backend.Exception.BusinessRuleViolationException;
import com.articlemanager.backend.Exception.ResourceNotFoundException;
import com.articlemanager.backend.Repository.UserFollowerRepository;
import com.articlemanager.backend.Repository.UserRepository;
import com.articlemanager.backend.entity.User;
import com.articlemanager.backend.entity.UserFollower;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final UserFollowerRepository userFollowerRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(FollowService.class);

    @Transactional
    public void followUser(Long followerId, Long followingId) {
        log.debug("follow.request_received followerId={} followingId={}", followerId, followingId);

        // Validation 1: User cannot follow themselves
        if (followerId.equals(followingId)) {
            log.warn("follow.self_follow_attempted userId={}", followerId);
            throw new BusinessRuleViolationException("A user cannot follow themselves");
        }

        // Validation 2: Both users must exist
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", followerId));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("User", followingId));

        log.debug("follow.users_exist follower={} following={}", follower.getFirstName(), following.getFirstName());

        // Validation 3: User must not already be following
        boolean alreadyFollowing = userFollowerRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
        if (alreadyFollowing) {
            log.warn("follow.duplicate_attempt followerId={} followingId={}", followerId, followingId);
            throw new BusinessRuleViolationException("You are already following this user");
        }

        // Create and save the following relationship
        UserFollower userFollower = new UserFollower();
        userFollower.setFollower(follower);
        userFollower.setFollowing(following);

        userFollowerRepository.save(userFollower);
        log.info("follow.saved followerId={} followingId={}", followerId, followingId);

        // Send notification email
        emailService.sendFollowNotification(follower, following);
        log.info("follow.notification_sent followingId={}", followingId);

    }

    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        log.debug("unfollow.request_received followerId={} followingId={}", followerId, followingId);

        // No validation needed - unfollow is idempotent (returns 200 regardless)
        userFollowerRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        log.info("unfollow.success followerId={} followingId={}", followerId, followingId);
    }

    public List<UserFollowerResponseDTO> getFollowers(Long userId) {
        log.debug("followers.fetch_request userId={}", userId);

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<UserFollower> followers = userFollowerRepository.findByFollowingId(userId);
        log.info("followers.fetched count={} userId={}", followers.size(), userId);

        return mapToResponseDTOs(followers, true); // true = get follower user info
    }

    public List<UserFollowerResponseDTO> getFollowing(Long userId) {
        log.debug("following.fetch_request userId={}", userId);

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<UserFollower> following = userFollowerRepository.findByFollowerId(userId);
        log.info("following.fetched count={} userId={}", following.size(), userId);

        return mapToResponseDTOs(following, false); // false = get following user info
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowerRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    private List<UserFollowerResponseDTO> mapToResponseDTOs(List<UserFollower> userFollowers, boolean getFollower) {
        List<UserFollowerResponseDTO> responseDTOs = new ArrayList<>();

        for (UserFollower uf : userFollowers) {
            User user = getFollower ? uf.getFollower() : uf.getFollowing();

            UserFollowerResponseDTO dto = new UserFollowerResponseDTO();
            dto.setId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setFollowedAt(uf.getFollowedAt());

            responseDTOs.add(dto);
        }

        return responseDTOs;
    }
}
