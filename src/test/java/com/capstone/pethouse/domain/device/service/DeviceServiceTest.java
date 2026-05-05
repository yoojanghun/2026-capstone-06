package com.capstone.pethouse.domain.device.service;

import com.capstone.pethouse.domain.User.entity.User;
import com.capstone.pethouse.domain.User.repository.UserRepository;
import com.capstone.pethouse.domain.device.dto.DeviceRequest;
import com.capstone.pethouse.domain.device.dto.DeviceVo;
import com.capstone.pethouse.domain.device.entity.Device;
import com.capstone.pethouse.domain.device.repository.DeviceRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceService 단위 테스트")
class DeviceServiceTest {

    @InjectMocks
    private DeviceService deviceService;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SerialRepository serialRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("장치를 등록한다 - 성공")
    void createDevice_Success() {
        // given
        DeviceRequest request = new DeviceRequest(null, "user1", "SN_001", "camera", "DEV_01", null);
        Serial serial = createSerial("SN_001", false);
        User user = createUser("user1");
        Device device = Device.of("DEV_01", user, "SN_001", "camera");
        ReflectionTestUtils.setField(device, "regDate", LocalDateTime.now());

        given(serialRepository.findBySerialNum("SN_001")).willReturn(Optional.of(serial));
        given(deviceRepository.existsByDeviceId("DEV_01")).willReturn(false);
        given(userRepository.findByMemberId("user1")).willReturn(Optional.of(user));
        given(deviceRepository.save(any(Device.class))).willReturn(device);

        // when
        DeviceVo result = deviceService.createDevice(request);

        // then
        assertThat(result.deviceId()).isEqualTo("DEV_01");
        assertThat(serial.isUse()).isTrue();
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("이미 사용 중인 시리얼 번호로 등록 시 예외가 발생한다")
    void createDevice_SerialInUse() {
        // given
        DeviceRequest request = new DeviceRequest(null, "user1", "SN_001", "camera", "DEV_01", null);
        Serial serial = createSerial("SN_001", true);
        given(serialRepository.findBySerialNum("SN_001")).willReturn(Optional.of(serial));

        // when & then
        assertThatThrownBy(() -> deviceService.createDevice(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용 중인 시리얼 번호입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 등록 시 예외가 발생한다")
    void createDevice_UserNotFound() {
        // given
        DeviceRequest request = new DeviceRequest(null, "non-existent", "SN_001", "camera", "DEV_01", null);
        Serial serial = createSerial("SN_001", false);
        given(serialRepository.findBySerialNum("SN_001")).willReturn(Optional.of(serial));
        given(deviceRepository.existsByDeviceId("DEV_01")).willReturn(false);
        given(userRepository.findByMemberId("non-existent")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deviceService.createDevice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 회원입니다.");
    }

    @Test
    @DisplayName("장치를 상세 조회한다 - 성공")
    void getDevice_Success() {
        // given
        Long seq = 1L;
        User user = createUser("user1");
        Device device = Device.of("DEV_01", user, "SN_001", "camera");
        ReflectionTestUtils.setField(device, "seq", seq);
        ReflectionTestUtils.setField(device, "regDate", LocalDateTime.now());
        given(deviceRepository.findById(seq)).willReturn(Optional.of(device));

        // when
        DeviceVo result = deviceService.getDevice(seq);

        // then
        assertThat(result.seq()).isEqualTo(seq);
        assertThat(result.deviceId()).isEqualTo("DEV_01");
    }

    @Test
    @DisplayName("장치를 삭제한다 - 성공")
    void deleteDevice_Success() {
        // given
        Long seq = 1L;
        Device device = Device.of("DEV_01", createUser("user1"), "SN_001", "camera");
        Serial serial = createSerial("SN_001", true);
        
        given(deviceRepository.findById(seq)).willReturn(Optional.of(device));
        given(serialRepository.findBySerialNum("SN_001")).willReturn(Optional.of(serial));

        // when
        deviceService.deleteDevice(seq);

        // then
        assertThat(serial.isUse()).isFalse();
        verify(deviceRepository).delete(device);
    }

    // Helper methods
    private Serial createSerial(String num, boolean inUse) {
        return Serial.of(num, inUse);
    }

    private User createUser(String memberId) {
        User user = User.ofUser(memberId, "password", "Name", "010-1234-5678");
        return user;
    }
}
