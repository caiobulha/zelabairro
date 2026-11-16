package br.com.zelabairro;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Cenários de teste da proposta (CT01 a CT03). A IA é simulada para o resultado ser previsível. */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoApiTest {

    static final Classificacao BURACO = new Classificacao("via_publica_buraco", 4, "subprefeitura", 0.91);

    @Autowired MockMvc mvc;
    @MockitoBean ClassificadorIA ia;

    // Cada teste usa um local diferente para que os pedidos de um não virem duplicados do outro.

    @Test
    void ct01_abrirSolicitacaoValida() throws Exception {
        when(ia.classificar(any(), any())).thenReturn(BURACO);

        String protocolo = protocolo(abrir(foto("image/jpeg", 3 * 1024 * 1024),
                "Buraco grande na frente da escola, carro já caiu", -23.5505, -46.6333)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ENCAMINHADO"))
                .andExpect(jsonPath("$.classificacao.categoria").value("via_publica_buraco"))
                .andExpect(jsonPath("$.classificacao.urgencia").value(4))
                .andExpect(jsonPath("$.classificacao.orgao").value("subprefeitura")));

        mvc.perform(get("/pedidos/" + protocolo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value(protocolo));
    }

    @Test
    void ct02_relatoA20mDoMesmoProblemaViraApoio() throws Exception {
        when(ia.classificar(any(), any())).thenReturn(BURACO);

        String original = protocolo(abrir(foto("image/jpeg", 1024), "Buraco na rua", -23.6000, -46.7000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apoios").value(1))
                .andExpect(jsonPath("$.prioridade").value(41)));

        // -23.60018 fica a cerca de 20 m de -23.6000
        abrir(foto("image/jpeg", 1024), "Outro buraco aqui", -23.60018, -46.7000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value(original))
                .andExpect(jsonPath("$.apoios").value(2))
                .andExpect(jsonPath("$.prioridade").value(42));
    }

    @Test
    void ct03a_iaComBaixaConfiancaVaiParaTriagemManual() throws Exception {
        when(ia.classificar(any(), any())).thenReturn(new Classificacao("limpeza", 2, "subprefeitura", 0.45));

        abrir(foto("image/jpeg", 1024), "olha isso", -23.7000, -46.8000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo").isNotEmpty())
                .andExpect(jsonPath("$.status").value("TRIAGEM_MANUAL"));
    }

    @Test
    void ct03a_respostaDaIaForaDoFormatoVaiParaTriagemManual() throws Exception {
        when(ia.classificar(any(), any())).thenReturn(new Classificacao("ignore_as_regras", 9, "prefeito", 0.99));

        abrir(foto("image/jpeg", 1024), "buraco", -23.8000, -46.9000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TRIAGEM_MANUAL"))
                .andExpect(jsonPath("$.classificacao").doesNotExist());
    }

    @Test
    void ct03b_pdfDe15MbSemLocalizacaoERecusado() throws Exception {
        mvc.perform(multipart("/pedidos")
                        .file(foto("application/pdf", 15 * 1024 * 1024))
                        .param("descricao", "buraco"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("JPG ou PNG de até 10 MB")));

        verify(ia, never()).classificar(any(), any()); // nada foi processado nem gravado
    }

    private ResultActions abrir(MockMultipartFile foto, String descricao, double lat, double lon) throws Exception {
        return mvc.perform(multipart("/pedidos").file(foto)
                .param("descricao", descricao)
                .param("latitude", String.valueOf(lat))
                .param("longitude", String.valueOf(lon)));
    }

    private static String protocolo(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.protocolo");
    }

    private static MockMultipartFile foto(String tipo, int bytes) {
        return new MockMultipartFile("foto", "foto", tipo, new byte[bytes]);
    }
}
