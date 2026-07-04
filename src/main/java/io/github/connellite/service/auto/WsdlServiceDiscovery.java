package io.github.connellite.service.auto;

import io.github.connellite.model.DiscoveredSoapService;
import io.github.connellite.model.WsdlOperationModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WsdlServiceDiscovery {

    private final WsdlParser wsdlParser;

    public List<DiscoveredSoapService> discover(String servicesUrl, List<String> wsdlUrls) throws IOException {
        Map<String, DiscoveredSoapService> discoveredByWsdl = new LinkedHashMap<>();

        if (StringUtils.hasText(servicesUrl)) {
            for (DiscoveredSoapService service : discoverFromServicesPage(servicesUrl)) {
                discoveredByWsdl.putIfAbsent(service.wsdlUrl(), service);
            }
        }

        for (String wsdlUrl : wsdlUrls) {
            if (!StringUtils.hasText(wsdlUrl) || discoveredByWsdl.containsKey(wsdlUrl)) {
                continue;
            }
            DiscoveredSoapService service = discoverFromWsdl(wsdlUrl);
            discoveredByWsdl.put(wsdlUrl, service);
            log.debug("Discovered service {} from WSDL URL {}", service.name(), wsdlUrl);
        }

        if (discoveredByWsdl.isEmpty()) {
            log.info("No SOAP services discovered (services-url and wsdl-*-url are empty or missing)");
            return List.of();
        }

        log.info("Discovered {} SOAP service(s)", discoveredByWsdl.size());
        return List.copyOf(discoveredByWsdl.values());
    }

    private List<DiscoveredSoapService> discoverFromServicesPage(String servicesUrl) throws IOException {
        String baseUrl = normalizeBaseUrl(servicesUrl);
        log.info("Discovering SOAP services from {}", baseUrl);

        Document document = Jsoup.connect(baseUrl)
                .timeout(30_000)
                .get();

        Map<String, String> services = new LinkedHashMap<>();
        Elements serviceNames = document.select("span.porttypename");
        for (Element serviceNameElement : serviceNames) {
            String serviceName = serviceNameElement.text().trim();
            if (serviceName.isEmpty()) {
                continue;
            }
            Element row = serviceNameElement.closest("tr");
            if (row == null) {
                continue;
            }
            Element wsdlLink = row.selectFirst("a[href*=wsdl]");
            if (wsdlLink == null) {
                continue;
            }
            services.putIfAbsent(serviceName, wsdlLink.attr("abs:href"));
        }

        if (services.isEmpty()) {
            throw new IllegalStateException("No SOAP services found at " + baseUrl);
        }

        List<DiscoveredSoapService> discovered = new ArrayList<>();
        for (Map.Entry<String, String> entry : services.entrySet()) {
            String wsdlUrl = entry.getValue();
            List<WsdlOperationModel> operations = wsdlParser.parseOperations(wsdlUrl);
            discovered.add(new DiscoveredSoapService(entry.getKey(), wsdlUrl, operations));
            log.debug("Discovered service {} with {} operation(s)", entry.getKey(), operations.size());
        }
        return discovered;
    }

    public DiscoveredSoapService discoverFromWsdl(String wsdlUrl) {
        String serviceName = serviceNameFromWsdlUrl(wsdlUrl);
        List<WsdlOperationModel> operations = wsdlParser.parseOperations(wsdlUrl);
        log.info("Loaded {} operation(s) from WSDL {}", operations.size(), wsdlUrl);
        return new DiscoveredSoapService(serviceName, wsdlUrl, operations);
    }

    static String serviceNameFromWsdlUrl(String wsdlUrl) {
        try {
            URI uri = URI.create(wsdlUrl);
            String path = uri.getPath();
            if (path != null && !path.isBlank()) {
                String segment = path.substring(path.lastIndexOf('/') + 1);
                if (!segment.isBlank()) {
                    if (segment.endsWith(".wsdl")) {
                        segment = segment.substring(0, segment.length() - 5);
                    }
                    return segment;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // fall through to default
        }
        return "SoapService";
    }

    private String normalizeBaseUrl(String servicesUrl) {
        if (servicesUrl.endsWith("/")) {
            return servicesUrl;
        }
        return servicesUrl + "/";
    }
}
