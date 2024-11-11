package com.example;

import org.zeromq.ZMQ;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Usuarios {
    private String identificadorUsuario;
    private int x, y;
    private int tiempoEspera; // En segundos (representa minutos en la simulación)
    private ZMQ.Context context;
    private ZMQ.Socket requester;

    private static int N; // Número de filas (se definirá con argumentos)
    private static int M; // Número de columnas (se definirá con argumentos)

    public Usuarios(String id, int x, int y, int tiempoEspera) {
        this.identificadorUsuario = id;
        this.x = x;
        this.y = y;
        this.tiempoEspera = tiempoEspera;
        this.context = ZMQ.context(1);
        this.requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://localhost:5555"); // Dirección del servidor

        // Configura el tiempo máximo de espera para recibir una respuesta
        requester.setReceiveTimeOut(5000); // Tiempo de espera en milisegundos (5 segundos)
    }

    public void solicitarTaxi() {
        try {
            Thread.sleep(tiempoEspera * 1000L); // Simular el tiempo hasta necesitar un taxi

            long tiempoInicio = System.currentTimeMillis(); // Tiempo antes de la solicitud
            requester.send("Solicitud de Taxi de: " + identificadorUsuario + " en posición (" + x + ", " + y + ")");

            // Intentar recibir una respuesta del servidor
            String respuesta = requester.recvStr();
            long tiempoRespuesta = System.currentTimeMillis() - tiempoInicio; // Tiempo total de respuesta

            if (respuesta == null) {
                System.out.println("Usuario " + identificadorUsuario + ": El servidor no respondió a tiempo.");
            } else if ("No Disponible".equals(respuesta)) {
                System.out.println("Usuario " + identificadorUsuario + ": No hay taxis disponibles.");
            } else {
                System.out.println("Usuario " + identificadorUsuario + ": Taxi asignado. Tiempo de respuesta: " + tiempoRespuesta + " ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error en la solicitud del usuario " + identificadorUsuario);
        } finally {
            cerrar();
        }
    }

    public void cerrar() {
        requester.close();
        context.close();
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Uso: java Usuarios <filas (N)> <columnas (M)> <archivo de coordenadas> <cantidad de usuarios>");
            return;
        }

        try {
            // Lee los argumentos
            N = Integer.parseInt(args[0]);
            M = Integer.parseInt(args[1]);
            String archivoCoordenadas = args[2];
            int cantidadUsuarios = Integer.parseInt(args[3]);

            List<int[]> coordenadasUsuarios = leerCoordenadas(archivoCoordenadas);
            if (coordenadasUsuarios.isEmpty() || coordenadasUsuarios.size() < cantidadUsuarios) {
                System.err.println("No hay suficientes coordenadas en el archivo para el número de usuarios especificado.");
                return;
            }

            ExecutorService ejecutor = Executors.newFixedThreadPool(cantidadUsuarios);
            Random random = new Random();

            for (int i = 0; i < cantidadUsuarios; i++) {
                int[] coordenadas = coordenadasUsuarios.get(i);
                int x = Math.min(coordenadas[0], N - 1); // Asegura que x esté dentro de los límites de la cuadrícula
                int y = Math.min(coordenadas[1], M - 1); // Asegura que y esté dentro de los límites de la cuadrícula
                int tiempoEspera = 5 + random.nextInt(10); // Asigna un tiempo aleatorio entre 5 y 15 segundos
                Usuarios usuario = new Usuarios("Usuario" + i, x, y, tiempoEspera);
                ejecutor.submit(usuario::solicitarTaxi);
            }

            ejecutor.shutdown();

        } catch (NumberFormatException e) {
            System.err.println("Error: Los argumentos de filas, columnas y cantidad de usuarios deben ser enteros.");
        }
    }

    private static List<int[]> leerCoordenadas(String archivo) {
        List<int[]> coordenadas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.trim().split(" ");
                if (partes.length >= 2) { // Verifica que haya al menos 2 valores
                    int x = Integer.parseInt(partes[0]);
                    int y = Integer.parseInt(partes[1]);
                    coordenadas.add(new int[]{x, y});
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de coordenadas: " + e.getMessage());
        }
        return coordenadas;
    }
}
