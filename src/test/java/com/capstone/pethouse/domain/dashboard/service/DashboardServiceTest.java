package com.capstone.pethouse.domain.dashboard.service;

import com.capstone.pethouse.domain.User.entity.User;
import com.capstone.pethouse.domain.User.repository.UserRepository;
import com.capstone.pethouse.domain.dashboard.dto.DashboardRequest.DeviceCreateReq;
import com.capstone.pethouse.domain.dashboard.dto.DashboardResponse.SensorDataRes;
import com.capstone.pethouse.domain.dashboard.repository.DashboardSensorRepository;
import com.capstone.pethouse.domain.device.entity.Device;
import com.capstone.pethouse.domain.device.entity.PetHouse;
import com.capstone.pethouse.domain.device.repository.DeviceRepository;
import com.capstone.pethouse.domain.device.repository.PetHouseRepository;
import com.capstone.pethouse.domain.serial.entity.Serial;
import com.capstone.pethouse.domain.serial.repository.SerialRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService 단위 테스트")
class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SerialRepository serialRepository;

    @Mock
    private DashboardSensorRepository sensorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetHouseRepository petHouseRepository;

    @Test
    @DisplayName("최신 센서 데이터를 조회한다 - 성공")
    void getLatestSensorData_Success() {
        // given
        String deviceId = "DEV_01";
        SensorDataRes expected = new SensorDataRes(deviceId, 25.0, 50.0, 80.0, 400.0, "2026-05-05 16:00:00");
        given(sensorRepository.getLatestSensorData(deviceId)).willReturn(expected);

        // when
        SensorDataRes result = dashboardService.getLatestSensorData(deviceId);

        // then
        assertThat(result.deviceId()).isEqualTo(deviceId);
        assertThat(result.temperature()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("대시보드에서 장치를 생성하고 펫하우스에 연결한다 - 성공")
    void createDevice_Success() {
        // given
        DeviceCreateReq dto = new DeviceCreateReq(1L, "DEV_01", "user1", "SN_001", "camera");
        User user = createUser("user1");
        PetHouse petHouse = createPetHouse(1L, user);
        Serial serial = createSerial("SN_001", false);

        given(userRepository.findByMemberId("user1")).willReturn(Optional.of(user));
        given(petHouseRepository.findById(1L)).willReturn(Optional.of(petHouse));
        given(serialRepository.findBySerialNum("SN_001")).willReturn(Optional.of(serial));

        // when
        dashboardService.createDevice(dto);

        // then
        verify(deviceRepository).save(any(Device.class));
        verify(serialRepository).findBySerialNum("SN_001");
        assertThat(serial.isUse()).isTrue();
    }

    @Test
    @DisplayName("본인 소유가 아닌 펫하우스에 장치 등록 시 예외가 발생한다")
    void createDevice_NotOwner() {
        // given
        DeviceCreateReq dto = new DeviceCreateReq(1L, "DEV_01", "user1", "SN_001", "camera");
        User user1 = createUser("user1");
        User user2 = createUser("user2");
        PetHouse petHouseOfUser2 = createPetHouse(1L, user2);

        given(userRepository.findByMemberId("user1")).willReturn(Optional.of(user1));
        given(petHouseRepository.findById(1L)).willReturn(Optional.of(petHouseOfUser2));

        // when & then
        assertThatThrownBy(() -> dashboardService.createDevice(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 펫하우스에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 펫하우스 ID로 장치 등록 시 예외가 발생한다")
    void createDevice_PetHouseNotFound() {
        // given
        DeviceCreateReq dto = new DeviceCreateReq(99L, "DEV_01", "user1", "SN_001", "camera");
        User user = createUser("user1");

        given(userRepository.findByMemberId("user1")).willReturn(Optional.of(user));
        given(petHouseRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dashboardService.createDevice(dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("펫하우스를 찾을 수 없습니다.");
    }

    // Helper methods
    private User createUser(String memberId) {
        return User.ofUser(memberId, "password", "Name", "010-1234-5678");
    }

    private PetHouse createPetHouse(Long id, User user) {
        PetHouse petHouse = PetHouse.createDefault(user, "MyHouse", null, "Pet", null);
        ReflectionTestUtils.setField(petHouse, "houseId", id);
        return petHouse;
    }

    private Serial createSerial(String num, boolean inUse) {
        return Serial.of(num, inUse);
    }
}
