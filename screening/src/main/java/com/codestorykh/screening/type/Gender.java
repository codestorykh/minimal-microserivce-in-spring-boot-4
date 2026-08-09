package com.codestorykh.screening.type;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other"),
    UNKNOWN("Unspecified / Prefer not to say");

    private final String label;

    Gender(String label) {
        this.label = label;
    }
}
