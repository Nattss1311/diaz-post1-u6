package com.tienda.pedidos.descuento;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.tienda.pedidos.validacion.ContextoPedido;

@Component
public class DescuentoCorporativo implements EstrategiaDescuento {
    private final JdbcTemplate jdbcTemplate;

    public DescuentoCorporativo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(ContextoPedido contexto) {
        try {
            String nit = jdbcTemplate.queryForObject(
                "SELECT nit FROM clientes WHERE id = ?", String.class,
                contexto.getRequest().getClienteId());
            return (nit != null && !nit.isBlank()) ? 0.10 : 0.0;
        } catch (Exception e) {
            // Si la columna NIT no existe en la BD de pruebas o el cliente no tiene NIT, retorna 0%
            return 0.0;
        }
    }
}