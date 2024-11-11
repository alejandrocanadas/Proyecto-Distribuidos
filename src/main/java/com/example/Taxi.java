package com.example;

import org.zeromq.ZMQ;
import java.util.Random;

public class Taxi {
    private String identificadorTaxi;
    private int x, y, serviciosDiarios;
    private float velocidad;
    private ZMQ.Context context;
    private ZMQ.Socket publisher;
    private ZMQ.Socket servicioSocket; // Socket para recibir servicios del servidor
    private Random random;
    private int serviciosRealizados;
    private boolean ocupado;
    private int posInicialX, posInicialY;

    private static int N;
    private static int M;

    public Taxi(String id, int x, int y, float velocidad, int serviciosDiarios) {
        this.identificadorTaxi = id;
        this.x = Math.min(x, N - 1);
        this.y = Math.min(y, M - 1);
        this.velocidad = velocidad;
        this.serviciosDiarios = serviciosDiarios;
        this.serviciosRealizados = 0;
        this.ocupado = false;
        this.random = new Random();
        this.posInicialX = x;
        this.posInicialY = y;

        // Crear contexto y socket para publicar posición
        context = ZMQ.context(1);
        publisher = context.socket(ZMQ.PUB);
        publisher.bind("tcp://*:5556");  // Publica en un puerto común para todos los taxis

        // Crear socket para recibir notificaciones de servicio en un puerto específico
        servicioSocket = context.socket(ZMQ.PULL);
        servicioSocket.bind("tcp://*:556" + identificadorTaxi); // Ejemplo: tcp://*:5561 para taxi con ID 1
    }

    public void mover() {
        if (ocupado || velocidad == 0) return;

        int movimiento = (int) (velocidad / 2);
        if (random.nextBoolean()) {
            x = Math.min(N - 1, Math.max(0, x + (random.nextBoolean() ? movimiento : -movimiento)));
        } else {
            y = Math.min(M - 1, Math.max(0, y + (random.nextBoolean() ? movimiento : -movimiento)));
        }

        enviarPosicion();
    }

    public void enviarPosicion() {
        String mensaje = identificadorTaxi + " " + x + " " + y;
        publisher.send(mensaje);
        System.out.println("Taxi " + identificadorTaxi + " se movio a la posicion: " + toString(x, y));
    }

    public void recibirSolicitud() {
        if (serviciosRealizados < serviciosDiarios) {
            ocupado = true;
            serviciosRealizados++;
            System.out.println("Taxi " + identificadorTaxi + " recibio una solicitud de servicio.");

            // Simulación del tiempo de servicio (30 segundos en lugar de 30 minutos)
            try {
                Thread.sleep(30000); // Espera simulada de 30 minutos de servicio
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Regresa a la posición inicial después del servicio
            x = posInicialX;
            y = posInicialY;
            enviarPosicion();
            System.out.println("Taxi " + identificadorTaxi + " regresó a posición inicial después del servicio.");
            ocupado = false;

            if (serviciosRealizados >= serviciosDiarios) {
                System.out.println("Taxi " + identificadorTaxi + " ha completado todos sus servicios del día.");
            }
        }
    }

    public String toString(int x, int y) {
        return "(" + x + ", " + y + ")";
    }

    public void cerrar() {
        publisher.close();
        servicioSocket.close();
        context.close();
    }

    public static void main(String[] args) {
        if (args.length != 7) {
            System.out.println("Uso: Taxi <id> <N> <M> <x> <y> <velocidad> <serviciosDiarios>");
            System.exit(1);
        }

        String id = args[0];
        N = Integer.parseInt(args[1]);
        M = Integer.parseInt(args[2]);
        int x = Integer.parseInt(args[3]);
        int y = Integer.parseInt(args[4]);
        float velocidad = Float.parseFloat(args[5]);
        int serviciosDiarios = Integer.parseInt(args[6]);

        Taxi taxi = new Taxi(id, x, y, velocidad, serviciosDiarios);

        // Hilo para mover el taxi y verificar si recibe solicitudes de servicio
        new Thread(() -> {
            while (taxi.serviciosRealizados < taxi.serviciosDiarios) {
                taxi.mover();

                // Intentar recibir una solicitud de servicio
                String solicitudServicio = taxi.servicioSocket.recvStr(ZMQ.DONTWAIT); // No bloqueante
                if (solicitudServicio != null && solicitudServicio.contains("Servicio")) {
                    taxi.recibirSolicitud();
                }

                // Simula el movimiento cada 30 segundos (equivalente a 30 minutos en el sistema)
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            taxi.cerrar();
        }).start();
    }
}
