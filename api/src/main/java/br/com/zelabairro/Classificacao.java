package br.com.zelabairro;

import java.util.Set;

/** Resposta da IA de triagem, no formato JSON fixo descrito na proposta. */
public record Classificacao(String categoria, int urgencia, String orgao, double confianca) {

    static final Set<String> CATEGORIAS = Set.of("via_publica_buraco", "iluminacao", "limpeza", "agua", "arvore");
    static final Set<String> ORGAOS = Set.of("subprefeitura", "concessionaria_energia", "concessionaria_agua");

    /** A resposta da IA é dado externo: só vale se estiver dentro das listas fechadas. */
    boolean valida() {
        return CATEGORIAS.contains(categoria) && ORGAOS.contains(orgao) && urgencia >= 1 && urgencia <= 5;
    }
}
