package com.capstone.pethouse.domain.iot.service;

import com.capstone.pethouse.domain.device.entity.Device;
import com.capstone.pethouse.domain.User.entity.User;
import com.capstone.pethouse.domain.device.repository.DeviceRepository;
import com.capstone.pethouse.domain.iot.dto.IotDataRequest;
import com.capstone.pethouse.domain.sensor.dto.HouseDataRequest;
import com.capstone.pethouse.domain.sensor.service.HouseDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IotDataServiceTest {

    @InjectMocks
    private IotDataService iotDataService;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private HouseDataService houseDataService;

    private Device createDevice() {
        User user = User.ofUser("user01", "pass", "Name", "010");
        Device d = Device.of("DEV001", user, "SN-001", "HOUSE");
        ReflectionTestUtils.setField(d, "seq", 1L);
        return d;
    }

    @Test
    @DisplayName("환경 데이터 등록 - SN→deviceId 매핑 후 HouseDataService 위임")
    void registerSuccess() {
        IotDataRequest req = new IotDataRequest("SN-001", 25.3, 60.0, 410.0);
        Device device = createDevice();

        given(deviceRepository.findBySerialNum("SN-001")).willReturn(Optional.of(device));

        iotDataService.registerEnvironmentData(req);

        ArgumentCaptor<HouseDataRequest> captor = ArgumentCaptor.forClass(HouseDataRequest.class);
        verify(houseDataService).create(captor.capture());

        HouseDataRequest mapped = captor.getValue();
        assertThat(mapped.deviceId()).isEqualTo("DEV001");
        assertThat(mapped.temVal()).isEqualTo(25.3);
        assertThat(mapped.humVal()).isEqualTo(60.0);
        assertThat(mapped.coVal()).isEqualTo(410.0);
    }

    @Test
    @DisplayName("등록 실패 - SN 누락")
    void registerFailNoSn() {
        IotDataRequest req = new IotDataRequest(null, 25.0, 60.0, 400.0);

        assertThatThrownBy(() -> iotDataService.registerEnvironmentData(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SN");

        verifyNoInteractions(houseDataService);
    }

    @Test
    @DisplayName("등록 실패 - 미등록 시리얼")
    void registerFailUnknownSerial() {
        IotDataRequest req = new IotDataRequest("SN-XXX", 25.0, 60.0, 400.0);
        given(deviceRepository.findBySerialNum("SN-XXX")).willReturn(Optional.empty());

        assertThatThrownBy(() -> iotDataService.registerEnvironmentData(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("등록되지 않은 시리얼");

        verifyNoInteractions(houseDataService);
    }
}
