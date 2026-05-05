package com.capstone.pethouse.domain.hospital.entity;

import com.capstone.pethouse.domain.code.entity.Code;
import com.capstone.pethouse.global.common.AuditingFields;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hospital extends AuditingFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_med_code_seq")
    private Code mainMedCode;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hospital_med_code_map",
            joinColumns = @JoinColumn(name = "hospital_seq"),
            inverseJoinColumns = @JoinColumn(name = "code_seq")
    )
    private List<Code> medCodes = new ArrayList<>();

    @Builder
    private Hospital(String name, String location, String phone, Double latitude, Double longitude, Code mainMedCode, List<Code> medCodes) {
        this.name = name;
        this.location = location;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mainMedCode = mainMedCode;
        if (medCodes != null) {
            this.medCodes = new ArrayList<>(medCodes);
        }
    }

    public static Hospital of(String name, String location, String phone, Double latitude, Double longitude, Code mainMedCode, List<Code> medCodes) {
        return Hospital.builder()
                .name(name)
                .location(location)
                .phone(phone)
                .latitude(latitude)
                .longitude(longitude)
                .mainMedCode(mainMedCode)
                .medCodes(medCodes)
                .build();
    }

    public void update(String name, String location, String phone, Double latitude, Double longitude, Code mainMedCode, List<Code> medCodes) {
        this.name = name;
        this.location = location;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mainMedCode = mainMedCode;
        this.medCodes.clear();
        if (medCodes != null) {
            this.medCodes.addAll(medCodes);
        }
    }
}
