package com.dadumserver.env.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "environment_tag")
@Getter
@NoArgsConstructor
public class EnvironmentTag {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "environment_tag_id")
  private UUID id;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;
}
