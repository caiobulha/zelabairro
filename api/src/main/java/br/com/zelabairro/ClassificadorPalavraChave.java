package br.com.zelabairro;

import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * shortcut: classifica só pelo texto, por palavra-chave, para a API rodar sem nuvem;
 * em produção, troque por uma implementação que chama o Amazon Bedrock com foto + texto.
 */
@Component
public class ClassificadorPalavraChave implements ClassificadorIA {

    @Override
    public Classificacao classificar(String descricao, byte[] foto) {
        String d = descricao.toLowerCase(Locale.ROOT);
        if (d.contains("buraco")) return new Classificacao("via_publica_buraco", 3, "subprefeitura", 0.8);
        if (d.contains("poste") || d.contains("lâmpada") || d.contains("escuro"))
            return new Classificacao("iluminacao", 3, "concessionaria_energia", 0.8);
        if (d.contains("lixo") || d.contains("entulho")) return new Classificacao("limpeza", 2, "subprefeitura", 0.8);
        if (d.contains("vazamento") || d.contains("água")) return new Classificacao("agua", 4, "concessionaria_agua", 0.8);
        if (d.contains("árvore") || d.contains("galho")) return new Classificacao("arvore", 3, "subprefeitura", 0.8);
        return new Classificacao("indefinida", 1, "subprefeitura", 0.3); // não reconheceu: vai para triagem manual
    }
}
