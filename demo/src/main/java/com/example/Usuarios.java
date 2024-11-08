package com.example;

import org.zeromq.ZMQ;

public class Usuarios {
    private String identificadorUsuario;
    private String PosicionUsuario;
    private ZMQ.Context context;
    private ZMQ.Socket requester;

    public Usuarios(String id) {
        this.identificadorUsuario = id;
        context = ZMQ.context(1);

        requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://localhost:5555");
    }

    public void solicitarTaxi() {
        requester.send("Solicitud de Taxi de: " + identificadorUsuario);
        String respuesta = requester.recvStr();
        
        if (respuesta.equals("No Disponible")) {
            System.out.println("No hay taxis disponibles para ti" + identificadorUsuario);
        } else {
            System.out.println("Taxi asignado para " + identificadorUsuario + ": " + respuesta);
        }
    }

    public void iniciarSolicitud() {
        new Thread(this::solicitarTaxi).start();
    }

    public void cerrar() {
        requester.close();
        context.close();
    }
}
