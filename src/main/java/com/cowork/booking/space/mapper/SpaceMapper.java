package com.cowork.booking.space.mapper;

import com.cowork.booking.space.dto.SpaceRequest;
import com.cowork.booking.space.dto.SpaceResponse;
import com.cowork.booking.space.model.Space;
import org.springframework.stereotype.Component;

@Component
public class SpaceMapper {

    public Space toEntity(SpaceRequest request) {
        return new Space(request.name().trim(), request.type(), request.capacity(),
                request.location().trim(), request.hourlyRate());
    }

    public void updateEntity(Space space, SpaceRequest request) {
        space.update(request.name().trim(), request.type(), request.capacity(),
                request.location().trim(), request.hourlyRate());
    }

    public SpaceResponse toResponse(Space space) {
        return new SpaceResponse(space.getId(), space.getName(), space.getType(), space.getCapacity(),
                space.getLocation(), space.getHourlyRate(), space.getCreatedAt(), space.getUpdatedAt());
    }
}
