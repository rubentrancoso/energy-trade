package com.energytrade.commonlogging;

import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Appender personalizado do Log4j2 que envia logs como payloads JSON via HTTP para um serviço externo.
 */
@Plugin(name = "HttpLogCollector", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class HttpLogCollectorAppender extends AbstractAppender {

    private final String endpointUrl;
    private final String serviceName;

    /**
     * Construtor protegido usado pelo factory method.
     */
    protected HttpLogCollectorAppender(
            String name,
            Layout<? extends Serializable> layout,
            String endpointUrl,
            String serviceName
    ) {
        super(name, null, layout, false, null);
        this.endpointUrl = endpointUrl;
        this.serviceName = serviceName != null ? serviceName : "unknown";
    }

    /**
     * Método de fábrica utilizado pelo Log4j para instanciar o appender com base no log4j2.xml.
     */
    @PluginFactory
    public static HttpLogCollectorAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("collectorUrl") String collectorUrl,
            @PluginAttribute("serviceName") String serviceName,
            @PluginElement("Layout") Layout<? extends Serializable> layout
    ) {
        if (name == null) {
            LOGGER.error("❌ Nenhum nome fornecido para HttpLogCollectorAppender");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        // Prioriza atributos explícitos; se não existirem, busca por system properties ou variáveis de ambiente
        if (collectorUrl == null || collectorUrl.isEmpty()) {
            collectorUrl = System.getProperty("log.collector.url");
            if (collectorUrl == null || collectorUrl.isEmpty()) {
                collectorUrl = System.getenv("LOG_COLLECTOR_URL");
            }
        }

        if (serviceName == null || serviceName.isEmpty()) {
            serviceName = System.getProperty("log.service.name");
            if (serviceName == null || serviceName.isEmpty()) {
                serviceName = System.getenv("LOG_SERVICE_NAME");
            }
        }

        if (collectorUrl == null || collectorUrl.isEmpty()) {
            LOGGER.warn("❌ collectorUrl não definido — HttpLogCollectorAppender desabilitado.");
            return null;
        }

        LOGGER.info("HttpLogCollectorAppender initialized. Logs to: {} | Service: {}", collectorUrl, serviceName);

        return new HttpLogCollectorAppender(name, layout, collectorUrl, serviceName);
    }

    /**
     * Método principal do Appender — envia cada evento de log como um JSON para o endpoint HTTP.
     */
    @Override
    public void append(LogEvent event) {
        try {
            // Cria conexão HTTP
            URL url = new URL(endpointUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // Escapa corretamente o conteúdo da mensagem
            String escapedMessage = event.getMessage().getFormattedMessage()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            // Monta o JSON do log
            String jsonPayload = String.format(
                    "{\"timestamp\":\"%s\",\"level\":\"%s\",\"service\":\"%s\",\"source\":\"%s\",\"message\":\"%s\"}",
                    Instant.ofEpochMilli(event.getTimeMillis()).toString(),
                    event.getLevel().toString(),
                    serviceName,
                    event.getLoggerName(),
                    escapedMessage
            );

            // Envia o payload
            try (Writer writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(jsonPayload);
            }

            conn.getResponseCode(); // Apenas dispara request
            conn.disconnect();
        } catch (Exception e) {
            LOGGER.error("❌ Falha ao enviar log para o coletor", e);
        }
    }
}
