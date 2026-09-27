package com.tienda.pedidos.descuento;

import org.springframework.stereotype.Component;

import com.tienda.pedidos.validacion.ContextoPedido;

@Component
public class DescuentoEstandar implements EstrategiaDescuento {
    @Override
    public double calcular(ContextoPedido contexto) { 
        return 0.0; 
    }
}