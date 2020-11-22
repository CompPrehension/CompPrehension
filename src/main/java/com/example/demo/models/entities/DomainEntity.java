package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Domain", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version"}))
public class DomainEntity {
    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "version")
    private String version;
}
