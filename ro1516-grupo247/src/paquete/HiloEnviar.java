package paquete;


public class HiloEnviar extends Thread {
	Router router;
	
	public HiloEnviar(Router router){
		super("Router enviando datos");
		this.router = router;
	}
	
	public void run(){
		
	}
}
