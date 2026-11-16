package br.com.zelabairro;

import java.time.Instant;

public class Pedido {

    public enum Status { ENCAMINHADO, TRIAGEM_MANUAL, EM_EXECUCAO, RESOLVIDO }

    private final String protocolo;
    private final String descricao;
    private final double latitude;
    private final double longitude;
    private final Classificacao classificacao;
    private final Status status;
    private final Instant criadoEm = Instant.now();
    private int apoios = 1;

    Pedido(String protocolo, String descricao, double latitude, double longitude,
           Classificacao classificacao, Status status) {
        this.protocolo = protocolo;
        this.descricao = descricao;
        this.latitude = latitude;
        this.longitude = longitude;
        this.classificacao = classificacao;
        this.status = status;
    }

    void apoiar() { apoios++; }

    /** Quanto maior, mais alto na fila do gestor: urgência da IA pesa mais, cada apoio desempata. */
    public int getPrioridade() { return (classificacao == null ? 0 : classificacao.urgencia() * 10) + apoios; }

    public String getProtocolo() { return protocolo; }
    public String getDescricao() { return descricao; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Classificacao getClassificacao() { return classificacao; }
    public Status getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
    public int getApoios() { return apoios; }
}
