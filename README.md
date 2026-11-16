# ZelaBairro

Zeladoria colaborativa com triagem por IA. O cidadão relata um problema na rua com foto e localização; a IA classifica categoria, urgência e órgão responsável; pedidos iguais no mesmo local viram um só.

Projeto do Desafio CitizenTech (FIAP).

## O que tem aqui

| Pasta | Conteúdo |
| --- | --- |
| `api/` | API em Java 21 + Spring Boot com as regras centrais: abrir pedido, triagem, prioridade e detecção de duplicados |
| `.github/workflows/ci.yml` | CI no GitHub Actions: compila e roda os testes a cada push e pull request |

Previstos na arquitetura, ainda não criados: `mobile/` (React Native), `web/` (painel do gestor) e `infra/` (Terraform/AWS CDK).

## Como rodar

Precisa de Java 21 e Maven.

```bash
cd api
mvn test                 # roda os cenários de teste
mvn spring-boot:run      # sobe a API em http://localhost:8080
```

Abrir um pedido:

```bash
curl -F foto=@buraco.jpg -F descricao="Buraco na frente da escola" \
     -F latitude=-23.5505 -F longitude=-46.6333 http://localhost:8080/pedidos
```

Consultar: `GET http://localhost:8080/pedidos/{protocolo}`

## Endpoints

| Método | Caminho | Resposta |
| --- | --- | --- |
| `POST` | `/pedidos` (multipart: `foto`, `descricao`, `latitude`, `longitude`) | `201` pedido novo · `200` era duplicado e virou apoio · `400` entrada inválida |
| `GET` | `/pedidos/{protocolo}` | `200` pedido · `404` não existe |

## Cenários de teste (`PedidoApiTest`)

| Teste | Cenário |
| --- | --- |
| `ct01_abrirSolicitacaoValida` | Foto JPG + local + descrição → 201, protocolo, status `ENCAMINHADO`, categoria/urgência/órgão da IA |
| `ct02_relatoA20mDoMesmoProblemaViraApoio` | Segundo relato a 20 m do mesmo problema → mesmo protocolo, apoios 1 → 2, prioridade sobe |
| `ct03a_iaComBaixaConfiancaVaiParaTriagemManual` | IA com confiança 0,45 → status `TRIAGEM_MANUAL` |
| `ct03a_respostaDaIaForaDoFormatoVaiParaTriagemManual` | IA devolve categoria fora da lista → triagem manual, nada da resposta é guardado |
| `ct03b_pdfDe15MbSemLocalizacaoERecusado` | PDF de 15 MB sem local → 400 com mensagem clara, IA nem é chamada |

Nos testes a IA é simulada (Mockito) para o resultado ser previsível.

## O que está simplificado nesta versão

| Aqui | Na arquitetura proposta (AWS) |
| --- | --- |
| Pedidos e fotos em memória | RDS PostgreSQL + PostGIS e Amazon S3 |
| Triagem dentro da requisição | Fila SQS + AWS Lambda |
| IA por palavra-chave (`ClassificadorPalavraChave`) | Amazon Bedrock com foto + texto |
| Distância calculada em Java (Haversine) | `ST_DWithin` do PostGIS |

## Fluxo de colaboração

1. Crie uma branch a partir da `main`: `feat/123-descricao`
2. Commits no padrão Conventional Commits (`feat:`, `fix:`, `test:`)
3. Abra um Pull Request; o merge exige CI verde e 1 aprovação
