package com.example;

import org.zeromq.ZMQ;
import java.util.List;
import java.util.Map;

public class Servidor {
    private List<Taxi> listaTaxis;
    private List<Usuarios> listaUsuarios;
    private List<Boolean> listaDisponibilidadTaxis;
    private int [][] ciudad;
    private int N;
    private int M;
    private ZMQ.Context context;
    private ZMQ.Socket subscriber;
    private ZMQ.Socket responder;
    private Map<String, String> posicionesTaxis;
    

    public Servidor(int N, int M) {
        this.N = N;
        this.M = M;
        this.ciudad = new int[N][M];

        context = ZMQ.context(1);
        
        subscriber = context.socket(ZMQ.SUB);
        subscriber.connect("tcp://localhost:5556");
        subscriber.subscribe("".getBytes());  

        // Configuración del socket para recibir solicitudes de los usuarios
        responder = context.socket(ZMQ.REP);
        responder.bind("tcp://*:5555");
    }

    public void start() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String posicionTaxi = subscriber.recvStr();
                actualizarPosicionTaxi(posicionTaxi);
            }
        }).start();

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String solicitud = responder.recvStr();
                String respuesta = procesarSolicitud(solicitud);
                responder.send(respuesta);
            }
        }).start();
    }

    public void registrarTaxi(Taxi taxi) {
        listaTaxis.add(taxi);
    }

    public void actualizarPosicionTaxi(String posicionTaxi) {
        
    }

    public String procesarSolicitud(String solicitud) {
        // Lógica para asignar un taxi o responder que no hay taxis disponibles
        return "Asignación de Taxi o No Disponible";
    }

    public void cerrar() {
        subscriber.close();
        responder.close();
        context.close();
    }
}
