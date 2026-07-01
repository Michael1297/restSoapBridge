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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WsdlServiceDiscovery {

    private final WsdlParser wsdlParser;

    public List<DiscoveredSoapService> discover(String servicesUrl) throws IOException {
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

        log.info("Discovered {} SOAP service(s)", discovered.size());
        return discovered;
    }

    private String normalizeBaseUrl(String servicesUrl) {
        if (servicesUrl.endsWith("/")) {
            return servicesUrl;
        }
        return servicesUrl + "/";
    }
}
