package paquete;

import java.net.InetAddress;

public class FilaTabla {
	private String direccionDestino;
	private int numeroSaltos;
	private String nextHop;
	private int puertoEnvio;
	public String mascaraSubRed;
	
	public FilaTabla(String direccionDestino, int numeroSaltos, String nextHop, int puertoEnvio){
		this.direccionDestino = direccionDestino;
		this.numeroSaltos = numeroSaltos;
		this.nextHop = nextHop;
		this.puertoEnvio = puertoEnvio;
	}
	
	public void actualizarInformacion(int numeroSaltos, String nextHop, int puertoEnvio){
		if(this.numeroSaltos>numeroSaltos){
			this.numeroSaltos = numeroSaltos;
			this.nextHop = nextHop;
			this.puertoEnvio = puertoEnvio;
		}
	}
	
	public String getDireccionDestino(){
		return this.direccionDestino;
	}
	
	public int getNumeroSaltos(){
		return this.numeroSaltos;
	}
	
	public String getNextHop(){
		return this.nextHop;
	}
	
	public String getMascaraSubRed(){
		return this.mascaraSubRed;
	}
}
