package com.capstone.pethouse.domain.code.service;

import com.capstone.pethouse.domain.code.dto.CodeRequest;
import com.capstone.pethouse.domain.code.dto.CodeVo;
import com.capstone.pethouse.domain.code.entity.Code;
import com.capstone.pethouse.domain.code.repository.CodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodeService 단위 테스트")
class CodeServiceTest {

    @InjectMocks
    private CodeService codeService;

    @Mock
    private CodeRepository codeRepository;

    @Test
    @DisplayName("코드를 상세 조회한다 - 성공")
    void getCode_Success() {
        // given
        Long seq = 1L;
        Code code = createCode(seq, "TEST_001", "테스트 코드", null);
        given(codeRepository.findById(seq)).willReturn(Optional.of(code));

        // when
        CodeVo result = codeService.getCode(seq);

        // then
        assertThat(result.seq()).isEqualTo(seq);
        assertThat(result.code()).isEqualTo("TEST_001");
        verify(codeRepository, times(1)).findById(seq);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 상세 조회 시 예외가 발생한다")
    void getCode_NotFound() {
        // given
        Long seq = 99L;
        given(codeRepository.findById(seq)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> codeService.getCode(seq))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("코드를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("새로운 코드를 생성한다 - 성공")
    void createCode_Success() {
        // given
        CodeRequest request = new CodeRequest("NEW_CODE", null, "새 코드");
        Code savedCode = createCode(1L, "NEW_CODE", "새 코드", null);
        
        given(codeRepository.findByCode("NEW_CODE")).willReturn(Optional.empty());
        given(codeRepository.save(any(Code.class))).willReturn(savedCode);

        // when
        CodeVo result = codeService.createCode(request);

        // then
        assertThat(result.code()).isEqualTo("NEW_CODE");
        assertThat(result.codeName()).isEqualTo("새 코드");
        verify(codeRepository).save(any(Code.class));
    }

    @Test
    @DisplayName("중복된 코드값으로 생성 시 예외가 발생한다")
    void createCode_Duplicate() {
        // given
        CodeRequest request = new CodeRequest("DUP_CODE", null, "중복 코드");
        Code existingCode = createCode(1L, "DUP_CODE", "기존 코드", null);
        given(codeRepository.findByCode("DUP_CODE")).willReturn(Optional.of(existingCode));

        // when & then
        assertThatThrownBy(() -> codeService.createCode(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 존재하는 코드입니다.");
    }

    @Test
    @DisplayName("부모 코드가 있는 자식 코드를 생성한다 - 성공")
    void createCode_WithParent() {
        // given
        Code parent = createCode(1L, "PARENT", "부모", null);
        CodeRequest request = new CodeRequest("CHILD", "PARENT", "자식");
        Code child = createCode(2L, "CHILD", "자식", parent);

        given(codeRepository.findByCode("CHILD")).willReturn(Optional.empty());
        given(codeRepository.findByCode("PARENT")).willReturn(Optional.of(parent));
        given(codeRepository.save(any(Code.class))).willReturn(child);

        // when
        CodeVo result = codeService.createCode(request);

        // then
        assertThat(result.code()).isEqualTo("CHILD");
        assertThat(result.groupCode()).isEqualTo("PARENT");
    }

    @Test
    @DisplayName("코드를 수정한다 - 성공")
    void updateCode_Success() {
        // given
        CodeRequest request = new CodeRequest("UPDATE_CODE", null, "수정 이름");
        Code existingCode = createCode(1L, "UPDATE_CODE", "기존 이름", null);
        given(codeRepository.findByCode("UPDATE_CODE")).willReturn(Optional.of(existingCode));

        // when
        CodeVo result = codeService.updateCode(request);

        // then
        assertThat(result.codeName()).isEqualTo("수정 이름");
        verify(codeRepository).findByCode("UPDATE_CODE");
    }

    @Test
    @DisplayName("코드를 삭제한다 - 성공")
    void deleteCode_Success() {
        // given
        Long seq = 1L;
        Code code = createCode(seq, "DEL", "삭제용", null);
        given(codeRepository.findById(seq)).willReturn(Optional.of(code));
        doNothing().when(codeRepository).delete(code);

        // when
        codeService.deleteCode(seq);

        // then
        verify(codeRepository, times(1)).delete(code);
    }

    @Test
    @DisplayName("코드를 목록 조회한다 (그룹 코드 필터링)")
    void getCodes_WithGroupCode() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String groupCode = "GRP";
        Code parent = createCode(1L, groupCode, "그룹", null);
        Code child = createCode(2L, "CHILD", "자식", parent);
        
        given(codeRepository.findByCode(groupCode)).willReturn(Optional.of(parent));
        given(codeRepository.findByParent(parent, pageable)).willReturn(new PageImpl<>(List.of(child)));

        // when
        Page<CodeVo> result = codeService.getCodes(pageable, groupCode);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("CHILD");
    }

    // Helper method to create Code entity for testing
    private Code createCode(Long seq, String codeStr, String name, Code parent) {
        Code code = Code.of(codeStr, parent, name);
        ReflectionTestUtils.setField(code, "seq", seq);
        ReflectionTestUtils.setField(code, "regDate", java.time.LocalDateTime.now());
        return code;
    }
}
