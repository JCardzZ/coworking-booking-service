package com.cowork.booking.space.service;

import com.cowork.booking.common.AppConstants;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.space.dto.SpaceFilter;
import com.cowork.booking.space.dto.SpaceRequest;
import com.cowork.booking.space.dto.SpaceResponse;
import com.cowork.booking.space.mapper.SpaceMapper;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.space.model.SpaceType;
import com.cowork.booking.space.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock
    private SpaceRepository spaceRepository;

    private SpaceService spaceService;

    @BeforeEach
    void setUp() {
        spaceService = new SpaceService(spaceRepository, new SpaceMapper());
    }

    @Test
    void createSavesTrimmedSpaceWhenNameIsFree() {
        SpaceRequest request = request("  Sala Andes  ");
        when(spaceRepository.existsByNameIgnoreCaseAndActiveTrue("Sala Andes")).thenReturn(false);
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpaceResponse response = spaceService.create(request);

        ArgumentCaptor<Space> saved = ArgumentCaptor.forClass(Space.class);
        verify(spaceRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Sala Andes");
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(response.name()).isEqualTo("Sala Andes");
        assertThat(response.hourlyRate()).isEqualByComparingTo("25.00");
    }

    @Test
    void createRejectsDuplicateName() {
        when(spaceRepository.existsByNameIgnoreCaseAndActiveTrue("Sala Andes")).thenReturn(true);

        assertThatThrownBy(() -> spaceService.create(request("Sala Andes")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code", "conflictingField")
                .containsExactly(AppConstants.ErrorCodes.SPACE_NAME_TAKEN, "name");
        verify(spaceRepository, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenSpaceDoesNotExistOrIsInactive() {
        when(spaceRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllMapsEachPageElement() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Space> page = new PageImpl<>(List.of(space("Sala Andes"), space("Puesto 1")), pageable, 2);
        when(spaceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<SpaceResponse> result = spaceService.findAll(new SpaceFilter(SpaceType.MEETING_ROOM, 4, null), pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(SpaceResponse::name).containsExactly("Sala Andes", "Puesto 1");
    }

    @Test
    void updateAppliesNewDataWhenNameIsFree() {
        Space existing = space("Sala Andes");
        when(spaceRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(spaceRepository.existsByNameIgnoreCaseAndActiveTrueAndIdNot("Sala Pacífico", 1L)).thenReturn(false);
        when(spaceRepository.saveAndFlush(existing)).thenReturn(existing);

        SpaceRequest request = new SpaceRequest("Sala Pacífico", SpaceType.PRIVATE_OFFICE, 6, "Piso 3", new BigDecimal("40.00"));
        SpaceResponse response = spaceService.update(1L, request);

        assertThat(response.name()).isEqualTo("Sala Pacífico");
        assertThat(response.type()).isEqualTo(SpaceType.PRIVATE_OFFICE);
        assertThat(response.capacity()).isEqualTo(6);
    }

    @Test
    void updateRejectsNameUsedByAnotherSpace() {
        Space existing = space("Sala Andes");
        when(spaceRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(spaceRepository.existsByNameIgnoreCaseAndActiveTrueAndIdNot("Puesto 1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> spaceService.update(1L, request("Puesto 1")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(existing.getName()).isEqualTo("Sala Andes");
        verify(spaceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateThrowsWhenSpaceDoesNotExist() {
        when(spaceRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.update(99L, request("Sala Andes")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDeactivatesTheSpaceInsteadOfRemovingIt() {
        Space existing = space("Sala Andes");
        when(spaceRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));

        spaceService.delete(1L);

        assertThat(existing.isActive()).isFalse();
        verify(spaceRepository, never()).delete(any(Space.class));
    }

    @Test
    void deleteThrowsWhenSpaceDoesNotExist() {
        when(spaceRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static SpaceRequest request(String name) {
        return new SpaceRequest(name, SpaceType.MEETING_ROOM, 8, "Piso 2", new BigDecimal("25.00"));
    }

    private static Space space(String name) {
        return new Space(name, SpaceType.MEETING_ROOM, 8, "Piso 2", new BigDecimal("25.00"));
    }
}
