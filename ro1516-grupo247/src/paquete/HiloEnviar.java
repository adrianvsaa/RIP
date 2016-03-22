package paquete;

import java.io.IOException;
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
					router.getSocket().send(router.getPaquete());
				}catch(IOException io){
					System.err.println("Error en E/S de datos");
					System.exit(0);
				}
			}
		};
		Timer t = new Timer();
		t.schedule(tarea, 0, 10*1000);		//Metodo que ejecuta tarea desde 0 milisegundos con un periodo de 10 segundos	
	}
}
