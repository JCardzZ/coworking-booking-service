package com.cowork.booking.space.service;

import com.cowork.booking.common.AppConstants.Audit;
import com.cowork.booking.common.AppConstants.ErrorCodes;
import com.cowork.booking.common.AppConstants.Messages;
import com.cowork.booking.common.Audited;
import com.cowork.booking.common.BusinessRuleException;
import com.cowork.booking.common.ResourceNotFoundException;
import com.cowork.booking.space.dto.SpaceFilter;
import com.cowork.booking.space.dto.SpaceRequest;
import com.cowork.booking.space.dto.SpaceResponse;
import com.cowork.booking.space.mapper.SpaceMapper;
import com.cowork.booking.space.model.Space;
import com.cowork.booking.space.repository.SpaceRepository;
import com.cowork.booking.space.repository.SpaceSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMapper spaceMapper;

    public Page<SpaceResponse> findAll(SpaceFilter filter, Pageable pageable) {
        return spaceRepository.findAll(SpaceSpecifications.matching(filter), pageable)
                .map(spaceMapper::toResponse);
    }

    public SpaceResponse findById(Long id) {
        return spaceMapper.toResponse(getActiveSpace(id));
    }

    @Transactional
    @Audited(Audit.SPACE_CREATE)
    public SpaceResponse create(SpaceRequest request) {
        if (spaceRepository.existsByNameIgnoreCaseAndActiveTrue(request.name().trim())) {
            throw nameTaken(request.name());
        }
        Space saved = spaceRepository.save(spaceMapper.toEntity(request));
        return spaceMapper.toResponse(saved);
    }

    @Transactional
    @Audited(Audit.SPACE_UPDATE)
    public SpaceResponse update(Long id, SpaceRequest request) {
        Space space = getActiveSpace(id);
        if (spaceRepository.existsByNameIgnoreCaseAndActiveTrueAndIdNot(request.name().trim(), id)) {
            throw nameTaken(request.name());
        }
        spaceMapper.updateEntity(space, request);
        // flush: fresh updatedAt and version conflicts surface here
        return spaceMapper.toResponse(spaceRepository.saveAndFlush(space));
    }

    @Transactional
    @Audited(Audit.SPACE_DELETE)
    public void delete(Long id) {
        getActiveSpace(id).deactivate();
    }

    private Space getActiveSpace(Long id) {
        return spaceRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.Space.RESOURCE_TYPE, id, Messages.Space.NOT_FOUND.formatted(id)));
    }

    private static BusinessRuleException nameTaken(String name) {
        return new BusinessRuleException(ErrorCodes.SPACE_NAME_TAKEN, Messages.Space.NAME_TAKEN.formatted(name.trim()),
                Messages.Space.NAME_FIELD);
    }
}
