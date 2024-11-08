package com.example;

import java.util.Random;

import org.zeromq.ZMQ;

public class Taxi {
    private String identificadorTaxi;
    private int N;
    private int M;
    private int x;
    private int y;
    private float velocidad;
    private int serviciosDiarios = 0;
    private ZMQ.Context context;
    private ZMQ.Socket publisher;
    private Random random;

    public Taxi(String id, int N, int M, int x, int y, float velocidad, int serviciosDiarios) {
        this.identificadorTaxi = id;
        this.N = N;
        this.M = M;
        this.x = x;
        this.y = y;
        this.velocidad = velocidad;
        this.serviciosDiarios = serviciosDiarios;

        if (x < 0 || x > N || y < 0 || y > M) {
            throw new IllegalArgumentException("La ubicación del taxi no es valida en el tamaño de la matriz.");
        }

        if (velocidad != 1 || velocidad != 2 || velocidad != 4) {
            throw new IllegalArgumentException("La velocidad del taxi debe ser 1, 2 o 4 km/h.");
        }

        if (serviciosDiarios > 3 || serviciosDiarios < 0) {
            throw new IllegalArgumentException("El número de servicios diarios debe ser un número entre 0 y 3.");
        }

        context = ZMQ.context(1);

        // Configuración del socket de publicador
        publisher = context.socket(ZMQ.PUB);
        publisher.bind("tcp://*:5556");
    }

    public void mover() {
        if (velocidad == 0) {
            return;
        } else if (velocidad > 0) {
            // implementacion de movimiento
            return;
        }
    }

    public String toString(int x, int y) {
        // Formato de la posición
        return "(" + x + ", " + y + ")";
    }

    public void enviarPosicion(ZMQ.Socket publisher, String identificadorTaxi, int x, int y) {
        String mensaje = identificadorTaxi + "en " + toString(x, y);
        publisher.send(mensaje);
    }

    public void recibirSolicitud() {
        //String solicitud = context.recv();
        //System.out.println("Recibida solicitud de: " + solicitud);
        return;
    }

    public void cerrar() {
        publisher.close();
        context.close();
    }
}
