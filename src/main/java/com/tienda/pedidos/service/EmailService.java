package com.tienda.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.info("Simulando envío de correo a [{}]", destinatario);
        log.info("Asunto: {}", asunto);
        log.info("Cuerpo:\n{}", cuerpo);
    }
}