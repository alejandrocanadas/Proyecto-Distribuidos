package com.example;

import org.zeromq.ZMQ;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    private Map<String, String> posicionesTaxis = new ConcurrentHashMap<>();
    private Map<String, Boolean> disponibilidadTaxis = new ConcurrentHashMap<>();
    private Map<String, ZMQ.Socket> socketsTaxis = new ConcurrentHashMap<>(); // Sockets dedicados para cada taxi
    private ZMQ.Context context;
    private ZMQ.Socket subscriber;
    private ZMQ.Socket responder;
    private int N;
    private int M;
    private volatile boolean running = true;

    public Servidor(int N, int M) {
        this.N = N;
        this.M = M;

        context = ZMQ.context(1);
        subscriber = context.socket(ZMQ.SUB);
        subscriber.bind("tcp://*:5556"); // Conexión a taxis que envían sus posiciones
        subscriber.subscribe("".getBytes()); // Suscribe a todos los mensajes

        responder = context.socket(ZMQ.REP);
        responder.bind("tcp://*:5555"); // Escuchar solicitudes de usuarios
    }

    public void start() {
        // Hilo para recibir y actualizar posiciones de taxis
        new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                String posicionTaxi = subscriber.recvStr();
                actualizarPosicionTaxi(posicionTaxi);
            }
        }).start();

        // Hilo para procesar solicitudes de los usuarios
        new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                String solicitud = responder.recvStr();
                String respuesta = procesarSolicitud(solicitud);
                responder.send(respuesta);
            }
        }).start();
    }

    public void actualizarPosicionTaxi(String posicionTaxi) {
        String[] datos = posicionTaxi.split(" ");
        String idTaxi = datos[0];
        String posicion = datos[1] + "," + datos[2];

        // Registrar posición y disponibilidad del taxi
        posicionesTaxis.put(idTaxi, posicion);
        disponibilidadTaxis.putIfAbsent(idTaxi, true);

        System.out.println("Servidor recibió posición de Taxi " + idTaxi + ": " + posicion);
    }

    public String procesarSolicitud(String solicitud) {
        String[] datos = solicitud.split(" ");
        String idUsuario = datos[0];
        int xUsuario = Integer.parseInt(datos[1]);
        int yUsuario = Integer.parseInt(datos[2]);

        String taxiAsignado = asignarTaxiMasCercano(xUsuario, yUsuario);
        if (taxiAsignado == null) {
            return "No Disponible";
        }

        disponibilidadTaxis.put(taxiAsignado, false); // Marca taxi como ocupado
        posicionesTaxis.remove(taxiAsignado); // Elimina temporalmente al taxi de la lista de taxis disponibles

        // Envía una solicitud de servicio al taxi asignado usando su socket dedicado
        ZMQ.Socket socketTaxi = socketsTaxis.get(taxiAsignado);
        if (socketTaxi == null) {
            socketTaxi = context.socket(ZMQ.PUSH);
            socketTaxi.connect("tcp://localhost:556" + taxiAsignado); // Puerto único para cada taxi
            socketsTaxis.put(taxiAsignado, socketTaxi);
        }

        socketTaxi.send("Servicio para taxi " + taxiAsignado);

        // Programar para marcar el taxi como disponible nuevamente después del servicio
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                disponibilidadTaxis.put(taxiAsignado, true); // Marca taxi como disponible después del servicio
                System.out.println("Servicio completado. Taxi " + taxiAsignado + " está disponible nuevamente.");
            }
        }, 30000); // 30 segundos en lugar de 30 minutos

        return "Taxi asignado: " + taxiAsignado;
    }

    private String asignarTaxiMasCercano(int xUsuario, int yUsuario) {
        String taxiMasCercano = null;
        double distanciaMinima = Double.MAX_VALUE;

        for (Map.Entry<String, String> entry : posicionesTaxis.entrySet()) {
            String idTaxi = entry.getKey();
            if (!disponibilidadTaxis.get(idTaxi)) continue;

            String[] posicion = entry.getValue().split(",");
            int xTaxi = Integer.parseInt(posicion[0]);
            int yTaxi = Integer.parseInt(posicion[1]);

            double distancia = calcularDistancia(xUsuario, yUsuario, xTaxi, yTaxi);
            if (distancia < distanciaMinima || (distancia == distanciaMinima && idTaxi.compareTo(taxiMasCercano) < 0)) {
                distanciaMinima = distancia;
                taxiMasCercano = idTaxi;
            }
        }

        return taxiMasCercano;
    }

    private double calcularDistancia(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public void cerrar() {
        running = false;

        // Cerrar todos los sockets
        subscriber.close();
        responder.close();
        for (ZMQ.Socket socket : socketsTaxis.values()) {
            socket.close();
        }
        context.close();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java Servidor <N> <M>");
            System.exit(1);
        }

        int N = Integer.parseInt(args[0]);
        int M = Integer.parseInt(args[1]);
        
        Servidor servidor = new Servidor(N, M);
        servidor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(servidor::cerrar));
    }
}
