package com.roofingcrm.service.user;

import com.roofingcrm.api.v1.user.UserPickerDto;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface UserService {

    List<UserPickerDto> searchUsers(@NonNull UUID tenantId, @NonNull UUID userId, String q, int limit);
}
