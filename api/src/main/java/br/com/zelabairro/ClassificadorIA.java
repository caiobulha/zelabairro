package br.com.zelabairro;

/** Triagem por IA: recebe a foto e a descrição e devolve categoria, urgência, órgão e confiança. */
public interface ClassificadorIA {
    Classificacao classificar(String descricao, byte[] foto);
}
