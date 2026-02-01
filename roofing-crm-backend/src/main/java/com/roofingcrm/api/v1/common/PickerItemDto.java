package com.roofingcrm.api.v1.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PickerItemDto {
    private UUID id;
    private String label;
    private String subLabel;
}
