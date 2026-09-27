package com.tienda.pedidos.validacion;

public abstract class ValidadorPedido {
    protected ValidadorPedido siguiente;

    public void setSiguiente(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
    }

    public void validar(ContextoPedido contexto) {
        ejecutar(contexto);
        if (siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void ejecutar(ContextoPedido contexto);
}