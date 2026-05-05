package com.capstone.pethouse.domain.hospital.service;

import com.capstone.pethouse.domain.code.entity.Code;
import com.capstone.pethouse.domain.code.repository.CodeRepository;
import com.capstone.pethouse.domain.hospital.dto.request.HospitalCreateRequest;
import com.capstone.pethouse.domain.hospital.dto.request.HospitalUpdateRequest;
import com.capstone.pethouse.domain.hospital.dto.response.HospitalDetailResponse;
import com.capstone.pethouse.domain.hospital.dto.response.HospitalStatusResponse;
import com.capstone.pethouse.domain.hospital.entity.Hospital;
import com.capstone.pethouse.domain.hospital.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("HospitalService 단위 테스트")
class HospitalServiceTest {

    @InjectMocks
    private HospitalService hospitalService;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private CodeRepository codeRepository;

    @Test
    @DisplayName("병원을 상세 조회한다 - 성공")
    void getHospital_Success() {
        // given
        Long seq = 1L;
        Code mainCode = createCode("MED_01", "내과");
        Hospital hospital = createHospital(seq, "동물병원", mainCode);
        given(hospitalRepository.findById(seq)).willReturn(Optional.of(hospital));

        // when
        HospitalDetailResponse result = hospitalService.getHospital(seq);

        // then
        assertThat(result.hospital().name()).isEqualTo("동물병원");
        assertThat(result.hospital().mainMedCode()).isEqualTo("MED_01");
    }

    @Test
    @DisplayName("병원을 등록한다 - 성공")
    void createHospital_Success() {
        // given
        HospitalCreateRequest request = new HospitalCreateRequest(
                "새 병원", "서울", "02-123-4567",
                37.5, 127.0,
                "MED_01", List.of("MED_02")
        );
        Code mainCode = createCode("MED_01", "내과");
        Code subCode = createCode("MED_02", "외과");
        Hospital savedHospital = createHospital(1L, "새 병원", mainCode);

        given(codeRepository.findByCode("MED_01")).willReturn(Optional.of(mainCode));
        given(codeRepository.findByCode("MED_02")).willReturn(Optional.of(subCode));
        given(hospitalRepository.save(any(Hospital.class))).willReturn(savedHospital);

        // when
        HospitalStatusResponse result = hospitalService.createHospital(request);

        // then
        assertThat(result.status()).isEqualTo("success");
        verify(hospitalRepository).save(any(Hospital.class));
    }

    @Test
    @DisplayName("존재하지 않는 진료 코드로 등록 시 예외가 발생한다")
    void createHospital_CodeNotFound() {
        // given
        HospitalCreateRequest request = new HospitalCreateRequest(
                "병원", "서울", "02-123-4567",
                37.5, 127.0,
                "INVALID_CODE", List.of()
        );
        given(codeRepository.findByCode("INVALID_CODE")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hospitalService.createHospital(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Main medical code not found");
    }

    @Test
    @DisplayName("병원을 수정한다 - 성공")
    void updateHospital_Success() {
        // given
        Long seq = 1L;
        HospitalUpdateRequest request = new HospitalUpdateRequest(
                "수정 병원", "서울", "02-123-4567",
                37.5, 127.0,
                "MED_01", List.of()
        );
        Code mainCode = createCode("MED_01", "내과");
        Hospital existingHospital = createHospital(seq, "기존 병원", mainCode);

        given(hospitalRepository.findById(seq)).willReturn(Optional.of(existingHospital));
        given(codeRepository.findByCode("MED_01")).willReturn(Optional.of(mainCode));

        // when
        HospitalStatusResponse result = hospitalService.updateHospital(seq, request);

        // then
        assertThat(result.status()).isEqualTo("success");
        assertThat(existingHospital.getName()).isEqualTo("수정 병원");
    }

    @Test
    @DisplayName("병원을 삭제한다 - 성공")
    void deleteHospital_Success() {
        // given
        Long seq = 1L;
        Hospital hospital = createHospital(seq, "삭제용", createCode("MED_01", "내과"));
        given(hospitalRepository.findById(seq)).willReturn(Optional.of(hospital));

        // when
        hospitalService.deleteHospital(seq);

        // then
        verify(hospitalRepository).delete(hospital);
    }

    // Helper methods
    private Code createCode(String codeStr, String name) {
        return Code.of(codeStr, null, name);
    }

    private Hospital createHospital(Long seq, String name, Code mainCode) {
        Hospital hospital = Hospital.of(name, "Location", "Phone", 
                37.5, 127.0, mainCode, new java.util.ArrayList<>());
        ReflectionTestUtils.setField(hospital, "seq", seq);
        ReflectionTestUtils.setField(hospital, "createdAt", java.time.LocalDateTime.now());
        return hospital;
    }
}
