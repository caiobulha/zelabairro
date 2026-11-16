package br.com.zelabairro;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PedidoService {

    static final double CONFIANCA_MINIMA = 0.7;
    static final double RAIO_DUPLICADO_METROS = 50;
    static final long TAMANHO_MAXIMO_FOTO = 10L * 1024 * 1024;
    static final Set<String> TIPOS_FOTO = Set.of("image/jpeg", "image/png");

    // shortcut: pedidos e fotos em memória (somem ao reiniciar); em produção, RDS PostgreSQL + PostGIS e Amazon S3.
    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final Map<String, byte[]> fotos = new ConcurrentHashMap<>();
    private final ClassificadorIA classificador;

    public PedidoService(ClassificadorIA classificador) {
        this.classificador = classificador;
    }

    public record Resultado(Pedido pedido, boolean duplicado) {}

    /**
     * shortcut: triagem síncrona dentro da requisição; em produção o pedido vai para a fila SQS
     * e a Lambda faz a triagem, para o cidadão receber o protocolo sem esperar a IA.
     */
    public synchronized Resultado abrir(String descricao, Double latitude, Double longitude, MultipartFile foto)
            throws IOException {
        if (foto == null || foto.isEmpty() || !TIPOS_FOTO.contains(foto.getContentType())
                || foto.getSize() > TAMANHO_MAXIMO_FOTO)
            throw new IllegalArgumentException("Envie uma foto JPG ou PNG de até 10 MB.");
        if (latitude == null || longitude == null || Math.abs(latitude) > 90 || Math.abs(longitude) > 180)
            throw new IllegalArgumentException("Informe o local do problema.");
        if (descricao == null || descricao.isBlank())
            throw new IllegalArgumentException("Descreva o problema em poucas palavras.");

        Classificacao c = classificador.classificar(descricao, foto.getBytes());
        boolean confiavel = c != null && c.valida() && c.confianca() >= CONFIANCA_MINIMA;

        if (confiavel) {
            Optional<Pedido> existente = buscarDuplicado(c.categoria(), latitude, longitude);
            if (existente.isPresent()) {
                existente.get().apoiar();
                return new Resultado(existente.get(), true);
            }
        }

        Pedido.Status status = confiavel ? Pedido.Status.ENCAMINHADO : Pedido.Status.TRIAGEM_MANUAL;
        Classificacao registrada = c != null && c.valida() ? c : null; // resposta fora do formato não é guardada
        Pedido pedido = new Pedido(UUID.randomUUID().toString(), descricao, latitude, longitude, registrada, status);
        pedidos.put(pedido.getProtocolo(), pedido);
        fotos.put(pedido.getProtocolo(), foto.getBytes());
        return new Resultado(pedido, false);
    }

    public Optional<Pedido> buscar(String protocolo) {
        return Optional.ofNullable(pedidos.get(protocolo));
    }

    private Optional<Pedido> buscarDuplicado(String categoria, double lat, double lon) {
        return pedidos.values().stream()
                .filter(p -> p.getStatus() == Pedido.Status.ENCAMINHADO || p.getStatus() == Pedido.Status.EM_EXECUCAO)
                .filter(p -> p.getClassificacao().categoria().equals(categoria))
                .filter(p -> distanciaMetros(lat, lon, p.getLatitude(), p.getLongitude()) <= RAIO_DUPLICADO_METROS)
                .findFirst();
    }

    /** Fórmula de Haversine; em produção o PostGIS faz essa conta no banco (ST_DWithin). */
    static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6_371_000 * Math.asin(Math.sqrt(a));
    }
}
