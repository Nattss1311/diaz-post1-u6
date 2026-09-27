package com.tienda.pedidos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.validacion.ContextoPedido;

@SpringBootTest
class GestorPedidosTest {

    @Autowired
    private CalculadorDescuentoFinal calculadorDescuentoFinal;

    @Test
    void test1_DescuentoPorVolumen() {
        ItemPedido item = new ItemPedido();
        item.setCantidad(25); // Mas de 20 unidades activa 12%

        PedidoRequest request = new PedidoRequest();
        request.setClienteId(1L);
        request.setItems(java.util.List.of(item));

        ContextoPedido contexto = new ContextoPedido(request);
        contexto.setTipoCliente("REGULAR");

        double descuento = calculadorDescuentoFinal.calcular(contexto);
        assertEquals(0.12, descuento, 0.001);
    }

    @Test
    void test2_DescuentoClienteVIP_GanaMayorDescuento() {
        ItemPedido item = new ItemPedido();
        item.setCantidad(2);

        PedidoRequest request = new PedidoRequest();
        request.setClienteId(2L);
        request.setItems(java.util.List.of(item));

        ContextoPedido contexto = new ContextoPedido(request);
        contexto.setTipoCliente("VIP");

        double descuento = calculadorDescuentoFinal.calcular(contexto);
        assertEquals(0.05, descuento, 0.001);
    }
}