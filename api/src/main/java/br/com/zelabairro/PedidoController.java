package br.com.zelabairro;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    /** 201 = pedido novo; 200 = era duplicado e virou apoio de um pedido que já existe. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Pedido> abrir(@RequestParam(required = false) MultipartFile foto,
                                        @RequestParam(required = false) String descricao,
                                        @RequestParam(required = false) Double latitude,
                                        @RequestParam(required = false) Double longitude) throws IOException {
        PedidoService.Resultado r = service.abrir(descricao, latitude, longitude, foto);
        return ResponseEntity.status(r.duplicado() ? HttpStatus.OK : HttpStatus.CREATED).body(r.pedido());
    }

    @GetMapping("/{protocolo}")
    public ResponseEntity<Pedido> buscar(@PathVariable String protocolo) {
        return ResponseEntity.of(service.buscar(protocolo));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> entradaInvalida(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }
}
