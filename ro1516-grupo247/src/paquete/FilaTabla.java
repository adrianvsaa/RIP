package paquete;

import java.net.InetAddress;

public class FilaTabla {
	private String direccionDestino;
	private int numeroSaltos;
	private String nextHop;
	private int puertoEnvio;
	
	public FilaTabla(String direccionDestino, int numeroSaltos, String nextHop, int puertoEnvio){
		this.direccionDestino = direccionDestino;
		this.numeroSaltos = numeroSaltos;
		this.nextHop = nextHop;
		this.puertoEnvio = puertoEnvio;
	}
	
	public void actualizarInformacion(){
		
	}
}
