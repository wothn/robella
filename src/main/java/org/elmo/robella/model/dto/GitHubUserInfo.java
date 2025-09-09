package org.elmo.robella.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GitHubUserInfo {
    private Long id;
    private String login;
    private String name;
    private String email;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("html_url")
    private String htmlUrl;
}
