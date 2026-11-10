package com.all4land.seoulsatellitearchive.provider.dto;

import com.all4land.seoulsatellitearchive.provider.Provider;

public record ProviderResponse(String code, String name, boolean isPaid) {

    public static ProviderResponse from(Provider provider) {
        return new ProviderResponse(provider.getCode(), provider.getName(), provider.isPaid());
    }
}
