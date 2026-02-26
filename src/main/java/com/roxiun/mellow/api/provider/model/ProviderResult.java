package com.roxiun.mellow.api.provider.model;

public class ProviderResult<T> {

    private final T value;
    private final String error;

    private ProviderResult(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public static <T> ProviderResult<T> success(T value) {
        return new ProviderResult<>(value, null);
    }

    public static <T> ProviderResult<T> failure(String error) {
        return new ProviderResult<>(null, error);
    }

    public boolean isSuccess() {
        return value != null;
    }

    public T getValue() {
        return value;
    }

    public String getError() {
        return error;
    }
}
