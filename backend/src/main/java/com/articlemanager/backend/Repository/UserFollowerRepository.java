package com.articlemanager.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.articlemanager.backend.entity.UserFollower;

public interface UserFollowerRepository extends JpaRepository<UserFollower, Long> {

    // Check if A follows B
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Get all users that userId is following
    List<UserFollower> findByFollowerId(Long followerId);

    // Get all followers of userID
    List<UserFollower> findByFollowingId(Long followingId);

    // Delete follow relationship
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

}
