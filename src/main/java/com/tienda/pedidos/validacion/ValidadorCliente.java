package com.tienda.pedidos.validacion;

import java.time.LocalTime;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ValidadorCliente extends ValidadorPedido {
    private final JdbcTemplate jdbcTemplate;

    public ValidadorCliente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();
        String tipo = null;
        
        try {
            tipo = jdbcTemplate.queryForObject(
                "SELECT tipo_cliente FROM clientes WHERE id = ?", String.class, clienteId);
        } catch (EmptyResultDataAccessException e) {
            contexto.rechazar("Cliente no registrado");
            return;
        }

        if (tipo == null) { 
            contexto.rechazar("Cliente no registrado"); 
            return; 
        }
        
        contexto.setTipoCliente(tipo);

        if (tipo.equals("MOROSO")) {
            Double deuda = jdbcTemplate.queryForObject(
                "SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false",
                Double.class, clienteId);
            boolean fueraDeHorarioDeCorte = !LocalTime.now().isBefore(LocalTime.of(20, 0));
            if (deuda != null && deuda > 0 && !fueraDeHorarioDeCorte) {
                contexto.rechazar("Cliente con deuda pendiente: $" + deuda);
            }
        }
    }
}