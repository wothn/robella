package org.elmo.robella.mapper;

import org.elmo.robella.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(String username);

    User findByEmail(String email);

    List<User> findByActive(@Param("active") Boolean active);

    List<User> findByRole(@Param("role") String role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    int deleteByUsername(String username);

    int deleteByEmail(String email);

    User findByGithubId(String githubId);

    boolean existsByGithubId(String githubId);

    }