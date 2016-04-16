package paquete;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class HiloEnviar extends Thread {
	Router router;
	
	public HiloEnviar(Router router){
		super("Router enviando datos");
		this.router = router;
	}
	
	public void run(){
		TimerTask tarea = new TimerTask(){
			public void run(){
				try{
					Set<String> keys = router.getTabla().keySet();
					for(String key : keys){
						InetAddress direccion = InetAddress.getByName(key);
						DatagramSocket socket = new DatagramSocket(router.getPuerto());
						DatagramPacket paquete = new DatagramPacket(router.getPaquete(), router.getPaquete().length, direccion
								, router.getTabla().get(key).getPuerto());
						socket.send(paquete);
						socket.close();
					}
				}catch(IOException io){
					System.err.println("Error en E/S de datos");
					System.exit(0);
				}
			}
		};
		Timer t = new Timer();
		t.schedule(tarea, 0, 30*1000);		//Metodo que ejecuta tarea desde 0 milisegundos con un periodo de 30 segundos	
	}
}
