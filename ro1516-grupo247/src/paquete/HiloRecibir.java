package paquete;

import java.io.IOException;
import java.net.DatagramPacket;

public class HiloRecibir extends Thread {
	Router router;
	
	public HiloRecibir(Router router){
		super("Router recibiendo datos");
		this.router = router;
	}
	
	public void run(){
		try{
			while(true){
				DatagramPacket paquete = new DatagramPacket(new byte[104], 104);
				router.getSocket().receive(paquete);
				router.actualizarTabla(paquete.getData());
			}
		}catch(IOException io){
			System.err.println("Error en E/S de datos");
			System.exit(0);
		}
	}
}
