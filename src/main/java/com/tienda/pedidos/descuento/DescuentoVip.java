package com.tienda.pedidos.descuento;

import org.springframework.stereotype.Component;

import com.tienda.pedidos.validacion.ContextoPedido;

@Component
public class DescuentoVip implements EstrategiaDescuento {
    @Override
    public double calcular(ContextoPedido contexto) {
        double subtotal = contexto.getSubtotal();
        if (subtotal > 1_000_000) return 0.15;
        if (subtotal > 500_000) return 0.10;
        return 0.05;
    }
}