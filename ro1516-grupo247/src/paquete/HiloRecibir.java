package paquete;

public class HiloRecibir extends Thread {
	Router router;
	
	public HiloRecibir(Router router){
		super("Router recibiendo datos");
		this.router = router;
	}
	
	public void run(){
		
	}
}
