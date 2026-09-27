package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ValidadorStock extends ValidadorPedido {
    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        for (var item : contexto.getRequest().getItems()) {
            Integer stockActual = jdbcTemplate.queryForObject(
                "SELECT stock FROM inventario WHERE producto_id = ?",
                Integer.class, item.getProductoId());

            if (stockActual == null || stockActual < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente para el producto ID: " + item.getProductoId());
                return;
            }
        }
    }
}