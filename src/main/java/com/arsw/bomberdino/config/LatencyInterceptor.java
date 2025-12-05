package com.arsw.bomberdino.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor para medir latencia de requests en tiempo real y calcular
 * percentiles. Imprime estadísticas cada 100 requests.
 *
 * @author BomberDino Team
 */
@Slf4j
@Component
public class LatencyInterceptor implements HandlerInterceptor {

    // Cola thread-safe para almacenar latencias
    private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
    private static final int MAX_SAMPLES = 1000; // Mantener últimas 1000 muestras
    private int requestCount = 0;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Guardar timestamp de inicio en el request
        request.setAttribute("startTime", System.currentTimeMillis());
        log.info("🔵 Request recibido: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        // Calcular latencia
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long latency = System.currentTimeMillis() - startTime;

            // Agregar a la cola
            latencies.offer(latency);

            // Limitar tamaño de la cola
            if (latencies.size() > MAX_SAMPLES) {
                latencies.poll();
            }

            requestCount++;

            log.info("🟢 Request #{} completado: {} {} - {}ms",
                    requestCount,
                    request.getMethod(),
                    request.getRequestURI(),
                    latency
            );

            // Imprimir estadísticas cada 10 requests (cambiar de 100 a 10 para testing)
            if (requestCount % 10 == 0) {
                log.info("📊 IMPRIMIENDO ESTADÍSTICAS (cada 10 requests)...");
                printStatistics();
            }
        }
    }

    /**
     * Calcula e imprime estadísticas de latencia
     */
    private void printStatistics() {
        if (latencies.isEmpty()) {
            return;
        }

        // Convertir a lista y ordenar
        List<Long> sortedLatencies = new ArrayList<>(latencies);
        sortedLatencies.sort(Long::compareTo);

        int size = sortedLatencies.size();
        long min = sortedLatencies.get(0);
        long max = sortedLatencies.get(size - 1);
        long avg = (long) sortedLatencies.stream().mapToLong(Long::longValue).average().orElse(0);

        // Calcular percentiles
        long p50 = getPercentile(sortedLatencies, 0.50);
        long p90 = getPercentile(sortedLatencies, 0.90);
        long p95 = getPercentile(sortedLatencies, 0.95);
        long p99 = getPercentile(sortedLatencies, 0.99);

        // Imprimir estadísticas sin caracteres especiales
        log.info("\n"
                + "================================================================\n"
                + "      ESTADISTICAS DE LATENCIA (Ultimos {} requests)          \n"
                + "================================================================\n"
                + "  Muestras totales:  {}\n"
                + "  Min:               {} ms\n"
                + "  Max:               {} ms\n"
                + "  Promedio:          {} ms\n"
                + "  ------------------------------------------------------------ \n"
                + "  p50 (Mediana):     {} ms\n"
                + "  p90:               {} ms\n"
                + "  >> p95:            {} ms  (objetivo: < 50ms)\n"
                + "  >> p99:            {} ms  (objetivo: < 150ms)\n"
                + "================================================================\n",
                size, size, min, max, avg, p50, p90, p95, p99
        );

        // Verificar cumplimiento de objetivos
        boolean meetsP95 = p95 < 50;
        boolean meetsP99 = p99 < 150;

        if (meetsP95 && meetsP99) {
            log.info("SUCCESS! Objetivos cumplidos: p95={} ms, p99={} ms", p95, p99);
        } else {
            log.warn("WARNING! Objetivos no cumplidos: p95={} ms {}, p99={} ms {}",
                    p95, meetsP95 ? "OK" : "FAIL",
                    p99, meetsP99 ? "OK" : "FAIL"
            );
        }
    }

    /**
     * Calcula el percentil de una lista ordenada
     */
    private long getPercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    /**
     * Método público para obtener estadísticas bajo demanda
     */
    public void printCurrentStatistics() {
        printStatistics();
    }
}
