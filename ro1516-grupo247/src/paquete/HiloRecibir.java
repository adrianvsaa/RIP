package paquete;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class HiloRecibir extends Thread {
	Router router;
	DatagramSocket socket;
	
	public HiloRecibir(Router router){
		super("Router recibiendo datos");
		this.router = router;
		try {
			socket = new DatagramSocket(router.getPuerto());
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println("Error en HiloRecibir al crear Socket");
			System.exit(0);
		}
	}
	
	public void run(){
		try{
			while(true){
				DatagramPacket paquete = new DatagramPacket(new byte[104], 104);
				socket.receive(paquete);
				router.actualizarTabla(paquete.getData());
				router.imprimirTabla();
			}
		}catch(IOException io){
			System.err.println("Error en E/S de datos");
			System.exit(0);
		}
	}
}
