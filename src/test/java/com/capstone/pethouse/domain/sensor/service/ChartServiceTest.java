package com.capstone.pethouse.domain.sensor.service;

import com.capstone.pethouse.domain.device.entity.Device;
import com.capstone.pethouse.domain.User.entity.User;
import com.capstone.pethouse.domain.device.repository.DeviceRepository;
import com.capstone.pethouse.domain.sensor.dto.DataVo;
import com.capstone.pethouse.domain.sensor.entity.HouseData;
import com.capstone.pethouse.domain.sensor.entity.NeckData;
import com.capstone.pethouse.domain.sensor.repository.HouseDataRepository;
import com.capstone.pethouse.domain.sensor.repository.NeckDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChartServiceTest {

    @InjectMocks
    private ChartService chartService;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private HouseDataRepository houseDataRepository;

    @Mock
    private NeckDataRepository neckDataRepository;

    @Test
    @DisplayName("HOUSE 타입 - 하우스 데이터 반환")
    void chartForHouseDevice() {
        User user = User.ofUser("user01", "pass", "Name", "010");
        Device device = Device.of("DEV001", user, "SN-001", "HOUSE");
        ReflectionTestUtils.setField(device, "seq", 1L);

        HouseData h = HouseData.of("DEV001", 25.0, 60.0, 400.0);
        ReflectionTestUtils.setField(h, "seq", 1L);
        ReflectionTestUtils.setField(h, "regDate", LocalDateTime.now());

        Page<HouseData> page = new PageImpl<>(List.of(h));

        given(deviceRepository.findBySerialNum("SN-001")).willReturn(Optional.of(device));
        given(houseDataRepository.findAllWithSearch(eq("DEV001"), any(Pageable.class))).willReturn(page);

        List<DataVo> result = chartService.getChartData("SN-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deviceId()).isEqualTo("DEV001");
        assertThat(result.get(0).humVal()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("COLLAR 타입 - 목걸이 데이터 반환")
    void chartForCollarDevice() {
        User user = User.ofUser("user01", "pass", "Name", "010");
        Device device = Device.of("DEV002", user, "SN-002", "COLLAR");

        NeckData n = NeckData.of("DEV002", 18.0, 64.0, 404.0);
        ReflectionTestUtils.setField(n, "seq", 1L);
        ReflectionTestUtils.setField(n, "regDate", LocalDateTime.now());

        Page<NeckData> page = new PageImpl<>(List.of(n));

        given(deviceRepository.findBySerialNum("SN-002")).willReturn(Optional.of(device));
        given(neckDataRepository.findAllWithSearch(eq("DEV002"), any(Pageable.class))).willReturn(page);

        List<DataVo> result = chartService.getChartData("SN-002");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).heartVal()).isEqualTo(64.0);
    }

    @Test
    @DisplayName("시리얼 없으면 빈 리스트")
    void chartNoSerial() {
        given(deviceRepository.findBySerialNum("SN-XXX")).willReturn(Optional.empty());

        assertThat(chartService.getChartData("SN-XXX")).isEmpty();
    }

    @Test
    @DisplayName("serialNum이 null/blank면 빈 리스트")
    void chartNullSerial() {
        assertThat(chartService.getChartData(null)).isEmpty();
        assertThat(chartService.getChartData("")).isEmpty();
    }
}
