package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Domain")
public class DomainEntity {
    @Id
    private String name;

    private String version;
}
