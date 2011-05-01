package com.proofpoint.experimental.http.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.proofpoint.experimental.discovery.client.DiscoveryClient;
import com.proofpoint.experimental.discovery.client.ServiceDescriptor;
import com.proofpoint.experimental.discovery.client.ServiceSelector;
import com.proofpoint.experimental.discovery.client.ServiceSelectorImpl;
import com.proofpoint.experimental.discovery.client.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;

public class HttpServiceSelector
{
    private final ServiceType type;
    private final ServiceSelector serviceSelector;

    public HttpServiceSelector(ServiceType type, DiscoveryClient client, ScheduledExecutorService executor)
    {
        Preconditions.checkNotNull(type, "type is null");
        Preconditions.checkNotNull(client, "client is null");
        Preconditions.checkNotNull(executor, "executor is null");

        this.type = type;
        this.serviceSelector = new ServiceSelectorImpl(type, client, executor);
    }

    public URI selectHttpService()
    {
        List<ServiceDescriptor> serviceDescriptors = Lists.newArrayList(serviceSelector.selectAllServices());
        Collections.shuffle(serviceDescriptors);

        for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            // favor https over http
            String https = serviceDescriptor.getProperties().get("https");
            if (https != null) {
                try {
                    return new URI(https);
                }
                catch (URISyntaxException ignored) {
                }
            }
            String http = serviceDescriptor.getProperties().get("https");
            if (http != null) {
                try {
                    return new URI(http);
                }
                catch (URISyntaxException ignored) {
                }
            }
        }
        throw new IllegalStateException(format("No %s services from pool %s available", type.value(), type.pool()));
    }
}
